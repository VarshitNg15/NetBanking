package com.netbanking.account.service;

import com.netbanking.account.entity.*;
import com.netbanking.account.exception.*;
import com.netbanking.account.repository.AccountPinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PinService {
    private static final Logger log = LoggerFactory.getLogger(PinService.class);

    private final AccountService accounts;
    private final AccountPinRepository pins;
    private final PasswordEncoder encoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${clients.notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;

    public PinService(AccountService a, AccountPinRepository p, PasswordEncoder e) {
        accounts = a;
        pins = p;
        encoder = e;
    }

    public boolean isPinSet(Long id) {
        return pins.existsById(id);
    }

    @Transactional
    public void set(Long id, String rawPin) {
        set(id, rawPin, null);
    }

    @Transactional
    public void set(Long id, String rawPin, String customerEmail) {
        Account a = accounts.account(id);
        boolean isUpdate = pins.existsById(id);

        pins.save(pins.findById(id).map(p -> {
            if (!encoder.matches(rawPin, p.getPinHash())) {
                return new AccountPin(a, encoder.encode(rawPin));
            }
            return p;
        }).orElseGet(() -> new AccountPin(a, encoder.encode(rawPin))));

        sendPinNotification(a, isUpdate, customerEmail);
    }

    @Transactional
    public boolean verify(Long id, String rawPin) {
        AccountPin p = pins.findById(id).orElseThrow(() -> new NotFoundException("PIN has not been set"));
        if (p.getLockedUntil() != null && p.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ConflictException("PIN is temporarily locked");
        }
        if (encoder.matches(rawPin, p.getPinHash())) {
            p.reset();
            return true;
        }
        p.failed();
        return false;
    }

    private void sendPinNotification(Account account, boolean isUpdate, String customerEmail) {
        try {
            String eventType = isUpdate ? "PIN_UPDATED" : "PIN_SET";
            String subject = isUpdate ? "Security Alert: NetBanking PIN Updated" : "Security Alert: NetBanking PIN Set";
            String body = "Dear Customer,\n\nYour 4-digit security PIN for Account " + account.getAccountNumber() +
                    (isUpdate ? " has been successfully updated." : " has been successfully configured.") +
                    "\nIf you did not initiate this security change, please contact customer support immediately.\n\nWarm regards,\nNetBanking Security Operations";

            String recipient = (customerEmail != null && !customerEmail.isBlank())
                    ? customerEmail
                    : (account.getCustomerId() + "@netbank.com");

            Map<String, Object> req = Map.of(
                    "eventId", "PIN-" + UUID.randomUUID(),
                    "eventType", eventType,
                    "customerId", account.getCustomerId(),
                    "recipientEmail", recipient,
                    "subject", subject,
                    "messageBody", body
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            restTemplate.postForLocation(notificationServiceUrl + "/api/v1/notifications", entity);
            log.info("Dispatched {} notification for account {} to {}", eventType, account.getAccountNumber(), recipient);
        } catch (Exception ex) {
            log.warn("Could not dispatch PIN notification to Notification-Service: {}", ex.getMessage());
        }
    }
}
