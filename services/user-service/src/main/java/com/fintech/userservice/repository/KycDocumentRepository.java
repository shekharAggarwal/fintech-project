package com.fintech.userservice.repository;

import com.fintech.userservice.entity.KycDocument;
import com.fintech.userservice.entity.enums.DocumentType;
import com.fintech.userservice.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByUserId(String userId);

    List<KycDocument> findByStatus(KycStatus status);

    List<KycDocument> findByUserIdAndStatus(String userId, KycStatus status);

    Optional<KycDocument> findByUserIdAndDocumentTypeAndStatusNot(String userId, DocumentType documentType, KycStatus status);

    boolean existsByUserIdAndDocumentTypeAndStatusIn(String userId, DocumentType documentType, List<KycStatus> statuses);
}
