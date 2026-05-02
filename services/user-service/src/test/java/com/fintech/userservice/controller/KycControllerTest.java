package com.fintech.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.security.service.AuthorizationService;
import com.fintech.userservice.dto.request.kyc.KycDocumentReviewRequest;
import com.fintech.userservice.dto.request.kyc.KycDocumentSubmitRequest;
import com.fintech.userservice.dto.response.kyc.KycDocumentResponse;
import com.fintech.userservice.dto.response.kyc.KycStatusResponse;
import com.fintech.userservice.entity.KycDocument;
import com.fintech.userservice.entity.enums.DocumentType;
import com.fintech.userservice.entity.enums.KycLevel;
import com.fintech.userservice.entity.enums.KycStatus;
import com.fintech.userservice.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KycController.class)
@AutoConfigureMockMvc(addFilters = false)
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KycService kycService;

    @MockBean
    private AuthorizationService authorizationService;

    private KycDocumentResponse sampleDocResponse;
    private KycStatusResponse sampleStatusResponse;

    @BeforeEach
    void setUp() {
        KycDocument doc = new KycDocument(
                "user-123", DocumentType.PASSPORT, "AB123456",
                "kyc/user-123/file_passport", LocalDate.of(2030, 12, 31)
        );
        sampleDocResponse = KycDocumentResponse.fromEntity(doc, true);

        sampleStatusResponse = new KycStatusResponse(
                "user-123", KycLevel.BASIC, KycStatus.APPROVED,
                LocalDateTime.now(), 1, 1, 0, 0,
                List.of(sampleDocResponse)
        );
    }

    @Nested
    @DisplayName("POST /api/users/{userId}/kyc/documents")
    class SubmitDocument {

        @Test
        @DisplayName("should submit document successfully")
        void shouldSubmitDocumentSuccessfully() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.submitDocument(eq("user-123"), any(KycDocumentSubmitRequest.class)))
                    .thenReturn(sampleDocResponse);

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            mockMvc.perform(post("/api/users/user-123/kyc/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.documentType").value("PASSPORT"));
        }

        @Test
        @DisplayName("should return 403 when submitting for another user as non-admin")
        void shouldReturn403ForAnotherUser() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("other-user");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            mockMvc.perform(post("/api/users/user-123/kyc/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access denied"));
        }

        @Test
        @DisplayName("should allow admin to submit for another user")
        void shouldAllowAdminToSubmitForAnotherUser() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(true);
            when(kycService.submitDocument(eq("user-123"), any(KycDocumentSubmitRequest.class)))
                    .thenReturn(sampleDocResponse);

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            mockMvc.perform(post("/api/users/user-123/kyc/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.submitDocument(eq("user-123"), any(KycDocumentSubmitRequest.class)))
                    .thenThrow(new IllegalArgumentException("User not found"));

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            mockMvc.perform(post("/api/users/user-123/kyc/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid request"));
        }

        @Test
        @DisplayName("should return 409 for duplicate document")
        void shouldReturn409ForDuplicate() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.submitDocument(eq("user-123"), any(KycDocumentSubmitRequest.class)))
                    .thenThrow(new IllegalStateException("Already pending"));

            KycDocumentSubmitRequest request = new KycDocumentSubmitRequest(
                    DocumentType.PASSPORT, "AB123456", LocalDate.of(2030, 12, 31)
            );

            mockMvc.perform(post("/api/users/user-123/kyc/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Document conflict"));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/kyc/status")
    class GetKycStatus {

        @Test
        @DisplayName("should return kyc status for own user")
        void shouldReturnKycStatusForOwnUser() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.getUserKycStatus("user-123", false)).thenReturn(sampleStatusResponse);

            mockMvc.perform(get("/api/users/user-123/kyc/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-123"))
                    .andExpect(jsonPath("$.kycLevel").value("BASIC"));
        }

        @Test
        @DisplayName("should return 403 for non-owner non-admin")
        void shouldReturn403ForNonOwnerNonAdmin() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("other-user");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);

            mockMvc.perform(get("/api/users/user-123/kyc/status"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access denied"));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.getUserKycStatus("user-123", false))
                    .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(get("/api/users/user-123/kyc/status"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not found"));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/kyc/documents")
    class GetUserDocuments {

        @Test
        @DisplayName("should return documents for own user")
        void shouldReturnDocumentsForOwnUser() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.getUserDocuments("user-123", false)).thenReturn(List.of(sampleDocResponse));

            mockMvc.perform(get("/api/users/user-123/kyc/documents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].documentType").value("PASSPORT"));
        }

        @Test
        @DisplayName("should return 403 for non-owner non-admin")
        void shouldReturn403ForNonOwnerNonAdmin() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("other-user");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);

            mockMvc.perform(get("/api/users/user-123/kyc/documents"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access denied"));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(authorizationService.hasAccessLevel("full", "user")).thenReturn(false);
            when(kycService.getUserDocuments("user-123", false))
                    .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(get("/api/users/user-123/kyc/documents"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/kyc/documents/{docId}/review")
    class ReviewDocument {

        @Test
        @DisplayName("should approve document successfully")
        void shouldApproveDocument() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(kycService.reviewDocument(eq(1L), eq("admin-1"), any(KycDocumentReviewRequest.class)))
                    .thenReturn(sampleDocResponse);

            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);

            mockMvc.perform(put("/api/admin/kyc/documents/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentType").value("PASSPORT"));
        }

        @Test
        @DisplayName("should return 400 for invalid review request")
        void shouldReturn400ForInvalidReview() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(kycService.reviewDocument(eq(1L), eq("admin-1"), any(KycDocumentReviewRequest.class)))
                    .thenThrow(new IllegalArgumentException("Rejection reason is required"));

            KycDocumentReviewRequest request = new KycDocumentReviewRequest(false, null);

            mockMvc.perform(put("/api/admin/kyc/documents/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid request"));
        }

        @Test
        @DisplayName("should return 409 for state conflict")
        void shouldReturn409ForStateConflict() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(kycService.reviewDocument(eq(1L), eq("admin-1"), any(KycDocumentReviewRequest.class)))
                    .thenThrow(new IllegalStateException("Cannot review own document"));

            KycDocumentReviewRequest request = new KycDocumentReviewRequest(true, null);

            mockMvc.perform(put("/api/admin/kyc/documents/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Review conflict"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/kyc/pending")
    class GetPendingDocuments {

        @Test
        @DisplayName("should return pending documents")
        void shouldReturnPendingDocuments() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(kycService.getPendingDocuments()).thenReturn(List.of(sampleDocResponse));

            mockMvc.perform(get("/api/admin/kyc/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPending").value(1))
                    .andExpect(jsonPath("$.documents[0].documentType").value("PASSPORT"));
        }

        @Test
        @DisplayName("should return empty list when no pending")
        void shouldReturnEmptyWhenNoPending() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(kycService.getPendingDocuments()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/kyc/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPending").value(0));
        }
    }
}
