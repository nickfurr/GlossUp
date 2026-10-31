package com.glossup.recomendacion.infrastructure.out.envio;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.out.TransportadoraPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * API de envíos real (HU14), activa con GLOSSUP_ENVIOS_URL: POST {url}/envios con los datos del
 * paquete; responde {transportadora, guia}. La referencia hace idempotentes los reintentos.
 */
@Component
@ConditionalOnExpression("!'${glossup.envios.url:}'.isBlank()")
public class EnvioApiAdapter implements TransportadoraPort {

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(5);

    private final RestClient api;

    public EnvioApiAdapter(@Value("${glossup.envios.url}") String url, @Value("${glossup.envios.api-key:}") String apiKey) {
        // Sin tiempo máximo, una transportadora lenta dejaría colgado el hilo (RNF-19).
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(TIEMPO_MAXIMO);
        http.setReadTimeout(TIEMPO_MAXIMO);
        // Con búfer se envía Content-Length en vez de chunked, que muchas API rechazan.
        this.api = RestClient.builder().baseUrl(url).requestFactory(new BufferingClientHttpRequestFactory(http))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    record Paquete(String referencia, Direccion destino, String metodo, String fechaEstimada, List<Linea> contenido) {
        record Linea(String nombre, int cantidad) {}
    }

    @Override
    public Aceptacion solicitarEnvio(Pedido pedido) {
        var envio = pedido.getEnvio();
        Paquete paquete = new Paquete("GLOSSUP-" + pedido.getId(), envio.getDestino(), envio.getMetodo().name(),
                envio.getFechaEstimada().toString(),
                pedido.getItems().stream().map(i -> new Paquete.Linea(i.nombre(), i.cantidad())).toList());
        Aceptacion aceptacion = api.post().uri("/envios").contentType(MediaType.APPLICATION_JSON).body(paquete)
                .retrieve().body(Aceptacion.class);
        if (aceptacion == null || aceptacion.guia() == null || aceptacion.guia().isBlank()) {
            throw new IllegalStateException("La API de envíos no devolvió una guía");
        }
        return aceptacion;
    }
}
