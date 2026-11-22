package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.config.NotificacionesProperties;
import com.ticketya.notificaciones.exception.EnvioEmailException;
import com.ticketya.notificaciones.service.impl.EnviadorEmailSmtp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnviadorEmailSmtpTest {

    @Mock
    private JavaMailSender mailSender;

    private EnviadorEmailSmtp enviador;

    @BeforeEach
    void setUp() {
        NotificacionesProperties propiedades = new NotificacionesProperties();
        propiedades.setRemitente("no-reply@ticketya.test");
        enviador = new EnviadorEmailSmtp(mailSender, propiedades);
    }

    @Test
    void armaElMensajeConRemitenteDestinatarioAsuntoYCuerpo() {
        enviador.enviar("ana@correo.com", "Tu compra", "Hola Ana");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertEquals("no-reply@ticketya.test", mensaje.getFrom());
        assertArrayEquals(new String[]{"ana@correo.com"}, mensaje.getTo());
        assertEquals("Tu compra", mensaje.getSubject());
        assertEquals("Hola Ana", mensaje.getText());
    }

    @Test
    void traduceLosErroresDelServidorSmtp() {
        doThrow(new MailSendException("smtp caido")).when(mailSender).send(any(SimpleMailMessage.class));

        EnvioEmailException error = assertThrows(EnvioEmailException.class,
                () -> enviador.enviar("ana@correo.com", "Tu compra", "Hola Ana"));

        assertEquals(MailSendException.class, error.getCause().getClass());
    }
}
