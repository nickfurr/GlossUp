package com.glossup.recomendacion.infrastructure.out.envio;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.TransportadoraPort;
import com.glossup.shared.domain.Dinero;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EnvioApiAdapterTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> largo = new AtomicReference<>();

    private String api(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/envios", intercambio -> {
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            largo.set(intercambio.getRequestHeaders().getFirst("Content-Length"));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    private static Pedido pedido() {
        Envio envio = new Envio(new Direccion(null, null, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle 1", null, null, false),
                MetodoEnvio.EXPRESS, Dinero.de(10000), LocalDate.of(2026, 11, 4));
        return new Pedido(5L, 7L, EstadoPedido.PAGADO, List.of(new PedidoItem(100L, "Sérum", 2, Dinero.de(10000))),
                OffsetDateTime.now(), envio);
    }

    @Test
    void enviaElPaqueteYDevuelveLaGuia() throws IOException {
        var aceptacion = new EnvioApiAdapter(api(201, "{\"transportadora\":\"Coordinadora\",\"guia\":\"C-123\"}"), "llave")
                .solicitarEnvio(pedido());

        assertEquals(new TransportadoraPort.Aceptacion("Coordinadora", "C-123"), aceptacion);
        assertEquals("Bearer llave", autorizacion.get());
        assertNotNull(largo.get(), "muchas API rechazan los cuerpos chunked");
        for (String esperado : List.of("GLOSSUP-5", "Calle 1", "EXPRESS", "Sérum", "2026-11-04")) {
            assertTrue(cuerpo.get().contains(esperado), () -> "falta «" + esperado + "» en " + cuerpo.get());
        }
    }

    @Test
    void siLaApiFallaOnoDaGuia_lanzaParaReintentar() throws IOException {
        EnvioApiAdapter caida = new EnvioApiAdapter(api(503, "{}"), "llave");
        assertThrows(RuntimeException.class, () -> caida.solicitarEnvio(pedido()));
        servidor.stop(0);

        EnvioApiAdapter sinGuia = new EnvioApiAdapter(api(200, "{\"transportadora\":\"Coordinadora\"}"), "llave");
        assertThrows(IllegalStateException.class, () -> sinGuia.solicitarEnvio(pedido()));
    }
}
