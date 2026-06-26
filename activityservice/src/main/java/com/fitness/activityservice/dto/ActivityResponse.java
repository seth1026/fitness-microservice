package com.fitness.activityservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fitness.activityservice.model.ActivityType;

import lombok.Data;


@Data
public class ActivityResponse {
    
    private String id;
    private String userId;
    private ActivityType type;
    private int duration; // in minutes
    private int caloriesBurned;
    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics; // For additional info like distance, steps, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
