package com.netbanking.notification.repository;

import com.netbanking.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    List<NotificationDelivery> findByNotificationNotificationIdOrderByAttemptNumberAsc(Long notificationId);
}
