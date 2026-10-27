package com.glossup.recomendacion.infrastructure.out.pago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.recomendacion.domain.model.TransaccionPago;
import com.glossup.recomendacion.domain.port.out.PasarelaPagosPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Pasarela Wompi (https://docs.wompi.co). El cliente paga en el Web Checkout de Wompi; GlossUP
 * firma el enlace con el secreto de integridad, verifica los eventos con el secreto de eventos y
 * consulta las transacciones a la API. Ninguna llave se escribe en el código: llegan por variables
 * de entorno (WOMPI_*).
 */
@Component
public class WompiPasarelaAdapter implements PasarelaPagosPort {

    private static final Logger log = LoggerFactory.getLogger(WompiPasarelaAdapter.class);
    private static final String MONEDA = "COP";
    private static final DateTimeFormatter FECHA_WOMPI =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(5);

    private final String llavePublica;
    private final String secretoIntegridad;
    private final String secretoEventos;
    private final String urlCheckout;
    private final RestClient api;
    private final ObjectMapper json;

    public WompiPasarelaAdapter(@Value("${glossup.pagos.wompi.llave-publica:}") String llavePublica,
                                @Value("${glossup.pagos.wompi.secreto-integridad:}") String secretoIntegridad,
                                @Value("${glossup.pagos.wompi.secreto-eventos:}") String secretoEventos,
                                @Value("${glossup.pagos.wompi.api-url:https://sandbox.wompi.co/v1}") String urlApi,
                                @Value("${glossup.pagos.wompi.checkout-url:https://checkout.wompi.co/p/}") String urlCheckout,
                                ObjectMapper json) {
        this.llavePublica = llavePublica;
        this.secretoIntegridad = secretoIntegridad;
        this.secretoEventos = secretoEventos;
        this.urlCheckout = urlCheckout;
        this.json = json;
        // Sin tiempo máximo, una pasarela lenta dejaría colgado el hilo del servidor (RNF-19).
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(TIEMPO_MAXIMO);
        http.setReadTimeout(TIEMPO_MAXIMO);
        this.api = RestClient.builder().baseUrl(urlApi).requestFactory(http).build();
    }

    @Override
    public String urlPago(String referencia, long montoCentavos, String urlRetorno, OffsetDateTime expira) {
        exigirConfiguracion(llavePublica, secretoIntegridad);
        String expiracion = FECHA_WOMPI.format(expira);
        // Orden exigido por Wompi: referencia, monto en centavos, moneda, expiración y secreto de integridad.
        String firma = sha256(referencia + montoCentavos + MONEDA + expiracion + secretoIntegridad);
        return UriComponentsBuilder.fromHttpUrl(urlCheckout)
                .queryParam("public-key", llavePublica)
                .queryParam("currency", MONEDA)
                .queryParam("amount-in-cents", montoCentavos)
                .queryParam("reference", referencia)
                .queryParam("signature:integrity", firma)
                .queryParam("expiration-time", expiracion)
                .queryParam("redirect-url", urlRetorno)
                .encode()
                .toUriString();
    }

    @Override
    public Optional<TransaccionPago> consultar(String transaccionId) {
        exigirConfiguracion(llavePublica);
        try {
            JsonNode respuesta = api.get()
                    .uri("/transactions/{id}", transaccionId)
                    .header("Authorization", "Bearer " + llavePublica)
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.ofNullable(respuesta).map(r -> aTransaccion(r.path("data")));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public Optional<TransaccionPago> leerEvento(String cuerpo) {
        if (secretoEventos.isBlank()) {
            log.warn("Llegó un evento de Wompi pero WOMPI_SECRETO_EVENTOS no está configurado: se ignora");
            return Optional.empty();
        }
        JsonNode evento;
        try {
            evento = json.readTree(cuerpo);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
        // Checksum = SHA256(valores de signature.properties en orden + timestamp + secreto de eventos).
        StringBuilder firmado = new StringBuilder();
        for (JsonNode propiedad : evento.path("signature").path("properties")) {
            JsonNode valor = evento.path("data");
            for (String parte : propiedad.asText().split("\\.")) {
                valor = valor.path(parte);
            }
            firmado.append(valor.asText());
        }
        firmado.append(evento.path("timestamp").asText()).append(secretoEventos);
        String esperado = evento.path("signature").path("checksum").asText().toLowerCase();
        boolean valido = MessageDigest.isEqual(sha256(firmado.toString()).getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            log.warn("Evento de Wompi con firma inválida: se ignora");
            return Optional.empty();
        }
        if (!"transaction.updated".equals(evento.path("event").asText())) {
            return Optional.empty();
        }
        return Optional.of(aTransaccion(evento.path("data").path("transaction")));
    }

    private static TransaccionPago aTransaccion(JsonNode t) {
        TransaccionPago.Estado estado = switch (t.path("status").asText()) {
            case "APPROVED" -> TransaccionPago.Estado.APROBADA;
            case "PENDING" -> TransaccionPago.Estado.PENDIENTE;
            case "DECLINED" -> TransaccionPago.Estado.RECHAZADA;
            case "VOIDED" -> TransaccionPago.Estado.ANULADA;
            default -> TransaccionPago.Estado.ERROR; // un estado desconocido nunca cuenta como pago
        };
        return new TransaccionPago(t.path("id").asText(), t.path("reference").asText(), estado,
                t.path("amount_in_cents").asLong(), t.path("currency").asText(), t.path("payment_method_type").asText());
    }

    private static void exigirConfiguracion(String... valores) {
        for (String v : valores) {
            if (v == null || v.isBlank()) {
                throw new ReglaNegocioException("Los pagos no están disponibles: la pasarela Wompi no está configurada");
            }
        }
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
