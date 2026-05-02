package com.fintech.userservice.repository;

import com.fintech.userservice.entity.KycDocument;
import com.fintech.userservice.entity.enums.DocumentStatus;
import com.fintech.userservice.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByUserId(String userId);

    List<KycDocument> findByStatus(DocumentStatus status);

    List<KycDocument> findByUserIdAndStatus(String userId, DocumentStatus status);

    Optional<KycDocument> findByUserIdAndDocumentTypeAndStatusNot(String userId, DocumentType documentType, DocumentStatus status);

    boolean existsByUserIdAndDocumentTypeAndStatusIn(String userId, DocumentType documentType, List<DocumentStatus> statuses);
}
