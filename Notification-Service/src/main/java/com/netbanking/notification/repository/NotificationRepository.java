package com.netbanking.notification.repository;

import com.netbanking.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventId(String eventId);
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
