package com.fintech.notificationservice.repository;

import com.fintech.notificationservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(String userId);

    Optional<NotificationPreference> findByUserIdAndChannel(String userId, NotificationPreference.Channel channel);
}
