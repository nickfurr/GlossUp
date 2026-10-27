package com.glossup.recomendacion.infrastructure.out.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.recomendacion.domain.model.TransaccionPago;
import com.glossup.shared.domain.ReglaNegocioException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Firmas contra valores SHA256 calculados por fuera (Python hashlib), siguiendo las fórmulas de la
 * documentación de Wompi: así la prueba no repite la implementación que verifica.
 */
class WompiPasarelaAdapterTest {

    private static final String EVENTO = """
            {"event":"transaction.updated","environment":"test","timestamp":1530291411,
             "data":{"transaction":{"id":"1234-1610641025-49201","amount_in_cents":4490000,
               "reference":"GLOSSUP-7-1700000000000","currency":"COP","payment_method_type":"NEQUI","status":"APPROVED"}},
             "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],
               "checksum":"%s"}}""";
    private static final String CHECKSUM_VALIDO = "177a142e25beca66d59f857f08b24ca96fb01b5cb39911a05940ea0cb8144a5d";

    private HttpServer servidor;

    @AfterEach
    void apagar() {
        if (servidor != null) servidor.stop(0);
    }

    private WompiPasarelaAdapter adapter(String apiUrl) {
        return new WompiPasarelaAdapter("pub_test_llave", "test_integrity_secreto", "test_events_secreto",
                apiUrl, "https://checkout.wompi.co/p/", new ObjectMapper());
    }

    @Test
    void urlPago_firmaConReferenciaMontoMonedaExpiracionYSecreto() {
        String url = adapter("http://localhost").urlPago("GLOSSUP-7-1700000000000", 4_490_000,
                "https://glossup.co/pago/resultado/7", OffsetDateTime.of(2026, 10, 27, 11, 30, 0, 0, ZoneOffset.ofHours(-5)));

        Map<String, String> p = UriComponentsBuilder.fromUriString(url).build().getQueryParams().toSingleValueMap();
        assertTrue(url.startsWith("https://checkout.wompi.co/p/?"));
        assertEquals("pub_test_llave", p.get("public-key"));
        assertEquals("COP", p.get("currency"));
        assertEquals("4490000", p.get("amount-in-cents"));
        assertEquals("2026-10-27T16:30:00.000Z", p.get("expiration-time"), "la expiración viaja en UTC");
        assertEquals("e7cd7652c3a3dba0a8e3e27d19f99bd73d72fe3ce92b5d4d60290be6d02e353e", p.get("signature:integrity"));
        assertFalse(url.contains("test_integrity_secreto"), "el secreto nunca viaja en la URL");
    }

    @Test
    void evento_conFirmaValidaDevuelveLaTransaccion() {
        Optional<TransaccionPago> t = adapter("http://localhost").leerEvento(EVENTO.formatted(CHECKSUM_VALIDO));
        assertTrue(t.isPresent());
        assertEquals(TransaccionPago.Estado.APROBADA, t.get().estado());
        assertEquals(4_490_000, t.get().montoCentavos());
        assertEquals("GLOSSUP-7-1700000000000", t.get().referencia());
        assertEquals("NEQUI", t.get().metodo());
    }

    @Test
    void evento_alteradoOConOtroSecretoSeRechaza() {
        String alterado = EVENTO.formatted(CHECKSUM_VALIDO).replace("4490000", "100");
        assertTrue(adapter("http://localhost").leerEvento(alterado).isEmpty(), "monto cambiado tras firmar");

        var otroSecreto = new WompiPasarelaAdapter("pub_test_llave", "x", "otro_secreto", "http://localhost",
                "https://checkout.wompi.co/p/", new ObjectMapper());
        assertTrue(otroSecreto.leerEvento(EVENTO.formatted(CHECKSUM_VALIDO)).isEmpty());
        assertTrue(adapter("http://localhost").leerEvento("no es json").isEmpty());
    }

    @Test
    void consultar_llamaALaApiConLaLlavePublicaYMapeaElEstado() throws Exception {
        AtomicReference<String> autorizacion = new AtomicReference<>();
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/v1/transactions/", ex -> {
            autorizacion.set(ex.getRequestHeaders().getFirst("Authorization"));
            boolean existe = ex.getRequestURI().getPath().endsWith("/tx-1");
            byte[] cuerpo = (existe
                    ? "{\"data\":{\"id\":\"tx-1\",\"reference\":\"GLOSSUP-7-1\",\"status\":\"DECLINED\",\"amount_in_cents\":500,\"currency\":\"COP\",\"payment_method_type\":\"CARD\"}}"
                    : "{\"error\":{\"type\":\"NOT_FOUND_ERROR\"}}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(existe ? 200 : 404, cuerpo.length);
            ex.getResponseBody().write(cuerpo);
            ex.close();
        });
        servidor.start();
        var wompi = adapter("http://localhost:" + servidor.getAddress().getPort() + "/v1");

        TransaccionPago t = wompi.consultar("tx-1").orElseThrow();
        assertEquals(TransaccionPago.Estado.RECHAZADA, t.estado());
        assertEquals("Bearer pub_test_llave", autorizacion.get());
        assertTrue(wompi.consultar("no-existe").isEmpty());
    }

    @Test
    void sinLlavesConfiguradasNoSeOfrecePagar() {
        var sinConfigurar = new WompiPasarelaAdapter("", "", "", "http://localhost", "https://checkout.wompi.co/p/",
                new ObjectMapper());
        assertThrows(ReglaNegocioException.class, () -> sinConfigurar.urlPago("R", 100, "http://x", OffsetDateTime.now()));
        assertTrue(sinConfigurar.leerEvento(EVENTO.formatted(CHECKSUM_VALIDO)).isEmpty());
    }
}
