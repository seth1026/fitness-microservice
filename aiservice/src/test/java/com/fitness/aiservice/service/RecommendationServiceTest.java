package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Unit Tests")
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    // ─── getUserRecommendations() ─────────────────────────────────────────────

    @Test
    @DisplayName("getUserRecommendations() - returns list of recommendations for userId")
    void getUserRecommendations_validUserId_returnsList() {
        Recommendation rec = new Recommendation();
        rec.setId("rec-001");
        rec.setUserId("user-123");
        rec.setActivityId("activity-001");

        when(recommendationRepository.findByUserId("user-123")).thenReturn(List.of(rec));

        List<Recommendation> results = recommendationService.getUserRecommendations("user-123");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("getUserRecommendations() - returns empty list when no recommendations exist")
    void getUserRecommendations_noResults_returnsEmptyList() {
        when(recommendationRepository.findByUserId("user-999")).thenReturn(List.of());

        List<Recommendation> results = recommendationService.getUserRecommendations("user-999");

        assertThat(results).isEmpty();
    }

    // ─── getActivityRecommendation() ──────────────────────────────────────────

    @Test
    @DisplayName("getActivityRecommendation() - returns recommendation for valid activityId")
    void getActivityRecommendation_validActivityId_returnsRecommendation() {
        Recommendation rec = new Recommendation();
        rec.setId("rec-001");
        rec.setActivityId("activity-001");

        when(recommendationRepository.findByActivityId("activity-001")).thenReturn(Optional.of(rec));

        Recommendation result = recommendationService.getActivityRecommendation("activity-001");

        assertThat(result.getId()).isEqualTo("rec-001");
        assertThat(result.getActivityId()).isEqualTo("activity-001");
    }

    @Test
    @DisplayName("getActivityRecommendation() - throws 404 ResponseStatusException when not found")
    void getActivityRecommendation_notFound_throws404() {
        when(recommendationRepository.findByActivityId("unknown-activity")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getActivityRecommendation("unknown-activity"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Recommendation not ready yet");
                });
    }
}
