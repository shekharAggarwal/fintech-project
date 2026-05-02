package com.fintech.userservice.dto.request.kyc;

import com.fintech.userservice.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class KycDocumentSubmitRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(min = 3, max = 100, message = "Document number must be between 3 and 100 characters")
    private String documentNumber;

    private LocalDate expiryDate;

    public KycDocumentSubmitRequest() {
    }

    public KycDocumentSubmitRequest(DocumentType documentType, String documentNumber,
                                    LocalDate expiryDate) {
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.expiryDate = expiryDate;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
