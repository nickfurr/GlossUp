package com.glossup.recomendacion.infrastructure.out.correo;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.NotificacionPort;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Envía el comprobante por correo (HU11) con el SMTP de las variables SPRING_MAIL_HOST, _PORT,
 * _USERNAME y _PASSWORD. Sin ellas no envía nada: el comprobante igual se descarga en "Mis pedidos".
 */
@Component
public class CorreoNotificacionAdapter implements NotificacionPort {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificacionAdapter.class);

    private final ObjectProvider<JavaMailSender> correo;
    private final String remitente;
    private final String urlPublica;

    public CorreoNotificacionAdapter(ObjectProvider<JavaMailSender> correo,
                                     @Value("${glossup.correo.remitente:}") String remitente,
                                     @Value("${glossup.url-publica}") String urlPublica) {
        this.correo = correo;
        this.remitente = remitente;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    @Override
    public void enviarComprobante(ClientePort.Cliente cliente, Pedido pedido, Comprobante comprobante) {
        JavaMailSender servidor = correo.getIfAvailable();
        if (servidor == null || remitente.isBlank()) {
            log.info("Correo no configurado: el comprobante {} no se envía (queda para descargar)", comprobante.numero());
            return;
        }
        NumberFormat cop = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        cop.setMaximumFractionDigits(0);
        try {
            var mensaje = servidor.createMimeMessage();
            var m = new MimeMessageHelper(mensaje, true, "UTF-8");
            m.setFrom(remitente, "GlossUP");
            m.setTo(cliente.email());
            m.setSubject("Tu pago fue aprobado · Pedido #" + pedido.getId() + " · GlossUP");
            m.setText("""
                    Hola %s:

                    Recibimos el pago de tu pedido #%d por %s. Ya lo estamos preparando.

                    Adjuntamos tu comprobante de compra %s. También puedes verlo y descargarlo cuando quieras en Mis pedidos:
                    %s/pedidos

                    Gracias por comprar en GlossUP.
                    """.formatted(cliente.nombre(), pedido.getId(), cop.format(pedido.getTotal().monto()),
                    comprobante.numero(), urlPublica));
            m.addAttachment("comprobante-" + comprobante.numero() + ".pdf", new ByteArrayResource(comprobante.pdf()), "application/pdf");
            servidor.send(mensaje);
            log.info("Comprobante {} enviado al cliente del pedido #{}", comprobante.numero(), pedido.getId());
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new MailSendException("No se pudo armar el correo del comprobante", e);
        }
    }
}
