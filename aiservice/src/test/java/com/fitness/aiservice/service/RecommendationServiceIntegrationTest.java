package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RecommendationService Integration Tests")
class RecommendationServiceIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @AfterEach
    void cleanUp() {
        recommendationRepository.deleteAll();
    }

    @Test
    @DisplayName("getActivityRecommendation() - retrieves recommendation persisted to MongoDB")
    void getActivityRecommendation_afterSave_returnsFromMongo() {
        Recommendation rec = new Recommendation();
        rec.setUserId("user-123");
        rec.setActivityId("activity-001");
        rec.setAiResponse("Keep it up!");
        recommendationRepository.save(rec);

        Recommendation result = recommendationService.getActivityRecommendation("activity-001");

        assertThat(result.getActivityId()).isEqualTo("activity-001");
        assertThat(result.getAiResponse()).isEqualTo("Keep it up!");
    }

    @Test
    @DisplayName("getActivityRecommendation() - throws 404 when recommendation not in MongoDB")
    void getActivityRecommendation_notFound_throws404() {
        assertThatThrownBy(() -> recommendationService.getActivityRecommendation("not-in-db"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("getUserRecommendations() - retrieves all recommendations for a userId")
    void getUserRecommendations_afterSavingMultiple_returnsAll() {
        Recommendation r1 = new Recommendation();
        r1.setUserId("user-456");
        r1.setActivityId("act-001");

        Recommendation r2 = new Recommendation();
        r2.setUserId("user-456");
        r2.setActivityId("act-002");

        recommendationRepository.saveAll(List.of(r1, r2));

        List<Recommendation> results = recommendationService.getUserRecommendations("user-456");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Recommendation::getUserId).containsOnly("user-456");
    }

    @Test
    @DisplayName("getUserRecommendations() - returns empty list for user with no recommendations")
    void getUserRecommendations_noResults_returnsEmpty() {
        List<Recommendation> results = recommendationService.getUserRecommendations("user-no-recs");
        assertThat(results).isEmpty();
    }
}
