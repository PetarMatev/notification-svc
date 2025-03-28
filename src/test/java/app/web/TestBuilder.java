package app.web;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.web.dto.NotificationPreferenceResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static NotificationPreference aRandomNotificationPreference() {

        return NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .enabled(true)
                .contactInfo("text")
                .type(NotificationType.EMAIL)
                .updatedOn(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static Notification aRandomNotification() {

        return Notification.builder()
                .id(UUID.randomUUID())
                .subject("subject of Notification")
                .body("body of Notification")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .build();
    }

    public static List<Notification> aRandomListOfNotificationResponses() {

        Notification one = Notification.builder()
                .id(UUID.randomUUID())
                .subject("subject of Notification")
                .body("body of Notification")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .build();

        Notification two = Notification.builder()
                .id(UUID.randomUUID())
                .subject("subject of Notification")
                .body("body of Notification")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .build();

        return List.of(one, two);
    }


    public static NotificationPreferenceResponse aRandomNotificationPreferenceResponse() {

        return NotificationPreferenceResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .enabled(true)
                .type(NotificationType.EMAIL)
                .contactInfo("email@gmail.com")
                .build();
    }
}
