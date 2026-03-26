package com.pieca.backend.services;

import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOfferNotification(Request request, User seller, BigDecimal price) {
        try {
            User buyer = request.getBuyer();
            if (buyer == null || buyer.getEmail() == null) {
                log.warn("Cannot send email: buyer or buyer email is null for request {}", request.getId());
                return;
            }

            String sellerName = (seller.getFirstName() != null ? seller.getFirstName() : "") + " " +
                    (seller.getLastName() != null ? seller.getLastName() : "");

            String title = request.getDescription();
            if (title != null && title.contains(" | ")) {
                title = title.substring(0, title.indexOf(" | "));
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@pieca.ma");
            message.setTo(buyer.getEmail());
            message.setSubject("Nouvelle offre sur votre demande - PIECA");
            message.setText(
                    "Bonjour " + (buyer.getFirstName() != null ? buyer.getFirstName() : "") + ",\n\n" +
                    "Un vendeur a repondu a votre demande !\n\n" +
                    "Demande : " + (title != null ? title : "N/A") + "\n" +
                    "Vendeur : " + sellerName.trim() + "\n" +
                    "Prix propose : " + price + " MAD\n\n" +
                    "Connectez-vous a PIECA pour consulter les details de cette offre.\n\n" +
                    "Cordialement,\n" +
                    "L'equipe PIECA"
            );

            mailSender.send(message);
            log.info("Email notification sent to {} for request {}", buyer.getEmail(), request.getId());
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendOfferAcceptedNotification(Request request, User seller, User buyer, BigDecimal price) {
        try {
            if (seller == null || seller.getEmail() == null) {
                log.warn("Cannot send email: seller or seller email is null for request {}", request.getId());
                return;
            }

            String buyerName = (buyer.getFirstName() != null ? buyer.getFirstName() : "") + " " +
                    (buyer.getLastName() != null ? buyer.getLastName() : "");

            String title = request.getDescription();
            if (title != null && title.contains(" | ")) {
                title = title.substring(0, title.indexOf(" | "));
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@pieca.ma");
            message.setTo(seller.getEmail());
            message.setSubject("Votre offre a ete acceptee ! - PIECA");
            message.setText(
                    "Bonjour " + (seller.getFirstName() != null ? seller.getFirstName() : "") + ",\n\n" +
                    "Bonne nouvelle ! Un acheteur a accepte votre offre.\n\n" +
                    "Demande : " + (title != null ? title : "N/A") + "\n" +
                    "Acheteur : " + buyerName.trim() + "\n" +
                    "Prix accepte : " + price + " MAD\n\n" +
                    "Connectez-vous a PIECA pour voir les details et contacter l'acheteur.\n\n" +
                    "Cordialement,\n" +
                    "L'equipe PIECA"
            );

            mailSender.send(message);
            log.info("Offer accepted email sent to {} for request {}", seller.getEmail(), request.getId());
        } catch (Exception e) {
            log.error("Failed to send offer accepted email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOfferDeclinedNotification(Request request, User seller, User buyer) {
        try {
            if (seller == null || seller.getEmail() == null) {
                log.warn("Cannot send email: seller or seller email is null for request {}", request.getId());
                return;
            }

            String buyerName = (buyer.getFirstName() != null ? buyer.getFirstName() : "") + " " +
                    (buyer.getLastName() != null ? buyer.getLastName() : "");

            String title = request.getDescription();
            if (title != null && title.contains(" | ")) {
                title = title.substring(0, title.indexOf(" | "));
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@pieca.ma");
            message.setTo(seller.getEmail());
            message.setSubject("Votre offre a ete refusee - PIECA");
            message.setText(
                    "Bonjour " + (seller.getFirstName() != null ? seller.getFirstName() : "") + ",\n\n" +
                    "Malheureusement, l'acheteur a decline votre offre.\n\n" +
                    "Demande : " + (title != null ? title : "N/A") + "\n" +
                    "Acheteur : " + buyerName.trim() + "\n\n" +
                    "Continuez a repondre aux demandes pour augmenter vos chances.\n\n" +
                    "Cordialement,\n" +
                    "L'equipe PIECA"
            );

            mailSender.send(message);
            log.info("Offer declined email sent to {} for request {}", seller.getEmail(), request.getId());
        } catch (Exception e) {
            log.error("Failed to send offer declined email: {}", e.getMessage());
        }
    }
}
