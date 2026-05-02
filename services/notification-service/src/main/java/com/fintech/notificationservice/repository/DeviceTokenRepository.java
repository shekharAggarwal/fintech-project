package com.fintech.notificationservice.repository;

import com.fintech.notificationservice.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserIdAndEnabledTrue(String userId);

    List<DeviceToken> findByUserId(String userId);

    Optional<DeviceToken> findByUserIdAndDeviceToken(String userId, String deviceToken);

    void deleteByUserIdAndDeviceToken(String userId, String deviceToken);
}
