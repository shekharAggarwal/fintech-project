package com.fintech.userservice.dto.response.kyc;

import com.fintech.userservice.entity.KycDocument;
import com.fintech.userservice.entity.enums.DocumentType;
import com.fintech.userservice.entity.enums.KycStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class KycDocumentResponse {

    private Long id;
    private String userId;
    private DocumentType documentType;
    private String documentNumber;
    private KycStatus status;
    private String rejectionReason;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDate expiryDate;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KycDocumentResponse() {
    }

    /**
     * Creates a response with masked document number for non-admin users.
     * Only the last 4 characters are visible; the rest are replaced with asterisks.
     */
    public static KycDocumentResponse fromEntity(KycDocument document, boolean isAdmin) {
        KycDocumentResponse response = new KycDocumentResponse();
        response.id = document.getId();
        response.userId = document.getUserId();
        response.documentType = document.getDocumentType();
        response.documentNumber = isAdmin ? document.getDocumentNumber() : maskDocumentNumber(document.getDocumentNumber());
        response.status = document.getStatus();
        response.rejectionReason = document.getRejectionReason();
        response.verifiedBy = document.getVerifiedBy();
        response.verifiedAt = document.getVerifiedAt();
        response.expiryDate = document.getExpiryDate();
        response.uploadedAt = document.getUploadedAt();
        response.createdAt = document.getCreatedAt();
        response.updatedAt = document.getUpdatedAt();
        return response;
    }

    private static String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.length() <= 4) {
            return "****";
        }
        int visibleLength = 4;
        String masked = "*".repeat(documentNumber.length() - visibleLength);
        return masked + documentNumber.substring(documentNumber.length() - visibleLength);
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public KycStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
