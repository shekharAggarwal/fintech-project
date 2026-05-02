package com.fintech.schedulerservice.repository;

import com.fintech.schedulerservice.entity.RecurringPayment;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for RecurringPayment entity
 */
@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, String> {

    List<RecurringPayment> findByUserId(String userId);

    Page<RecurringPayment> findByUserId(String userId, Pageable pageable);

    List<RecurringPayment> findByStatus(RecurringPaymentStatus status);

    List<RecurringPayment> findByUserIdAndStatus(String userId, RecurringPaymentStatus status);

    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.status = 'ACTIVE' AND rp.nextExecutionDate <= :date")
    List<RecurringPayment> findPaymentsDueForExecution(@Param("date") LocalDate date);

    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.status = 'ACTIVE' AND rp.endDate IS NOT NULL AND rp.endDate < :date")
    List<RecurringPayment> findExpiredPayments(@Param("date") LocalDate date);

    long countByUserIdAndStatus(String userId, RecurringPaymentStatus status);
}
