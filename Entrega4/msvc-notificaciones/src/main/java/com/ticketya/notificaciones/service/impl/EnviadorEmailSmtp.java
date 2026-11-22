package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.config.NotificacionesProperties;
import com.ticketya.notificaciones.exception.EnvioEmailException;
import com.ticketya.notificaciones.service.EnviadorEmail;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envio real por SMTP (p. ej. Mailtrap) con JavaMailSender. Las credenciales salen de variables de entorno. */
@Component
public class EnviadorEmailSmtp implements EnviadorEmail {

    private final JavaMailSender mailSender;
    private final NotificacionesProperties propiedades;

    public EnviadorEmailSmtp(JavaMailSender mailSender, NotificacionesProperties propiedades) {
        this.mailSender = mailSender;
        this.propiedades = propiedades;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedades.getRemitente());
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            throw new EnvioEmailException("El servidor SMTP no acepto el mensaje: " + e.getMessage(), e);
        }
    }
}
