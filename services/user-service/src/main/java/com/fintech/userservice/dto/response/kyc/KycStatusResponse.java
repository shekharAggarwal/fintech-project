package com.fintech.userservice.dto.response.kyc;

import com.fintech.userservice.entity.enums.KycLevel;
import com.fintech.userservice.entity.enums.KycStatus;

import java.time.LocalDateTime;
import java.util.List;

public class KycStatusResponse {

    private String userId;
    private KycLevel kycLevel;
    private KycStatus kycStatus;
    private LocalDateTime kycVerifiedAt;
    private int totalDocuments;
    private int approvedDocuments;
    private int pendingDocuments;
    private int rejectedDocuments;
    private List<KycDocumentResponse> documents;

    public KycStatusResponse() {
    }

    public KycStatusResponse(String userId, KycLevel kycLevel, KycStatus kycStatus,
                             LocalDateTime kycVerifiedAt, int totalDocuments,
                             int approvedDocuments, int pendingDocuments,
                             int rejectedDocuments, List<KycDocumentResponse> documents) {
        this.userId = userId;
        this.kycLevel = kycLevel;
        this.kycStatus = kycStatus;
        this.kycVerifiedAt = kycVerifiedAt;
        this.totalDocuments = totalDocuments;
        this.approvedDocuments = approvedDocuments;
        this.pendingDocuments = pendingDocuments;
        this.rejectedDocuments = rejectedDocuments;
        this.documents = documents;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public KycLevel getKycLevel() {
        return kycLevel;
    }

    public void setKycLevel(KycLevel kycLevel) {
        this.kycLevel = kycLevel;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public LocalDateTime getKycVerifiedAt() {
        return kycVerifiedAt;
    }

    public void setKycVerifiedAt(LocalDateTime kycVerifiedAt) {
        this.kycVerifiedAt = kycVerifiedAt;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public int getApprovedDocuments() {
        return approvedDocuments;
    }

    public void setApprovedDocuments(int approvedDocuments) {
        this.approvedDocuments = approvedDocuments;
    }

    public int getPendingDocuments() {
        return pendingDocuments;
    }

    public void setPendingDocuments(int pendingDocuments) {
        this.pendingDocuments = pendingDocuments;
    }

    public int getRejectedDocuments() {
        return rejectedDocuments;
    }

    public void setRejectedDocuments(int rejectedDocuments) {
        this.rejectedDocuments = rejectedDocuments;
    }

    public List<KycDocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KycDocumentResponse> documents) {
        this.documents = documents;
    }
}
