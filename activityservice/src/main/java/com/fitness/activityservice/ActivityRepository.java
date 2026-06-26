package com.fitness.activityservice;

import com.fitness.activityservice.model.Activity;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {
    // Custom query methods can be defined here if needed
    List<Activity> findByUserId(String userId);
}
