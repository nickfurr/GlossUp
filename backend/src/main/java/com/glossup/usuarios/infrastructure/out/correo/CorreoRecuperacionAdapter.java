package com.glossup.usuarios.infrastructure.out.correo;

import com.glossup.usuarios.domain.port.out.AvisoCuentaPort;
import com.glossup.usuarios.domain.port.out.CorreoRecuperacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** En otro hilo: si la respuesta esperara al correo, el tiempo delataría qué direcciones están registradas. */
@Component
public class CorreoRecuperacionAdapter implements CorreoRecuperacionPort, AvisoCuentaPort {

    private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacionAdapter.class);

    private final ObjectProvider<JavaMailSender> correo;
    private final String remitente;

    public CorreoRecuperacionAdapter(ObjectProvider<JavaMailSender> correo, @Value("${glossup.correo.remitente:}") String remitente) {
        this.correo = correo;
        this.remitente = remitente;
    }

    @Async
    @Override
    public void enviarEnlace(String email, String nombre, String enlace) {
        JavaMailSender servidor = correo.getIfAvailable();
        if (servidor == null || remitente.isBlank()) {
            log.info("Correo no configurado: el enlace de recuperación no se envía");
            return;
        }
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(email);
        mensaje.setSubject("Recupera tu contraseña · GlossUP");
        mensaje.setText("""
                Hola %s:

                Para crear una contraseña nueva abre este enlace. Solo sirve una vez y vence pronto:
                %s

                Si no lo pediste, ignora este correo: tu contraseña sigue igual.
                """.formatted(nombre, enlace));
        try {
            servidor.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el enlace de recuperación: {}", e.getClass().getSimpleName()); // sin el mensaje: trae el correo (OBS-T14)
        }
    }

    @Async
    @Override
    public void avisarAprobacion(String email, String nombre) {
        JavaMailSender servidor = correo.getIfAvailable();
        if (servidor == null || remitente.isBlank()) {
            return;
        }
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(email);
        mensaje.setSubject("Tu cuenta de vendedor fue aprobada · GlossUP");
        mensaje.setText("""
                Hola %s:

                Un administrador revisó y aprobó tu cuenta de vendedor. Ya puedes iniciar sesión y publicar tus productos.

                Gracias por vender en GlossUP.
                """.formatted(nombre));
        try {
            servidor.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el aviso de aprobación: {}", e.getClass().getSimpleName());
        }
    }
}
