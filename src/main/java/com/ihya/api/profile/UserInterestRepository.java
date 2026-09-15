package com.ihya.api.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserInterestRepository extends JpaRepository<UserInterest, UserInterest.UserInterestId> {

    List<UserInterest> findAllByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
