package com.glossup.shared.exception;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.ratelimit.ClientIpResolver;
import com.glossup.shared.security.LoginTarpitService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Manejo global de errores (@ControllerAdvice) para toda la aplicacion. Centraliza la traduccion
 * de excepciones a respuestas HTTP con el cuerpo {@link ApiError}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ClientIpResolver ipResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final LoginTarpitService tarpitService;

    @org.springframework.beans.factory.annotation.Autowired
    public GlobalExceptionHandler(
            org.springframework.beans.factory.ObjectProvider<ClientIpResolver> ipResolverProvider,
            org.springframework.beans.factory.ObjectProvider<ApplicationEventPublisher> eventPublisherProvider,
            org.springframework.beans.factory.ObjectProvider<LoginTarpitService> tarpitServiceProvider) {
        this(ipResolverProvider.getIfAvailable(() -> new ClientIpResolver(0)),
                eventPublisherProvider.getIfAvailable(),
                tarpitServiceProvider.getIfAvailable());
    }

    public GlobalExceptionHandler(ClientIpResolver ipResolver,
                                  ApplicationEventPublisher eventPublisher,
                                  LoginTarpitService tarpitService) {
        this.ipResolver = ipResolver;
        this.eventPublisher = eventPublisher;
        this.tarpitService = tarpitService;
    }

    public GlobalExceptionHandler() {
        this(new ClientIpResolver(0), null, null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleInvalidCredentials(Exception ex, HttpServletRequest req) {
        String ip = ipResolver != null ? ipResolver.resolver(req) : "0.0.0.0";
        if (eventPublisher != null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken("anonymous", "[PROTECTED]");
            authToken.setDetails(ip);
            eventPublisher.publishEvent(
                    new AuthenticationFailureBadCredentialsEvent(authToken, new BadCredentialsException(ex.getMessage())));
        }
        if (tarpitService != null) {
            tarpitService.aplicarRetardo(ip);
        }
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(CuentaSuspendidaException.class)
    public ResponseEntity<ApiError> handleCuentaSuspendida(CuentaSuspendidaException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, ex.getMotivo() != null ? List.of(ex.getMotivo()) : List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Error de validacion", req, detalles);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {
        String param = ex.getName();
        String valor = ex.getValue() != null ? ex.getValue().toString() : "null";
        String mensaje = String.format("El parámetro '%s' con valor '%s' no tiene un formato válido", param, valor);
        return build(HttpStatus.BAD_REQUEST, mensaje, req, List.of("El valor debe corresponder al tipo esperado"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Acceso denegado: permisos insuficientes", req, List.of());
    }

    /** JSON ilegible en el cuerpo es un error del cliente, no un 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleCuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido", req, List.of());
    }

    /** Base de datos caida: 503 inmediato en vez de colgar la peticion (auditoria R-16). */
    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ApiError> handleBaseDeDatosCaida(Exception ex, HttpServletRequest req) {
        log.error("Base de datos no disponible en {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "El servicio no esta disponible en este momento. Intenta de nuevo en unos minutos.", req, List.of());
    }

    /** Nunca devuelve el mensaje interno (SQL, clases de Java) al cliente; queda en el log (auditoria R-09). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        // 404, 405, 413... de Spring conservan su codigo en vez de volverse 500.
        if (ex instanceof ErrorResponse er && HttpStatus.resolve(er.getStatusCode().value()) instanceof HttpStatus status) {
            return build(status, status.getReasonPhrase(), req, List.of());
        }
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno en el servidor", req, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest req, List<String> detalles) {
        ApiError body = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                detalles);
        return ResponseEntity.status(status).body(body);
    }
}
