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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KycService {

    private static final Logger logger = LoggerFactory.getLogger(KycService.class);

    private final KycDocumentRepository kycDocumentRepository;
    private final UserProfileRepository userProfileRepository;
    private final KycEventKafkaPublisher kycEventKafkaPublisher;

    public KycService(KycDocumentRepository kycDocumentRepository,
                      UserProfileRepository userProfileRepository,
                      KycEventKafkaPublisher kycEventKafkaPublisher) {
        this.kycDocumentRepository = kycDocumentRepository;
        this.userProfileRepository = userProfileRepository;
        this.kycEventKafkaPublisher = kycEventKafkaPublisher;
    }

    /**
     * Submit a KYC document for verification.
     * Prevents duplicate active submissions of the same document type.
     */
    @Transactional
    public KycDocumentResponse submitDocument(String userId, KycDocumentSubmitRequest request) {
        logger.info("Submitting KYC document for userId: {}, type: {}", userId, request.getDocumentType());

        // Verify user exists
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Prevent duplicate active document submissions (PENDING or UNDER_REVIEW)
        List<KycStatus> activeStatuses = List.of(KycStatus.PENDING, KycStatus.UNDER_REVIEW);
        boolean hasDuplicate = kycDocumentRepository.existsByUserIdAndDocumentTypeAndStatusIn(
                userId, request.getDocumentType(), activeStatuses);

        if (hasDuplicate) {
            throw new IllegalStateException(
                    "A document of type " + request.getDocumentType() + " is already pending or under review");
        }

        // Server-generated file path — never trust client-provided paths
        String filePath = "kyc/" + userId + "/" + UUID.randomUUID() + "_" + request.getDocumentType().name().toLowerCase();

        KycDocument document = new KycDocument(
                userId,
                request.getDocumentType(),
                request.getDocumentNumber(),
                filePath,
                request.getExpiryDate()
        );

        KycDocument savedDocument;
        try {
            savedDocument = kycDocumentRepository.save(document);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "A document of type " + request.getDocumentType() + " already exists with the same status for this user");
        }

        // Update user KYC status to PENDING if currently NONE level
        if (userProfile.getKycLevel() == KycLevel.NONE && userProfile.getKycStatus() != KycStatus.PENDING) {
            userProfile.setKycStatus(KycStatus.PENDING);
            userProfileRepository.save(userProfile);
        }

        // Publish event
        kycEventKafkaPublisher.publishKycSubmitted(userId, savedDocument.getId(),
                request.getDocumentType().name());

        logger.info("KYC document submitted successfully. docId: {}, userId: {}", savedDocument.getId(), userId);
        return KycDocumentResponse.fromEntity(savedDocument, false);
    }

    /**
     * Review a KYC document (admin only).
     * Prevents self-review and validates state transitions.
     */
    @Transactional
    public KycDocumentResponse reviewDocument(Long documentId, String reviewerId, KycDocumentReviewRequest request) {
        logger.info("Reviewing KYC document. docId: {}, reviewerId: {}, approved: {}",
                documentId, reviewerId, request.getApproved());

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Self-review prevention
        if (document.getUserId().equals(reviewerId)) {
            throw new IllegalStateException("Cannot review your own KYC document");
        }

        // State machine validation — only PENDING or UNDER_REVIEW documents can be reviewed
        if (document.getStatus() != KycStatus.PENDING && document.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Document cannot be reviewed in its current state: " + document.getStatus());
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            document.setStatus(KycStatus.APPROVED);
            document.setRejectionReason(null);
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a document");
            }
            document.setStatus(KycStatus.REJECTED);
            document.setRejectionReason(request.getRejectionReason());
        }

        document.setVerifiedBy(reviewerId);
        document.setVerifiedAt(LocalDateTime.now());

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Update user KYC status based on all documents
        updateUserKycStatus(document.getUserId());

        // Publish event
        if (Boolean.TRUE.equals(request.getApproved())) {
            kycEventKafkaPublisher.publishKycApproved(document.getUserId(), documentId, reviewerId);
        } else {
            kycEventKafkaPublisher.publishKycRejected(document.getUserId(), documentId,
                    request.getRejectionReason(), reviewerId);
        }

        logger.info("KYC document reviewed. docId: {}, status: {}", documentId, savedDocument.getStatus());
        return KycDocumentResponse.fromEntity(savedDocument, true);
    }

    /**
     * Get aggregated KYC status for a user.
     */
    public KycStatusResponse getUserKycStatus(String userId, boolean isAdmin) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);
        List<KycDocumentResponse> documentResponses = documents.stream()
                .map(doc -> KycDocumentResponse.fromEntity(doc, isAdmin))
                .toList();

        int approved = (int) documents.stream().filter(d -> d.getStatus() == KycStatus.APPROVED).count();
        int pending = (int) documents.stream()
                .filter(d -> d.getStatus() == KycStatus.PENDING || d.getStatus() == KycStatus.UNDER_REVIEW)
                .count();
        int rejected = (int) documents.stream().filter(d -> d.getStatus() == KycStatus.REJECTED).count();

        return new KycStatusResponse(
                userId,
                userProfile.getKycLevel(),
                userProfile.getKycStatus(),
                userProfile.getKycVerifiedAt(),
                documents.size(),
                approved,
                pending,
                rejected,
                documentResponses
        );
    }

    /**
     * Get user's KYC documents.
     */
    public List<KycDocumentResponse> getUserDocuments(String userId, boolean isAdmin) {
        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);
        return documents.stream()
                .map(doc -> KycDocumentResponse.fromEntity(doc, isAdmin))
                .toList();
    }

    /**
     * Get all pending KYC documents (admin).
     */
    public List<KycDocumentResponse> getPendingDocuments() {
        List<KycDocument> documents = kycDocumentRepository.findByStatus(KycStatus.PENDING);
        return documents.stream()
                .map(doc -> KycDocumentResponse.fromEntity(doc, true))
                .toList();
    }

    /**
     * Get the KYC level for a user based on their approved documents.
     * LEVEL mapping:
     *   NONE = no approved documents
     *   BASIC = at least 1 approved ID document (PASSPORT, DRIVERS_LICENSE, or ID_CARD)
     *   STANDARD = 1 ID document + 1 proof of address (UTILITY_BILL or BANK_STATEMENT)
     *   ENHANCED = 2+ ID documents + 1 proof of address (full verification)
     */
    public KycLevel getKycLevel(Long userId) {
        String userIdStr = userId.toString();
        List<KycDocument> approvedDocs = kycDocumentRepository.findByUserIdAndStatus(userIdStr, KycStatus.APPROVED);

        long idDocuments = approvedDocs.stream()
                .filter(d -> d.getDocumentType() == DocumentType.PASSPORT
                        || d.getDocumentType() == DocumentType.DRIVERS_LICENSE
                        || d.getDocumentType() == DocumentType.ID_CARD)
                .count();

        long proofOfAddress = approvedDocs.stream()
                .filter(d -> d.getDocumentType() == DocumentType.UTILITY_BILL
                        || d.getDocumentType() == DocumentType.BANK_STATEMENT)
                .count();

        if (idDocuments >= 2 && proofOfAddress >= 1) {
            return KycLevel.ENHANCED;
        } else if (idDocuments >= 1 && proofOfAddress >= 1) {
            return KycLevel.STANDARD;
        } else if (idDocuments >= 1) {
            return KycLevel.BASIC;
        } else {
            return KycLevel.NONE;
        }
    }

    /**
     * Check if a user's KYC is approved (has at least BASIC/LEVEL_1 KYC).
     * Returns true if user has at least one approved ID document.
     */
    public boolean isKycApproved(Long userId) {
        KycLevel level = getKycLevel(userId);
        return level != KycLevel.NONE;
    }

    /**
     * Upgrade user's KYC level based on approved documents.
     * BASIC = 1 approved ID document
     * STANDARD = 1 ID document + 1 proof of address
     * ENHANCED = 2+ ID documents + 1 proof of address
     */
    @Transactional
    public KycLevel upgradeKycLevel(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<KycDocument> approvedDocs = kycDocumentRepository.findByUserIdAndStatus(userId, KycStatus.APPROVED);

        long idDocuments = approvedDocs.stream()
                .filter(d -> d.getDocumentType() == DocumentType.PASSPORT
                        || d.getDocumentType() == DocumentType.DRIVERS_LICENSE
                        || d.getDocumentType() == DocumentType.ID_CARD)
                .count();

        long proofOfAddress = approvedDocs.stream()
                .filter(d -> d.getDocumentType() == DocumentType.UTILITY_BILL
                        || d.getDocumentType() == DocumentType.BANK_STATEMENT)
                .count();

        KycLevel newLevel;
        if (idDocuments >= 2 && proofOfAddress >= 1) {
            newLevel = KycLevel.ENHANCED;
        } else if (idDocuments >= 1 && proofOfAddress >= 1) {
            newLevel = KycLevel.STANDARD;
        } else if (idDocuments >= 1) {
            newLevel = KycLevel.BASIC;
        } else {
            newLevel = KycLevel.NONE;
        }

        userProfile.setKycLevel(newLevel);
        if (newLevel != KycLevel.NONE) {
            userProfile.setKycStatus(KycStatus.APPROVED);
            userProfile.setKycVerifiedAt(LocalDateTime.now());
        }
        userProfileRepository.save(userProfile);

        logger.info("KYC level upgraded for userId: {} to level: {}", userId, newLevel);
        return newLevel;
    }

    /**
     * Updates user's KYC status after a document review.
     */
    private void updateUserKycStatus(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);

        boolean hasApproved = documents.stream().anyMatch(d -> d.getStatus() == KycStatus.APPROVED);
        boolean allRejected = documents.stream().allMatch(d -> d.getStatus() == KycStatus.REJECTED);
        boolean hasPending = documents.stream()
                .anyMatch(d -> d.getStatus() == KycStatus.PENDING || d.getStatus() == KycStatus.UNDER_REVIEW);

        if (hasApproved) {
            // Automatically upgrade level when documents are approved
            upgradeKycLevel(userId);
        } else if (allRejected && !documents.isEmpty()) {
            userProfile.setKycStatus(KycStatus.REJECTED);
            userProfileRepository.save(userProfile);
        } else if (hasPending) {
            userProfile.setKycStatus(KycStatus.PENDING);
            userProfileRepository.save(userProfile);
        }
    }
}
