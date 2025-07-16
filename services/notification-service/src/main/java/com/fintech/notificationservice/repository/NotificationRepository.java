package com.fintech.notificationservice.repository;

import com.fintech.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipient(String recipient);
    
    List<Notification> findByStatus(Notification.NotificationStatus status);
    
    List<Notification> findByType(Notification.NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status")
    long countByStatus(@Param("status") Notification.NotificationStatus status);
}
