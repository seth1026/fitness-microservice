package com.fitness.aiservice.repository;

import com.fitness.aiservice.model.Recommendation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserId(String userId);

    Optional<Recommendation> findByActivityId(String activityId);
}
  