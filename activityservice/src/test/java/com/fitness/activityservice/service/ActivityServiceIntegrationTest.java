package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ActivityService Integration Tests")
class ActivityServiceIntegrationTest {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityRepository activityRepository;

    // Mock RabbitMQ and UserValidation — no need for real infrastructure in integration tests
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private UserValidationService userValidationService;

    @AfterEach
    void cleanUp() {
        activityRepository.deleteAll();
    }

    @Test
    @DisplayName("trackActivity() - persists activity to MongoDB and returns valid response")
    void trackActivity_validUser_persistsToMongo() {
        when(userValidationService.validateUser("user-123")).thenReturn(true);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        ActivityRequest request = buildRequest("user-123", ActivityType.RUNNING, 30, 300);

        ActivityResponse response = activityService.trackActivity(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getType()).isEqualTo(ActivityType.RUNNING);
        assertThat(response.getDuration()).isEqualTo(30);

        // Verify it actually saved to MongoDB
        assertThat(activityRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("getUserActivities() - retrieves all activities for a user from MongoDB")
    void getUserActivities_returnsPersistedActivities() {
        when(userValidationService.validateUser("user-456")).thenReturn(true);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        activityService.trackActivity(buildRequest("user-456", ActivityType.CYCLING, 45, 400));
        activityService.trackActivity(buildRequest("user-456", ActivityType.SWIMMING, 60, 500));

        List<ActivityResponse> activities = activityService.getUserActivities("user-456");

        assertThat(activities).hasSize(2);
        assertThat(activities).extracting(ActivityResponse::getUserId).containsOnly("user-456");
    }

    @Test
    @DisplayName("getActivityById() - retrieves persisted activity by its ID")
    void getActivityById_afterSave_returnsCorrectActivity() {
        when(userValidationService.validateUser("user-789")).thenReturn(true);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        ActivityResponse saved = activityService.trackActivity(
                buildRequest("user-789", ActivityType.YOGA, 20, 100)
        );

        ActivityResponse fetched = activityService.getActivityById(saved.getId());

        assertThat(fetched.getId()).isEqualTo(saved.getId());
        assertThat(fetched.getType()).isEqualTo(ActivityType.YOGA);
    }

    @Test
    @DisplayName("getActivityById() - throws RuntimeException for non-existent activityId")
    void getActivityById_unknownId_throwsException() {
        assertThatThrownBy(() -> activityService.getActivityById("does-not-exist"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
    }

    private ActivityRequest buildRequest(String userId, ActivityType type, int duration, int calories) {
        ActivityRequest request = new ActivityRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setDuration(duration);
        request.setCaloriesBurned(calories);
        request.setStartTime(LocalDateTime.now());
        return request;
    }
}
