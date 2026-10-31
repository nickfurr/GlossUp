package com.glossup.recomendacion.infrastructure.out.correo;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorreoAvisoEnvioTest {

    private static final ClientePort.Cliente ANA = new ClientePort.Cliente("Ana", "ana@correo.co");

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> con(JavaMailSender servidor) {
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(servidor);
        return proveedor;
    }

    private static Pedido enEtapa(EstadoEnvio etapa) {
        Envio envio = new Envio(new Direccion(null, null, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle 1", null, null, false),
                MetodoEnvio.ESTANDAR, Dinero.de(6000), LocalDate.of(2026, 11, 4));
        envio.aceptado("Coordinadora", "C-5", OffsetDateTime.now());
        Pedido pedido = new Pedido(5L, 7L, EstadoPedido.PAGADO, List.of(new PedidoItem(100L, 1, Dinero.de(10000))), OffsetDateTime.now(), envio);
        pedido.actualizarEnvio(etapa, OffsetDateTime.now());
        return pedido;
    }

    private static SimpleMailMessage enviado(EstadoEnvio etapa) {
        JavaMailSender servidor = mock(JavaMailSender.class);
        new CorreoNotificacionAdapter(con(servidor), "tienda@glossup.co", "http://localhost:4200/").avisarEnvio(ANA, enEtapa(etapa));
        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(servidor).send(mensaje.capture());
        return mensaje.getValue();
    }

    @Test
    void cadaEtapaTieneSuAviso() {
        SimpleMailMessage enCamino = enviado(EstadoEnvio.EN_CAMINO);
        assertTrue(enCamino.getSubject().contains("va en camino"));
        assertTrue(enCamino.getText().contains("Coordinadora"));
        assertTrue(enCamino.getText().contains("4 de noviembre"));
        assertTrue(enCamino.getText().contains("http://localhost:4200/pedidos?pedido=5"));
        assertTrue(enviado(EstadoEnvio.ENTREGADO).getSubject().contains("fue entregado"));
        assertTrue(enviado(EstadoEnvio.NO_ENTREGADO).getSubject().contains("No pudimos entregar"));
    }

    @Test
    void sinCorreoConfigurado_noEnvia() {
        JavaMailSender servidor = mock(JavaMailSender.class);
        new CorreoNotificacionAdapter(con(servidor), "", "http://localhost:4200").avisarEnvio(ANA, enEtapa(EstadoEnvio.EN_CAMINO));
        verify(servidor, never()).send(any(SimpleMailMessage.class));
    }
}
