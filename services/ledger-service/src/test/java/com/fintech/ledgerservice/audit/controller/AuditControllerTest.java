package com.fintech.ledgerservice.audit.controller;

import com.fintech.ledgerservice.audit.dto.AuditSearchCriteria;
import com.fintech.ledgerservice.audit.entity.*;
import com.fintech.ledgerservice.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditService auditService;

    private AuditEvent createAuditEvent(AuditEventType eventType, String actorId) {
        return new AuditEvent(eventType, actorId, ActorType.USER,
                ResourceType.TRANSACTION, "RES-001", AuditAction.CREATE,
                "Test audit event", "127.0.0.1");
    }

    @Test
    @DisplayName("GET /api/audit/trail returns paginated audit events")
    void getAuditTrail_returnsPaginatedEvents() throws Exception {
        AuditEvent event = createAuditEvent(AuditEventType.TRANSACTION_COMPLETED, "user-1");
        Page<AuditEvent> page = new PageImpl<>(List.of(event));

        when(auditService.getAuditTrail(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit/trail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].actorId", is("user-1")))
                .andExpect(jsonPath("$.content[0].eventType", is("TRANSACTION_COMPLETED")));
    }

    @Test
    @DisplayName("GET /api/audit/trail with pagination parameters")
    void getAuditTrail_withPagination() throws Exception {
        Page<AuditEvent> emptyPage = new PageImpl<>(List.of());

        when(auditService.getAuditTrail(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/audit/trail")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/audit/type/{eventType} returns events by type")
    void getAuditByType_returnsFilteredEvents() throws Exception {
        AuditEvent event = createAuditEvent(AuditEventType.LEDGER_ENTRY_CREATED, "system");
        Page<AuditEvent> page = new PageImpl<>(List.of(event));

        when(auditService.getAuditByType(eq(AuditEventType.LEDGER_ENTRY_CREATED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/audit/type/{eventType}", "LEDGER_ENTRY_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventType", is("LEDGER_ENTRY_CREATED")));
    }

    @Test
    @DisplayName("GET /api/audit/actor/{actorId} returns events by actor")
    void getAuditByActor_returnsFilteredEvents() throws Exception {
        String actorId = "user-42";
        AuditEvent event = createAuditEvent(AuditEventType.TRANSACTION_INITIATED, actorId);
        Page<AuditEvent> page = new PageImpl<>(List.of(event));

        when(auditService.getAuditByActor(eq(actorId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit/actor/{actorId}", actorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].actorId", is(actorId)));
    }

    @Test
    @DisplayName("GET /api/audit/range returns events within date range")
    void getAuditByDateRange_returnsFilteredEvents() throws Exception {
        AuditEvent event = createAuditEvent(AuditEventType.PAYMENT_RECEIVED, "user-1");
        Page<AuditEvent> page = new PageImpl<>(List.of(event));

        when(auditService.getAuditByDateRange(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/audit/range")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/audit/search with criteria returns matching events")
    void searchAudit_returnsCriteriaMatches() throws Exception {
        AuditEvent event = createAuditEvent(AuditEventType.LEDGER_RECONCILIATION, "admin-1");
        Page<AuditEvent> page = new PageImpl<>(List.of(event));

        when(auditService.searchAudit(any(AuditSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        AuditSearchCriteria criteria = new AuditSearchCriteria();
        criteria.setActorId("admin-1");
        criteria.setEventType(AuditEventType.LEDGER_RECONCILIATION);

        mockMvc.perform(post("/api/audit/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].actorId", is("admin-1")));
    }
}
