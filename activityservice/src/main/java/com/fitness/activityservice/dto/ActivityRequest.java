package com.fitness.activityservice.dto;

import com.fitness.activityservice.model.ActivityType;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ActivityRequest {
    private String userId;
    private ActivityType type;
    private int duration; // in minutes
    private int caloriesBurned;
    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics; // For additional info like distance, steps,
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
}
