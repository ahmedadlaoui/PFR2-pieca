package com.pieca.backend.services;

import com.pieca.backend.domain.entities.Offer;
import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.OfferStatus;
import com.pieca.backend.domain.enums.RequestStatus;
import com.pieca.backend.domain.enums.Role;
import com.pieca.backend.exceptions.BusinessViolationException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.exceptions.UnauthorizedActionException;
import com.pieca.backend.repositories.CategoryRepository;
import com.pieca.backend.repositories.OfferRepository;
import com.pieca.backend.repositories.RequestRepository;
import com.pieca.backend.repositories.SellerProfileRepository;
import com.pieca.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock RequestRepository requestRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock SellerProfileRepository sellerProfileRepository;
    @Mock OfferRepository offerRepository;
    @Mock EmailService emailService;
    @InjectMocks RequestService requestService;

    private User buyer() {
        return User.builder().id(1L).email("buyer@test.ma").role(Role.BUYER).firstName("Ali").build();
    }

    private User seller() {
        return User.builder().id(2L).email("seller@test.ma").role(Role.SELLER).firstName("Omar").build();
    }

    private Request pendingRequest(User buyer) {
        return Request.builder().id(10L).description("Ecran LCD | 24 pouces").status(RequestStatus.PENDING).buyer(buyer).build();
    }

    private Offer pendingOffer(User buyer, User seller) {
        Request req = pendingRequest(buyer);
        return Offer.builder().id(100L).price(BigDecimal.valueOf(500)).proofImageUrl("N/A")
                .status(OfferStatus.PENDING).request(req).seller(seller).build();
    }

    // ─── buyerAcceptOffer ──────────────────────────────────────────────────────

    @Test
    void buyerAcceptOffer_setsOfferAccepted_andRequestResolved() {
        User buyer = buyer();
        User seller = seller();
        Offer offer = pendingOffer(buyer, seller);

        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        requestService.buyerAcceptOffer(100L, "buyer@test.ma");

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(offer.getRequest().getStatus()).isEqualTo(RequestStatus.RESOLVED);
        verify(offerRepository).save(offer);
        verify(requestRepository).save(offer.getRequest());
        verify(emailService).sendOfferAcceptedNotification(any(), any(), any(), any());
    }

    @Test
    void buyerAcceptOffer_throws_whenOfferNotFound() {
        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer()));
        when(offerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.buyerAcceptOffer(999L, "buyer@test.ma"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buyerAcceptOffer_throws_whenBuyerIsNotOwnerOfOffer() {
        User realOwner = User.builder().id(99L).email("other@test.ma").role(Role.BUYER).build();
        User attacker = buyer();
        Offer offer = pendingOffer(realOwner, seller());

        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(attacker));
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> requestService.buyerAcceptOffer(100L, "buyer@test.ma"))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(offerRepository, never()).save(any());
    }

    @Test
    void buyerAcceptOffer_throws_whenOfferIsAlreadyProcessed() {
        User buyer = buyer();
        Offer offer = pendingOffer(buyer, seller());
        offer.setStatus(OfferStatus.ACCEPTED);

        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> requestService.buyerAcceptOffer(100L, "buyer@test.ma"))
                .isInstanceOf(BusinessViolationException.class);
    }

    @Test
    void buyerAcceptOffer_throws_whenCallerIsNotABuyer() {
        User seller = seller();
        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> requestService.buyerAcceptOffer(100L, "seller@test.ma"))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(offerRepository, never()).findById(any());
    }

    // ─── buyerDeclineOffer ─────────────────────────────────────────────────────

    @Test
    void buyerDeclineOffer_setsOfferRejected_andSendsEmail() {
        User buyer = buyer();
        User seller = seller();
        Offer offer = pendingOffer(buyer, seller);

        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        requestService.buyerDeclineOffer(100L, "buyer@test.ma");

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.REJECTED);
        verify(offerRepository).save(offer);
        verify(emailService).sendOfferDeclinedNotification(any(), any(), any());
    }

    @Test
    void buyerDeclineOffer_throws_whenOfferIsNotPending() {
        User buyer = buyer();
        Offer offer = pendingOffer(buyer, seller());
        offer.setStatus(OfferStatus.ACCEPTED);

        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> requestService.buyerDeclineOffer(100L, "buyer@test.ma"))
                .isInstanceOf(BusinessViolationException.class);

        verify(offerRepository, never()).save(any());
    }

    // ─── acceptRequest (seller submits an offer) ───────────────────────────────

    @Test
    void acceptRequest_savesOffer_andSendsEmailNotification() {
        User seller = seller();
        Request req = pendingRequest(buyer());

        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(offerRepository.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(false);

        requestService.acceptRequest(10L, "seller@test.ma", BigDecimal.valueOf(600));

        verify(offerRepository).save(argThat(o ->
                o.getPrice().compareTo(BigDecimal.valueOf(600)) == 0
                        && o.getStatus() == OfferStatus.PENDING
                        && o.getSeller().equals(seller)
        ));
        verify(emailService).sendOfferNotification(eq(req), eq(seller), any(BigDecimal.class));
    }

    @Test
    void acceptRequest_throws_whenSellerAlreadyMadeOffer() {
        User seller = seller();
        Request req = pendingRequest(buyer());

        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(offerRepository.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> requestService.acceptRequest(10L, "seller@test.ma", BigDecimal.valueOf(600)))
                .isInstanceOf(BusinessViolationException.class);

        verify(offerRepository, never()).save(any());
    }

    @Test
    void acceptRequest_throws_whenCallerIsBuyer() {
        User buyer = buyer();
        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> requestService.acceptRequest(10L, "buyer@test.ma", BigDecimal.valueOf(600)))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(requestRepository, never()).findById(any());
    }

    @Test
    void acceptRequest_throws_whenRequestNotFound() {
        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller()));
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.acceptRequest(999L, "seller@test.ma", BigDecimal.valueOf(100)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── cancelOffer ───────────────────────────────────────────────────────────

    @Test
    void cancelOffer_setsOfferCancelled() {
        User seller = seller();
        Offer offer = pendingOffer(buyer(), seller);

        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller));
        when(offerRepository.findByRequestIdAndSellerId(10L, 2L)).thenReturn(Optional.of(offer));

        requestService.cancelOffer(10L, "seller@test.ma");

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.CANCELLED);
        verify(offerRepository).save(offer);
    }

    @Test
    void cancelOffer_throws_whenOfferIsNotPending() {
        User seller = seller();
        Offer offer = pendingOffer(buyer(), seller);
        offer.setStatus(OfferStatus.ACCEPTED);

        when(userRepository.findByEmail("seller@test.ma")).thenReturn(Optional.of(seller));
        when(offerRepository.findByRequestIdAndSellerId(10L, 2L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> requestService.cancelOffer(10L, "seller@test.ma"))
                .isInstanceOf(BusinessViolationException.class);

        verify(offerRepository, never()).save(any());
    }

    @Test
    void cancelOffer_throws_whenCallerIsBuyer() {
        User buyer = buyer();
        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> requestService.cancelOffer(10L, "buyer@test.ma"))
                .isInstanceOf(UnauthorizedActionException.class);
    }
}
