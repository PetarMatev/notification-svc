package app.web.dto;

import app.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationPreferenceResponse {

    private UUID Id;

    private UUID userId;

    private boolean enabled;

    private NotificationType type;

    private String contactInfo;
}
