package com.fintech.userservice.controller;

import com.fintech.security.annotation.RequireAuthorization;
import com.fintech.security.service.AuthorizationService;
import com.fintech.userservice.dto.request.kyc.KycDocumentReviewRequest;
import com.fintech.userservice.dto.request.kyc.KycDocumentSubmitRequest;
import com.fintech.userservice.dto.response.kyc.KycDocumentResponse;
import com.fintech.userservice.dto.response.kyc.KycStatusResponse;
import com.fintech.userservice.entity.enums.KycLevel;
import com.fintech.userservice.service.KycService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class KycController {

    private static final Logger logger = LoggerFactory.getLogger(KycController.class);

    private final KycService kycService;
    private final AuthorizationService authorizationService;

    public KycController(KycService kycService, AuthorizationService authorizationService) {
        this.kycService = kycService;
        this.authorizationService = authorizationService;
    }

    /**
     * Submit a KYC document for verification.
     */
    @PostMapping("/users/{userId}/kyc/documents")
    @RequireAuthorization(message = "Access denied: Authentication required", resourceType = "user")
    public ResponseEntity<?> submitDocument(@PathVariable String userId,
                                            @Valid @RequestBody KycDocumentSubmitRequest request) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("KYC document submission by user: {} for userId: {}", currentUserId, userId);

        // Users can only submit documents for themselves unless admin
        if (!userId.equals(currentUserId) && !isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied", "reason", "Cannot submit documents for another user"));
        }

        try {
            KycDocumentResponse response = kycService.submitDocument(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "reason", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Document conflict", "reason", e.getMessage()));
        }
    }

    /**
     * Get KYC status for a user.
     */
    @GetMapping("/users/{userId}/kyc/status")
    @RequireAuthorization(message = "Access denied: Authentication required", resourceType = "user")
    public ResponseEntity<?> getKycStatus(@PathVariable String userId) {
        String currentUserId = authorizationService.getCurrentUserId();

        // Users can view their own status; admins can view anyone's
        if (!userId.equals(currentUserId) && !isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied", "reason", "Cannot view KYC status of another user"));
        }

        try {
            boolean isAdmin = isAdmin(currentUserId);
            KycStatusResponse response = kycService.getUserKycStatus(userId, isAdmin);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "reason", e.getMessage()));
        }
    }

    /**
     * Get KYC documents for a user.
     */
    @GetMapping("/users/{userId}/kyc/documents")
    @RequireAuthorization(message = "Access denied: Authentication required", resourceType = "user")
    public ResponseEntity<?> getUserDocuments(@PathVariable String userId) {
        String currentUserId = authorizationService.getCurrentUserId();

        if (!userId.equals(currentUserId) && !isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied", "reason", "Cannot view documents of another user"));
        }

        try {
            boolean isAdmin = isAdmin(currentUserId);
            List<KycDocumentResponse> documents = kycService.getUserDocuments(userId, isAdmin);
            return ResponseEntity.ok(documents);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "reason", e.getMessage()));
        }
    }

    /**
     * Review a KYC document (admin only).
     */
    @PutMapping("/admin/kyc/documents/{docId}/review")
    @RequireAuthorization(
            expression = "hasFullAccess()",
            message = "Access denied: Admin privileges required",
            resourceType = "user"
    )
    public ResponseEntity<?> reviewDocument(@PathVariable Long docId,
                                            @Valid @RequestBody KycDocumentReviewRequest request) {
        String reviewerId = authorizationService.getCurrentUserId();
        logger.info("Admin {} reviewing document {}", reviewerId, docId);

        try {
            KycDocumentResponse response = kycService.reviewDocument(docId, reviewerId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "reason", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Review conflict", "reason", e.getMessage()));
        }
    }

    /**
     * Get all pending KYC documents (admin only).
     */
    @GetMapping("/admin/kyc/pending")
    @RequireAuthorization(
            expression = "hasFullAccess()",
            message = "Access denied: Admin privileges required",
            resourceType = "user"
    )
    public ResponseEntity<?> getPendingDocuments() {
        logger.info("Admin {} fetching pending KYC documents", authorizationService.getCurrentUserId());
        List<KycDocumentResponse> documents = kycService.getPendingDocuments();
        return ResponseEntity.ok(Map.of(
                "totalPending", documents.size(),
                "documents", documents
        ));
    }

    private boolean isAdmin(String userId) {
        try {
            return authorizationService.hasFullAccess();
        } catch (Exception e) {
            logger.warn("Error checking admin status for user: {}", userId, e);
            return false;
        }
    }
}
