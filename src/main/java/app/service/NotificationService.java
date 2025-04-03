package app.service;


import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
import app.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationRepository notificationRepository;
    private final MailSender mailSender;

    @Autowired
    public NotificationService(NotificationPreferenceRepository notificationPreferenceRepository, NotificationRepository notificationRepository, MailSender mailSender) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {

        // if exist - return NotificationPreference
        // if it does not exist - then throws exception
        NotificationPreference notificationPreference = getPreferenceByUserId(userId);
        notificationPreference.setEnabled(enabled);
        return notificationPreferenceRepository.save(notificationPreference);
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return notificationPreferenceRepository.findByUserId(userId).orElseThrow(() -> new NullPointerException(String.format("Notification preference for user id %s was not found.", userId)));
    }

    public List<Notification> getNotificationHistory(UUID userId) {
        return notificationRepository.findAllByUserIdAndDeletedIsFalse(userId);
    }

    public void clearNotifications(UUID userId) {
        List<Notification> notifications = getNotificationHistory(userId);

        notifications.forEach(notification -> {
            notification.setDeleted(true);
            notificationRepository.save(notification);
        });

    }

    public void retryFailedNotifications(UUID userId) {

        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException(String.format("User with id %s does not allow to receive notifications.", userId));
        }

        List<Notification> failedNotifications = notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED);
        failedNotifications = failedNotifications.stream().filter(notification -> !notification.isDeleted()).toList();


        for (Notification notification : failedNotifications) {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userPreference.getContactInfo());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());

            try {
                mailSender.send(message);
                notification.setStatus(NotificationStatus.SUCCEEDED);
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                String contactInfo = userPreference.getContactInfo();
                if (userPreference.getContactInfo() == null) {
                    log.warn("There was an issue sending an email due to missing contact info: {}", e.getMessage());
                } else {
                    log.warn("There was an issue sending an email to {} due to {}.", userPreference.getContactInfo(), e.getMessage());
                }
            }
            notificationRepository.save(notification);
        }
    }

    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {

        // upsert

        // 1. try to find if such exist in the database.
        Optional<NotificationPreference> userNotificationOptional = notificationPreferenceRepository.findByUserId(dto.getUserId());

        // 2. if exists - just update it.
        if (userNotificationOptional.isPresent()) {
            NotificationPreference preference = userNotificationOptional.get();
            preference.setContactInfo(dto.getContactInfo());
            preference.setEnabled(dto.isNotificationEnabled());
            preference.setType(DtoMapper.fromNotificationTypeRequest(dto.getType()));
            preference.setUpdatedOn(LocalDateTime.now());
            return notificationPreferenceRepository.save(preference);
        }

        // 3. if it does not exist - just create a new one.
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .userId(dto.getUserId())
                .type(DtoMapper.fromNotificationTypeRequest(dto.getType()))
                .enabled(dto.isNotificationEnabled())
                .contactInfo(dto.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();


        return notificationPreferenceRepository.save(notificationPreference);
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {

        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException(String.format("User with id %s does not allow to receive notifications.", userId));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userPreference.getContactInfo());
        message.setSubject(notificationRequest.getSubject());
        message.setText(notificationRequest.getBody());

        // We record new notification in the database to show that we have sent notification to that user
        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .isDeleted(false)
                .type(NotificationType.EMAIL)
                .build();

        try {
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            String contactInfo = userPreference.getContactInfo();
            log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

        return notificationRepository.save(notification);
    }
}
