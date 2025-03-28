package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationTypeRequest;
import app.web.dto.UpsertNotificationPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    // 1. changeNotificationPreference
    @Test
    void givenNotExistingNotificationPreference_whenChangeNotificationPreference_thenExpectException() {

        // Given
        UUID userId = UUID.randomUUID();
        boolean isNotificationEnabled = true;
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NullPointerException.class, () -> notificationService.changeNotificationPreference(userId, isNotificationEnabled));
    }

    @Test
    void givenExistingNotificationPreference_whenChangeNotificationPreference_thenExpectingEnabledToBeChanged() {

        // Given
        UUID userId = UUID.randomUUID();
        boolean isNotificationEnabled = true;
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(false)
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));

        // When
        notificationService.changeNotificationPreference(userId, isNotificationEnabled);

        // Then
        assertTrue(notificationPreference.isEnabled());
        verify(notificationPreferenceRepository, times(1)).save(notificationPreference);
    }

    // 2. getPreferenceByUserId
    @Test
    void givenUserIdThatDoesNotExistInTheDatabase_thenThrowException() {

        // Given
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserId(userId)).thenThrow(new NullPointerException());

        // When & Then
        assertThrows(NullPointerException.class, () -> notificationService.getPreferenceByUserId(userId));
        verify(notificationPreferenceRepository, times(1)).findByUserId(userId);
    }

    @Test
    void givenUserIdThatDoesExistInTheDatabase_thenReturnNotificationPreference() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("test@email.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));

        // When
        NotificationPreference returnedOptionalNotificationPreference = notificationService.getPreferenceByUserId(userId);

        // Then
        assertNotNull(returnedOptionalNotificationPreference);
        assertEquals(notificationPreference.getId(), returnedOptionalNotificationPreference.getId());
        assertEquals(notificationPreference.getUserId(), returnedOptionalNotificationPreference.getUserId());
        assertEquals(notificationPreference.getType(), returnedOptionalNotificationPreference.getType());
        assertEquals(notificationPreference.isEnabled(), returnedOptionalNotificationPreference.isEnabled());
        assertEquals(notificationPreference.getContactInfo(), returnedOptionalNotificationPreference.getContactInfo());
        assertEquals(notificationPreference.getCreatedOn(), returnedOptionalNotificationPreference.getCreatedOn());
        assertEquals(notificationPreference.getUpdatedOn(), returnedOptionalNotificationPreference.getUpdatedOn());
        verify(notificationPreferenceRepository, times(1)).findByUserId(userId);
    }

    // .3 getNotificationHistory
    @Test
    void givenUserIdThatIsValidInTheDatabase_ThenReturnListOfNotification() {

        // Given
        UUID userId = UUID.randomUUID();

        Notification notificationOne = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment")
                .body("Body Of Email")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .isDeleted(false)
                .build();

        Notification notificationTwo = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment")
                .body("Body Of Email")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .isDeleted(false)
                .build();

        List<Notification> listOfNotifications = List.of(notificationOne, notificationTwo);

        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(userId)).thenReturn(listOfNotifications);

        // When
        List<Notification> returnedListOfNotificationHistory = notificationService.getNotificationHistory(userId);

        // Then
        Notification firstNotification = returnedListOfNotificationHistory.get(0);
        assertEquals(notificationOne.getId(), firstNotification.getId());
        assertEquals(notificationOne.getUserId(), firstNotification.getUserId());
        assertEquals(notificationOne.getSubject(), firstNotification.getSubject());
        assertEquals(notificationOne.getBody(), firstNotification.getBody());
        assertEquals(notificationOne.getCreatedOn(), firstNotification.getCreatedOn());
        assertEquals(notificationOne.getStatus(), firstNotification.getStatus());
        assertEquals(notificationOne.getType(), firstNotification.getType());
        assertEquals(notificationOne.isDeleted(), firstNotification.isDeleted());

        verify(notificationRepository, times(1)).findAllByUserIdAndDeletedIsFalse(userId);
    }

    // 4. clearNotifications
    @Test
    void givenUserIdThatIsValidInTheDatabase_thenClearNotifications() {

        // Given
        UUID userId = UUID.randomUUID();

        Notification notificationOne = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment")
                .body("Body Of Email")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .isDeleted(false)
                .build();

        Notification notificationTwo = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment")
                .body("Body Of Email")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .isDeleted(false)
                .build();

        List<Notification> listOfNotifications = List.of(notificationOne, notificationTwo);
        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(userId)).thenReturn(listOfNotifications);

        // When
        notificationService.clearNotifications(userId);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getId().equals(notificationOne.getId()) && notification.isDeleted()
        ));

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getId().equals(notificationTwo.getId()) && notification.isDeleted()
        ));

        verify(notificationRepository, times(1)).findAllByUserIdAndDeletedIsFalse(userId);
    }

    // 5. retryFailedNotifications
    @Test
    void givenUserIdWithNoPreference_thenThrowException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class,
                () -> notificationService.getPreferenceByUserId(userId));
    }

    @Test
    void givenUserIdWithDisabledNotifications_ThenThrowException() {
        // Given
        UUID userId = UUID.randomUUID();

        NotificationPreference disabledPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(false)  // Setting the preference to disabled
                .contactInfo("test@email.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(disabledPreference));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> notificationService.retryFailedNotifications(userId));
        assertEquals(String.format("User with id %s does not allow to receive notifications.", userId), exception.getMessage());
    }

    @Test
    void givenUserIdWithFailedNotifications_thenRetrieveFailedNotifications() {

        // Given
        UUID userId = UUID.randomUUID();
        Notification failedNotificationOne = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment Failed")
                .body("Your payment attempt has failed.")
                .status(NotificationStatus.FAILED)
                .isDeleted(false)
                .build();

        Notification failedNotificationTwo = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment Failed")
                .body("Your payment attempt has failed.")
                .status(NotificationStatus.FAILED)
                .isDeleted(true)
                .build();

        List<Notification> failedNotifications = List.of(failedNotificationOne, failedNotificationTwo);

        when(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED))
                .thenReturn(failedNotifications);

        NotificationPreference userPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .enabled(true)  // Notifications are enabled
                .contactInfo("test@email.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(userPreference));

        // When
        notificationService.retryFailedNotifications(userId);
        verify(notificationRepository, times(1)).save(failedNotificationOne);
        verify(notificationRepository, never()).save(failedNotificationTwo);
        verify(notificationRepository, times(1)).findAllByUserIdAndStatus(userId, NotificationStatus.FAILED);
    }

    @Test
    void givenUserIdWithFailedNotificationsIncludingDeleted_thenFilterOutDeleted() {

        // Given
        UUID userId = UUID.randomUUID();

        Notification failedNotificationOne = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment Failed")
                .body("Your payment attempt has failed.")
                .status(NotificationStatus.FAILED)
                .isDeleted(false)  // Not deleted
                .build();

        Notification failedNotificationTwo = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Payment Failed")
                .body("Your payment attempt has failed.")
                .status(NotificationStatus.FAILED)
                .isDeleted(true)  // Deleted
                .build();

        List<Notification> failedNotifications = List.of(failedNotificationOne, failedNotificationTwo);

        when(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED)).thenReturn(failedNotifications);

        NotificationPreference userPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .enabled(true)
                .contactInfo("test@email.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(userPreference));

        // When
        notificationService.retryFailedNotifications(userId);

        // Then
        verify(notificationRepository, times(1)).findAllByUserIdAndStatus(userId, NotificationStatus.FAILED);
        verify(notificationRepository, times(1)).save(failedNotificationOne);
        verify(notificationRepository, never()).save(failedNotificationTwo);
    }

    // 6.sendNotification
    @Test
    void givenUserIdWithExistingPreference_whenUpsert_thenUpdatePreference() {

        // Given
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("test@abv.bg")
                .build();

        NotificationPreference existingPreference = NotificationPreference.builder()
                .userId(userId)
                .contactInfo("oldEmail@example.com")
                .enabled(false)
                .type(NotificationType.EMAIL)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference updatedPreference = notificationService.upsertPreference(dto);

        // Then
        assertNotNull(updatedPreference);
        assertEquals(dto.getContactInfo(), updatedPreference.getContactInfo());
        assertEquals(dto.isNotificationEnabled(), updatedPreference.isEnabled());
        verify(notificationPreferenceRepository, times(1)).save(updatedPreference);
    }

    @Test
    void givenUserIdWithNoExistingPreference_whenUpsert_thenCreateNewPreference() {

        // Given
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("test@abv.bg")
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));  // Return the argument passed to save

        // When
        NotificationPreference newPreference = notificationService.upsertPreference(dto);

        // Then
        assertNotNull(newPreference);
        assertEquals(dto.getUserId(), newPreference.getUserId());
        assertEquals(dto.getContactInfo(), newPreference.getContactInfo());
        assertEquals(dto.isNotificationEnabled(), newPreference.isEnabled());
        assertNotNull(newPreference.getCreatedOn());
        assertNotNull(newPreference.getUpdatedOn());
        verify(notificationPreferenceRepository, times(1)).save(newPreference);
    }

    @Test
    void givenUserPreferenceIsEnabled_whenSendNotification_thenSendNotificationSuccessfully() {

        // Given
        UUID userId = UUID.randomUUID();
        String contactInfo = "test@email.com";
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("subject")
                .body("body")
                .build();

        NotificationPreference userPreference = NotificationPreference.builder()
                .userId(userId)
                .contactInfo(contactInfo)
                .enabled(true)
                .type(NotificationType.EMAIL)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        SimpleMailMessage mockMessage = new SimpleMailMessage();
        mockMessage.setTo(contactInfo);
        mockMessage.setSubject(notificationRequest.getSubject());
        mockMessage.setText(notificationRequest.getBody());

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(userPreference));
        doNothing().when(mailSender).send(mockMessage);
        Notification savedNotification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .isDeleted(false)
                .type(NotificationType.EMAIL)
                .status(NotificationStatus.SUCCEEDED)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification notification = notificationService.sendNotification(notificationRequest);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();

        assertNotNull(capturedNotification);
        assertEquals(NotificationStatus.SUCCEEDED, capturedNotification.getStatus());
        assertEquals(notificationRequest.getSubject(), capturedNotification.getSubject());
        assertEquals(notificationRequest.getBody(), capturedNotification.getBody());

        assertEquals(userId, capturedNotification.getUserId());
        assertEquals(NotificationType.EMAIL, capturedNotification.getType());
        assertFalse(capturedNotification.isDeleted());

        verify(mailSender, times(1)).send(mockMessage);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void givenUserPreferenceIsEnabled_whenSendNotificationFails_thenUpdateNotificationStatusToFailed() {

        // Given
        UUID userId = UUID.randomUUID();
        String contactInfo = "test@email.com";
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("subject")
                .body("body")
                .build();

        NotificationPreference userPreference = NotificationPreference.builder()
                .userId(userId)
                .contactInfo(contactInfo)
                .enabled(true)
                .type(NotificationType.EMAIL)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        doThrow(new org.springframework.mail.MailSendException("Mail send failed")).when(mailSender).send(any(SimpleMailMessage.class));

        Notification savedNotification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .isDeleted(false)
                .type(NotificationType.EMAIL)
                .status(NotificationStatus.FAILED)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(userPreference));

        // When
        Notification notification = notificationService.sendNotification(notificationRequest);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();

        assertNotNull(capturedNotification);
        assertEquals(NotificationStatus.FAILED, capturedNotification.getStatus());
        assertEquals(notificationRequest.getSubject(), capturedNotification.getSubject());
        assertEquals(notificationRequest.getBody(), capturedNotification.getBody());
        assertEquals(userId, capturedNotification.getUserId());
        assertEquals(NotificationType.EMAIL, capturedNotification.getType());
        assertFalse(capturedNotification.isDeleted());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

}
