package com.fintech.userservice.service;

import com.fintech.userservice.dto.request.kyc.KycDocumentReviewRequest;
import com.fintech.userservice.dto.request.kyc.KycDocumentSubmitRequest;
import com.fintech.userservice.dto.response.kyc.KycDocumentResponse;
import com.fintech.userservice.dto.response.kyc.KycStatusResponse;
import com.fintech.userservice.entity.KycDocument;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.entity.enums.DocumentType;
import com.fintech.userservice.entity.enums.KycLevel;
import com.fintech.userservice.entity.enums.KycStatus;
import com.fintech.userservice.messaging.KycEventKafkaPublisher;
import com.fintech.userservice.repository.KycDocumentRepository;
import com.fintech.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private KycEventKafkaPublisher kycEventKafkaPublisher;

    @InjectMocks
    private KycService kycService;

    private UserProfile sampleProfile;
    private KycDocument sampleDocument;

    @BeforeEach
    void setUp() {
        sampleProfile = new UserProfile(
                "user-123", "John", "Doe", "john@example.com",
                "+1234567890", "123 Main St", "1990-01-01",
                "Engineer", 1000.0, "ACCOUNT_HOLDER", "000000000001"
        );

        sampleDocument = new KycDocument(
                "user-123", DocumentType.PASSPORT, "AB123456",
                "kyc/user-123/test_passport", LocalDate.of(2030, 12, 31)
        );
    }

    @Nested
    @DisplayName("submitDocument")
    class SubmitDocument {

        @Test
        @DisplayName("should submit document successfully")
        void shouldSubmitDocumentSuccessfully() {
            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.existsByUserIdAndDocumentTypeAndStatusIn(
                    eq("user-123"), eq(DocumentType.PASSPORT), anyList())).thenReturn(false);
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(sampleDocument);

            KycDocumentResponse response = kycService.submitDocument("user-123", request);

            assertThat(response).isNotNull();
            assertThat(response.getDocumentType()).isEqualTo(DocumentType.PASSPORT);
            verify(kycDocumentRepository).save(any(KycDocument.class));
            verify(kycEventKafkaPublisher).publishKycSubmitted(eq("user-123"), any(), eq("PASSPORT"));
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycService.submitDocument("non-existent", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw when duplicate active document exists")
        void shouldThrowWhenDuplicateExists() {
            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.existsByUserIdAndDocumentTypeAndStatusIn(
                    eq("user-123"), eq(DocumentType.PASSPORT), anyList())).thenReturn(true);

            assertThatThrownBy(() -> kycService.submitDocument("user-123", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already pending or under review");
        }

        @Test
        @DisplayName("should update user kyc status to PENDING when level is NONE")
        void shouldUpdateUserKycStatusToPending() {
            sampleProfile.setKycLevel(KycLevel.NONE);
            sampleProfile.setKycStatus(KycStatus.APPROVED); // Not PENDING

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.existsByUserIdAndDocumentTypeAndStatusIn(
                    eq("user-123"), eq(DocumentType.PASSPORT), anyList())).thenReturn(false);
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(sampleDocument);

            kycService.submitDocument("user-123", request);

            verify(userProfileRepository).save(sampleProfile);
            assertThat(sampleProfile.getKycStatus()).isEqualTo(KycStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("getUserDocuments")
    class GetUserDocuments {

        @Test
        @DisplayName("should return documents for user")
        void shouldReturnDocumentsForUser() {
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of(sampleDocument));

            List<KycDocumentResponse> documents = kycService.getUserDocuments("user-123", false);

            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getDocumentType()).isEqualTo(DocumentType.PASSPORT);
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyWhenNoDocuments() {
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of());

            List<KycDocumentResponse> documents = kycService.getUserDocuments("user-123", false);

            assertThat(documents).isEmpty();
        }

        @Test
        @DisplayName("should mask document number for non-admin")
        void shouldMaskDocumentNumberForNonAdmin() {
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of(sampleDocument));

            List<KycDocumentResponse> documents = kycService.getUserDocuments("user-123", false);

            assertThat(documents.get(0).getDocumentNumber()).contains("****");
        }
    }

    @Nested
    @DisplayName("reviewDocument")
    class ReviewDocument {

        @Test
        @DisplayName("should approve document successfully")
        void shouldApproveDocumentSuccessfully() {
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);

            when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(i -> i.getArgument(0));
            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of(sampleDocument));
            when(kycDocumentRepository.findByUserIdAndStatus("user-123", KycStatus.APPROVED))
                    .thenReturn(List.of(sampleDocument));

            KycDocumentResponse response = kycService.reviewDocument(1L, "admin-1", request);

            assertThat(response.getStatus()).isEqualTo(KycStatus.APPROVED);
            verify(kycEventKafkaPublisher).publishKycApproved("user-123", 1L, "admin-1");
        }

        @Test
        @DisplayName("should reject document with reason")
        void shouldRejectDocumentWithReason() {
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(false, "Document is expired");

            when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(i -> i.getArgument(0));
            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of(sampleDocument));

            KycDocumentResponse response = kycService.reviewDocument(1L, "admin-1", request);

            assertThat(response.getStatus()).isEqualTo(KycStatus.REJECTED);
            assertThat(response.getRejectionReason()).isEqualTo("Document is expired");
            verify(kycEventKafkaPublisher).publishKycRejected("user-123", 1L, "Document is expired", "admin-1");
        }

        @Test
        @DisplayName("should throw when document not found")
        void shouldThrowWhenDocumentNotFound() {
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);
            when(kycDocumentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycService.reviewDocument(999L, "admin-1", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("should throw on self-review")
        void shouldThrowOnSelfReview() {
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);
            when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

            assertThatThrownBy(() -> kycService.reviewDocument(1L, "user-123", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot review your own");
        }

        @Test
        @DisplayName("should throw when rejecting without reason")
        void shouldThrowWhenRejectingWithoutReason() {
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(false, null);
            when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

            assertThatThrownBy(() -> kycService.reviewDocument(1L, "admin-1", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rejection reason is required");
        }

        @Test
        @DisplayName("should throw when document is already approved")
        void shouldThrowWhenDocumentAlreadyApproved() {
            sampleDocument.setStatus(KycStatus.APPROVED);
            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);
            when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

            assertThatThrownBy(() -> kycService.reviewDocument(1L, "admin-1", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be reviewed in its current state");
        }
    }

    @Nested
    @DisplayName("getKycLevel")
    class GetKycLevel {

        @Test
        @DisplayName("should return NONE when no approved documents")
        void shouldReturnNoneWhenNoApprovedDocs() {
            when(kycDocumentRepository.findByUserIdAndStatus("1", KycStatus.APPROVED))
                    .thenReturn(List.of());

            KycLevel level = kycService.getKycLevel(1L);

            assertThat(level).isEqualTo(KycLevel.NONE);
        }

        @Test
        @DisplayName("should return BASIC with one ID document")
        void shouldReturnBasicWithOneIdDoc() {
            KycDocument passport = new KycDocument("1", DocumentType.PASSPORT, "P123", "path", null);
            when(kycDocumentRepository.findByUserIdAndStatus("1", KycStatus.APPROVED))
                    .thenReturn(List.of(passport));

            KycLevel level = kycService.getKycLevel(1L);

            assertThat(level).isEqualTo(KycLevel.BASIC);
        }

        @Test
        @DisplayName("should return STANDARD with ID + proof of address")
        void shouldReturnStandardWithIdAndProof() {
            KycDocument passport = new KycDocument("1", DocumentType.PASSPORT, "P123", "path", null);
            KycDocument bill = new KycDocument("1", DocumentType.UTILITY_BILL, "B456", "path", null);
            when(kycDocumentRepository.findByUserIdAndStatus("1", KycStatus.APPROVED))
                    .thenReturn(List.of(passport, bill));

            KycLevel level = kycService.getKycLevel(1L);

            assertThat(level).isEqualTo(KycLevel.STANDARD);
        }

        @Test
        @DisplayName("should return ENHANCED with 2+ IDs + proof of address")
        void shouldReturnEnhancedWithMultipleIdsAndProof() {
            KycDocument passport = new KycDocument("1", DocumentType.PASSPORT, "P123", "path", null);
            KycDocument license = new KycDocument("1", DocumentType.DRIVERS_LICENSE, "DL456", "path", null);
            KycDocument bill = new KycDocument("1", DocumentType.UTILITY_BILL, "B789", "path", null);
            when(kycDocumentRepository.findByUserIdAndStatus("1", KycStatus.APPROVED))
                    .thenReturn(List.of(passport, license, bill));

            KycLevel level = kycService.getKycLevel(1L);

            assertThat(level).isEqualTo(KycLevel.ENHANCED);
        }
    }

    @Nested
    @DisplayName("getUserKycStatus")
    class GetUserKycStatus {

        @Test
        @DisplayName("should return aggregated kyc status")
        void shouldReturnAggregatedKycStatus() {
            sampleDocument.setStatus(KycStatus.APPROVED);

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(kycDocumentRepository.findByUserId("user-123")).thenReturn(List.of(sampleDocument));

            KycStatusResponse response = kycService.getUserKycStatus("user-123", false);

            assertThat(response.getUserId()).isEqualTo("user-123");
            assertThat(response.getTotalDocuments()).isEqualTo(1);
            assertThat(response.getApprovedDocuments()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycService.getUserKycStatus("non-existent", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getPendingDocuments")
    class GetPendingDocuments {

        @Test
        @DisplayName("should return pending documents")
        void shouldReturnPendingDocuments() {
            when(kycDocumentRepository.findByStatus(KycStatus.PENDING)).thenReturn(List.of(sampleDocument));

            List<KycDocumentResponse> documents = kycService.getPendingDocuments();

            assertThat(documents).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no pending documents")
        void shouldReturnEmptyWhenNoPending() {
            when(kycDocumentRepository.findByStatus(KycStatus.PENDING)).thenReturn(List.of());

            List<KycDocumentResponse> documents = kycService.getPendingDocuments();

            assertThat(documents).isEmpty();
        }
    }
}
