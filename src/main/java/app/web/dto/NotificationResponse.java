package app.web.dto;

import app.model.NotificationStatus;
import app.model.NotificationType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
public class NotificationResponse {

    private String subject;

    private LocalDateTime createdOn;

    private NotificationStatus status;

    private NotificationType type;

}
