package app.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// DTO = contact

@Data
@Builder
@Getter
@Setter
public class UpsertNotificationPreference {

    @NotNull
    private UUID userId;

    private boolean notificationEnabled;

    // the web piece is called the presentation layer.
    // we are restricting at the moment the user to be able to select only NotificationTypeRequests.

    @NotNull
    private NotificationTypeRequest type;

    private String contactInfo;

}
