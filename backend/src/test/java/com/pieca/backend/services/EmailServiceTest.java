package com.pieca.backend.services;

import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService emailService;

    private User buyer(String email) {
        return User.builder().id(1L).email(email).firstName("Ali").build();
    }

    private User seller(String email) {
        return User.builder().id(2L).email(email).firstName("Omar").build();
    }

    private Request request(User buyer) {
        return Request.builder().id(10L).description("Ecran LCD | 24 pouces").buyer(buyer).build();
    }

    @Test
    void sendOfferNotification_sendsEmailToBuyer() {
        User buyer = buyer("buyer@test.ma");
        User seller = seller("seller@test.ma");
        Request req = request(buyer);

        emailService.sendOfferNotification(req, seller, BigDecimal.valueOf(500));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("buyer@test.ma");
        assertThat(sent.getSubject()).contains("PIECA");
        assertThat(sent.getText()).contains("500");
    }

    @Test
    void sendOfferNotification_doesNotSend_whenBuyerEmailIsNull() {
        User buyer = User.builder().id(1L).firstName("Ali").build();
        User seller = seller("seller@test.ma");
        Request req = request(buyer);

        emailService.sendOfferNotification(req, seller, BigDecimal.valueOf(300));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOfferNotification_doesNotSend_whenBuyerIsNull() {
        Request req = Request.builder().id(10L).description("Test").buyer(null).build();
        User seller = seller("seller@test.ma");

        emailService.sendOfferNotification(req, seller, BigDecimal.valueOf(300));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOfferAcceptedNotification_sendsEmailToSeller() {
        User buyer = buyer("buyer@test.ma");
        User seller = seller("seller@test.ma");
        Request req = request(buyer);

        emailService.sendOfferAcceptedNotification(req, seller, buyer, BigDecimal.valueOf(750));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("seller@test.ma");
        assertThat(sent.getSubject()).containsIgnoringCase("acceptee");
    }

    @Test
    void sendOfferAcceptedNotification_doesNotSend_whenSellerEmailIsNull() {
        User buyer = buyer("buyer@test.ma");
        User seller = User.builder().id(2L).firstName("Omar").build();
        Request req = request(buyer);

        emailService.sendOfferAcceptedNotification(req, seller, buyer, BigDecimal.valueOf(750));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOfferDeclinedNotification_sendsEmailToSeller() {
        User buyer = buyer("buyer@test.ma");
        User seller = seller("seller@test.ma");
        Request req = request(buyer);

        emailService.sendOfferDeclinedNotification(req, seller, buyer);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).contains("seller@test.ma");
    }

    @Test
    void sendOfferDeclinedNotification_doesNotSend_whenSellerEmailIsNull() {
        User buyer = buyer("buyer@test.ma");
        User seller = User.builder().id(2L).firstName("Omar").build();
        Request req = request(buyer);

        emailService.sendOfferDeclinedNotification(req, seller, buyer);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
