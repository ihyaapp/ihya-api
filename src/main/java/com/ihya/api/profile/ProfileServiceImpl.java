package com.ihya.api.profile;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void createProfile(UUID userId) {
        Profile profile = new Profile(userId);
        profileRepository.save(profile);
    }
}