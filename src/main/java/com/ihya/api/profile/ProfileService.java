package com.ihya.api.profile;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    void createProfile(UUID userId);
    Profile getProfile(UUID userId);
    List<String> getInterestSlugs(UUID userId);
    Profile updateProfile(UUID userId, String name, Boolean personalizePromptDismissed, List<String> interests);
}