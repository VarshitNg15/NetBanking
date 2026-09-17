package com.netbanking.notification.service;

import com.netbanking.notification.dto.NotificationCreateRequest;
import com.netbanking.notification.entity.Notification;
import com.netbanking.notification.entity.NotificationStatus;
import com.netbanking.notification.exception.DuplicateEventException;
import com.netbanking.notification.exception.ResourceNotFoundException;
import com.netbanking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(NotificationCreateRequest request) {
        if (notificationRepository.findByEventId(request.eventId()).isPresent()) {
            throw new DuplicateEventException("Notification event already processed: " + request.eventId());
        }

        Notification notification = Notification.builder()
                .eventId(request.eventId())
                .eventType(request.eventType())
                .customerId(request.customerId())
                .recipientEmail(request.recipientEmail())
                .subject(request.subject())
                .messageBody(request.messageBody())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Notification> findByCustomer(String customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
