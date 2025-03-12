package app.web.mapper;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationType;
import app.web.dto.NotificationPreferenceResponse;
import app.web.dto.NotificationResponse;
import app.web.dto.NotificationTypeRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {


    // mapping logic: transferring one type of data into another type of data, in this case enum data
    public static NotificationType fromNotificationTypeRequest(NotificationTypeRequest notificationTypeRequest) {
        return switch (notificationTypeRequest) {
            case EMAIL -> NotificationType.EMAIL;
        };
    }


    // Build DTO from Entity
    public static NotificationPreferenceResponse fromNotificationPreference(NotificationPreference entity) {


        return NotificationPreferenceResponse.builder()
                .Id(entity.getId())
                .type(entity.getType())
                .contactInfo(entity.getContactInfo())
                .enabled(entity.isEnabled())
                .userId(entity.getUserId())
                .build();
    }


    public static NotificationResponse fromNotification(Notification entity) {

        return NotificationResponse.builder()
                .subject(entity.getSubject())
                .status(entity.getStatus())
                .createdOn(entity.getCreatedOn())
                .type(entity.getType())
                .build();
    }
}
