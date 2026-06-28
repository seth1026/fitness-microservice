package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService Unit Tests")
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ActivityService activityService;

    private ActivityRequest request;
    private Activity savedActivity;

    @BeforeEach
    void setUp() {
        // Inject @Value fields that would normally come from config
        ReflectionTestUtils.setField(activityService, "exchange", "fitness-exchange");
        ReflectionTestUtils.setField(activityService, "routingKey", "activity.tracking");

        request = new ActivityRequest();
        request.setUserId("user-123");
        request.setType(ActivityType.RUNNING);
        request.setDuration(30);
        request.setCaloriesBurned(300);
        request.setStartTime(LocalDateTime.now());

        savedActivity = Activity.builder()
                .id("activity-001")
                .userId("user-123")
                .type(ActivityType.RUNNING)
                .duration(30)
                .caloriesBurned(300)
                .startTime(request.getStartTime())
                .build();
    }

    // ─── trackActivity() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("trackActivity() - saves activity and publishes to RabbitMQ for valid user")
    void trackActivity_validUser_savesAndPublishes() {
        when(userValidationService.validateUser("user-123")).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        ActivityResponse response = activityService.trackActivity(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("activity-001");
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getType()).isEqualTo(ActivityType.RUNNING);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("fitness-exchange"), eq("activity.tracking"), eq(savedActivity));
    }

    @Test
    @DisplayName("trackActivity() - throws RuntimeException for invalid user")
    void trackActivity_invalidUser_throwsException() {
        when(userValidationService.validateUser("user-123")).thenReturn(false);

        assertThatThrownBy(() -> activityService.trackActivity(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid User");

        verify(activityRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("trackActivity() - still returns response even if RabbitMQ publish fails")
    void trackActivity_rabbitMqFails_stillReturnsResponse() {
        when(userValidationService.validateUser("user-123")).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        ActivityResponse response = activityService.trackActivity(request);

        // Should not throw - RabbitMQ failure is caught internally
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("activity-001");
    }

    // ─── getUserActivities() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getUserActivities() - returns mapped list for a userId")
    void getUserActivities_returnsListOfResponses() {
        when(activityRepository.findByUserId("user-123")).thenReturn(List.of(savedActivity));

        List<ActivityResponse> activities = activityService.getUserActivities("user-123");

        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getId()).isEqualTo("activity-001");
    }

    @Test
    @DisplayName("getUserActivities() - returns empty list for user with no activities")
    void getUserActivities_noActivities_returnsEmptyList() {
        when(activityRepository.findByUserId("user-456")).thenReturn(List.of());

        List<ActivityResponse> activities = activityService.getUserActivities("user-456");

        assertThat(activities).isEmpty();
    }

    // ─── getActivityById() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getActivityById() - returns response for valid activityId")
    void getActivityById_validId_returnsResponse() {
        when(activityRepository.findById("activity-001")).thenReturn(Optional.of(savedActivity));

        ActivityResponse response = activityService.getActivityById("activity-001");

        assertThat(response.getId()).isEqualTo("activity-001");
        assertThat(response.getDuration()).isEqualTo(30);
    }

    @Test
    @DisplayName("getActivityById() - throws RuntimeException for unknown activityId")
    void getActivityById_unknownId_throwsException() {
        when(activityRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.getActivityById("unknown-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
    }
}
