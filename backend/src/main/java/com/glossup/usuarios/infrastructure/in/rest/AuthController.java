package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.shared.security.CookieSesion;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.AutenticacionResultado;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioUseCase;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.glossup.usuarios.infrastructure.in.rest.dto.LoginRequest;
import com.glossup.usuarios.infrastructure.in.rest.dto.LoginResponse;
import com.glossup.usuarios.infrastructure.in.rest.dto.RegistrarUsuarioRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final CookieSesion cookieSesion;

    public AuthController(AutenticarUsuarioUseCase autenticarUsuarioUseCase,
                          RegistrarUsuarioUseCase registrarUsuarioUseCase,
                          CookieSesion cookieSesion) {
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.cookieSesion = cookieSesion;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse respuesta) {
        AutenticacionResultado resultado = autenticarUsuarioUseCase.autenticar(
                new AutenticarUsuarioCommand(request.email(), request.password()));
        Set<String> roles = resultado.usuario().getRoles().stream()
                .map(rol -> rol.name())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        // addHeader y no ResponseEntity.header: este reemplazaría la cookie XSRF-TOKEN que ya agregó Spring Security.
        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookieSesion.abrir(resultado.token()));
        return new LoginResponse(resultado.usuario().getId(), resultado.usuario().getEmail(),
                resultado.usuario().getNombre(), roles);
    }

    /** Cierra la sesión de este navegador. Para cerrar las de todos los dispositivos: POST /api/cuenta/cerrar-sesiones. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse respuesta) {
        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookieSesion.cerrar());
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrarUsuarioRequest request) {
        registrarUsuarioUseCase.registrar(new RegistrarUsuarioCommand(
                request.email(), request.nombre(), request.password(), request.rol(),
                request.aceptaPolitica(), request.mayorDeEdad()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
