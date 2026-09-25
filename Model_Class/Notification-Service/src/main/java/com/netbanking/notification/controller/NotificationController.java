package com.netbanking.notification.controller;

import com.netbanking.notification.dto.NotificationCreateRequest;
import com.netbanking.notification.entity.Notification;
import com.netbanking.notification.service.EmailService;
import com.netbanking.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"})
@RequiredArgsConstructor
@Tag(name = "Notification Services", description = "Endpoints for creating and sending customer notifications via Google SMTP")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Notification> create(@Valid @RequestBody NotificationCreateRequest request) {
        Notification notification = notificationService.create(request);
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> send(@PathVariable Long id) {
        emailService.send(notificationService.findById(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Notification>> findAll() {
        return ResponseEntity.ok(notificationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> findById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.findById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationService.findByCustomer(customerId));
    }
}
