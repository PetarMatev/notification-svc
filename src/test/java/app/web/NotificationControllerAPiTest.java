package app.web;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.service.NotificationService;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationTypeRequest;
import app.web.dto.UpsertNotificationPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static app.web.TestBuilder.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationControllerAPiTest {

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    // 01. upsertNotificationPreference
    @Test
    void postWithBodyToCreatePreference_returns201AndCorrectDtoStructure() throws Exception {

        // 01. Build Request
        UpsertNotificationPreference requestDto = UpsertNotificationPreference.builder()
                .userId(UUID.randomUUID())
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("text")
                .notificationEnabled(true)
                .build();

        when(notificationService.upsertPreference(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request = post("/api/v1/notifications/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(requestDto));


        // 02. Send Request
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    // 02. getUserNotificationPreference
    @Test
    void getRequestNotificationPreference_happyPath() throws Exception {

        // 01. Build Request
        when(notificationService.getPreferenceByUserId(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request = get("/api/v1/notifications/preferences").param("userId", UUID.randomUUID().toString());

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    // 03. sendNotification
    @Test
    void postWithBodyToCreateNotification_returns201AndCorrectDtoStructure() throws Exception {

        // 01. Build Request
        NotificationRequest requestDto = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .subject("Payment")
                .body("body of email")
                .build();

        when(notificationService.sendNotification(any())).thenReturn(aRandomNotification());
        MockHttpServletRequestBuilder request = post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(requestDto));

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("subject").isNotEmpty())
                .andExpect(jsonPath("createdOn").isNotEmpty())
                .andExpect(jsonPath("status").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty());
    }

    // 04. getNotificationHistory
    @Test
    void getNotificationHistory_happyPath() throws Exception {

        // 01. Build Request
        when(notificationService.getNotificationHistory(any())).thenReturn(aRandomListOfNotificationResponses());
        MockHttpServletRequestBuilder request = get("/api/v1/notifications").param("userId", UUID.randomUUID().toString());

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").isNotEmpty())
                .andExpect(jsonPath("$[0].createdOn").isNotEmpty())
                .andExpect(jsonPath("$[0].status").isNotEmpty())
                .andExpect(jsonPath("$[0].type").isNotEmpty());
    }

    // 05. changeNotificationPreference
    @Test
    void changeNotificationPreference_shouldReturn200AndUpdatedPreference() throws Exception {

        // 01. Build Request
        UUID userId = UUID.randomUUID();
        boolean enabled = true;

        NotificationPreference mockPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(enabled)
                .contactInfo("user@example.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationService.changeNotificationPreference(userId, enabled))
                .thenReturn(mockPreference);

        // 2. Send Request
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/notifications/preferences")
                        .param("userId", userId.toString())
                        .param("enabled", String.valueOf(enabled))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.enabled").value(enabled));
    }

    // 06. clearNotificationHistory
    @Test
    void clearNotificationHistory_shouldReturn200AndClearNotifications() throws Exception {

        // 01. Build Request
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).clearNotifications(userId);

        // 2. Send Request
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/notifications")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // 07. retryFailedNotifications
    @Test
    void retryFailedNotifications_shouldReturn200AndRetryFailedNotifications() throws Exception {

        // 01. Build Request
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).retryFailedNotifications(userId);

        // 2. Send Request
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/notifications")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
