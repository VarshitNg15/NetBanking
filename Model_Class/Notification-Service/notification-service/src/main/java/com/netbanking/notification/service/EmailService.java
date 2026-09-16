package com.netbanking.notification.service;

import com.netbanking.notification.entity.DeliveryStatus;
import com.netbanking.notification.entity.Notification;
import com.netbanking.notification.entity.NotificationDelivery;
import com.netbanking.notification.entity.NotificationStatus;
import com.netbanking.notification.entity.SmtpProvider;
import com.netbanking.notification.repository.NotificationDeliveryRepository;
import com.netbanking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import com.netbanking.notification.mail.EmailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSender emailSender;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    @Transactional
    public void send(Notification notification) {
        int attemptNumber = deliveryRepository
                .findByNotificationNotificationIdOrderByAttemptNumberAsc(notification.getNotificationId())
                .size() + 1;

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(notification)
                .attemptNumber(attemptNumber)
                .smtpProvider(SmtpProvider.GOOGLE_SMTP)
                .attemptedAt(LocalDateTime.now())
                .build();

        try {
            emailSender.send(
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getMessageBody()
            );

            delivery.setDeliveryStatus(DeliveryStatus.SUCCESS);
            delivery.setDeliveredAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } catch (Exception ex) {
            delivery.setDeliveryStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(ex.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
        }

        deliveryRepository.save(delivery);
        notificationRepository.save(notification);
    }
}
