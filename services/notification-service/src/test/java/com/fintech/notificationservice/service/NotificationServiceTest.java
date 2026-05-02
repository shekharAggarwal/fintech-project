package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.EmailMessageDto;
import com.fintech.notificationservice.dto.PushMessageDto;
import com.fintech.notificationservice.dto.SmsMessageDto;
import com.fintech.notificationservice.entity.Notification;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PreferenceService preferenceService;

    @InjectMocks
    private NotificationService notificationService;

    private static final String USER_ID = "user-123";
    private static final String RECIPIENT = "user@example.com";
    private static final String SUBJECT = "Test Subject";
    private static final String BODY = "Test Body";

    @Nested
    @DisplayName("routeNotification - TRANSACTION_ALERT")
    class TransactionAlertRouting {

        @Test
        @DisplayName("should send to all channels: email, sms, and push")
        void sendsToAllChannels() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "TRANSACTION_ALERT");

            // Email sent via JavaMailSender (async)
            verify(emailSender, timeout(2000)).send(any(SimpleMailMessage.class));
            // SMS via RabbitMQ
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.sms"), any(SmsMessageDto.class));
            // Push via RabbitMQ
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.push"), any(PushMessageDto.class));
        }

        @Test
        @DisplayName("should save notification record after email send")
        void savesNotificationRecord() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "TRANSACTION_ALERT");

            verify(notificationRepository, timeout(2000)).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("routeNotification - OTP")
    class OtpRouting {

        @Test
        @DisplayName("should send SMS as primary channel")
        void sendsSmsAsPrimary() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "OTP");

            ArgumentCaptor<SmsMessageDto> smsCaptor = ArgumentCaptor.forClass(SmsMessageDto.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.sms"), smsCaptor.capture());

            SmsMessageDto sms = smsCaptor.getValue();
            assertThat(sms.getPhoneNumber()).isEqualTo(RECIPIENT);
            assertThat(sms.getMessage()).isEqualTo(BODY);
            assertThat(sms.getType()).isEqualTo("OTP");
        }

        @Test
        @DisplayName("should also send email as fallback")
        void sendsEmailFallback() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "OTP");

            verify(emailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        }
    }

    @Nested
    @DisplayName("routeNotification - MARKETING (preference-based)")
    class MarketingRouting {

        @Test
        @DisplayName("should send email when email preference is enabled and not in quiet hours")
        void sendsEmailWhenEnabled() {
            NotificationPreference emailPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(emailPref));
            when(preferenceService.isInQuietHours(emailPref)).thenReturn(false);

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            verify(emailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should send SMS when sms preference is enabled and not in quiet hours")
        void sendsSmsWhenEnabled() {
            NotificationPreference smsPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.SMS, true);
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(smsPref));
            when(preferenceService.isInQuietHours(smsPref)).thenReturn(false);

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.sms"), any(SmsMessageDto.class));
        }

        @Test
        @DisplayName("should send push when push preference is enabled and not in quiet hours")
        void sendsPushWhenEnabled() {
            NotificationPreference pushPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.PUSH, true);
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(pushPref));
            when(preferenceService.isInQuietHours(pushPref)).thenReturn(false);

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.push"), any(PushMessageDto.class));
        }

        @Test
        @DisplayName("should skip channel when preference is disabled")
        void skipsDisabledChannel() throws Exception {
            NotificationPreference disabledPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, false);
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(disabledPref));

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            Thread.sleep(200);
            verify(emailSender, never()).send(any(SimpleMailMessage.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("should skip channel during quiet hours")
        void skipsChannelDuringQuietHours() throws Exception {
            NotificationPreference pref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            pref.setQuietHoursStart(LocalTime.of(22, 0));
            pref.setQuietHoursEnd(LocalTime.of(7, 0));
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(pref));
            when(preferenceService.isInQuietHours(pref)).thenReturn(true);

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            Thread.sleep(200);
            verify(emailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should not send anything when no preferences exist")
        void noPreferences() throws Exception {
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(Collections.emptyList());

            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "MARKETING");

            Thread.sleep(200);
            verify(emailSender, never()).send(any(SimpleMailMessage.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("routeNotification - Default (WELCOME, unknown types)")
    class DefaultRouting {

        @Test
        @DisplayName("should send email only for WELCOME type")
        void sendsEmailOnlyForWelcome() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "WELCOME");

            verify(emailSender, timeout(2000)).send(any(SimpleMailMessage.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("should send email only for unknown notification type")
        void sendsEmailOnlyForUnknownType() {
            notificationService.routeNotification(USER_ID, RECIPIENT, SUBJECT, BODY, "UNKNOWN_TYPE");

            verify(emailSender, timeout(2000)).send(any(SimpleMailMessage.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("should send email and save notification with SENT status")
        void sendsEmailSuccessfully() throws Exception {
            EmailMessageDto emailDto = new EmailMessageDto(RECIPIENT, SUBJECT, BODY, "WELCOME");

            notificationService.sendEmail(emailDto).join();

            verify(emailSender).send(any(SimpleMailMessage.class));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
            assertThat(saved.getRecipient()).isEqualTo(RECIPIENT);
            assertThat(saved.getSubject()).isEqualTo(SUBJECT);
        }

        @Test
        @DisplayName("should save notification with FAILED status when email sending fails")
        void handlesFailure() {
            doThrow(new RuntimeException("SMTP error")).when(emailSender).send(any(SimpleMailMessage.class));
            EmailMessageDto emailDto = new EmailMessageDto(RECIPIENT, SUBJECT, BODY, "WELCOME");

            try {
                notificationService.sendEmail(emailDto).join();
            } catch (Exception ignored) {
            }

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED);
            assertThat(saved.getErrorMessage()).contains("SMTP error");
        }
    }

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {

        @Test
        @DisplayName("should compose and send welcome email with account details")
        void sendsWelcomeEmail() {
            notificationService.sendWelcomeEmail("user@test.com", "John Doe", "ACC-001", 1000.50);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(emailSender, timeout(2000)).send(captor.capture());

            SimpleMailMessage msg = captor.getValue();
            assertThat(msg.getTo()).contains("user@test.com");
            assertThat(msg.getSubject()).contains("Welcome");
            assertThat(msg.getText()).contains("John Doe");
            assertThat(msg.getText()).contains("ACC-001");
            assertThat(msg.getText()).contains("1000.50");
        }
    }
}
