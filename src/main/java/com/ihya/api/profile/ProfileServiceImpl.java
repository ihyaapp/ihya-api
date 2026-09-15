package com.ihya.api.profile;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserInterestRepository userInterestRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository, UserInterestRepository userInterestRepository) {
        this.profileRepository = profileRepository;
        this.userInterestRepository = userInterestRepository;
    }

    @Override
    public void createProfile(UUID userId) {
        Profile profile = new Profile(userId);
        profileRepository.save(profile);
    }

    /**
     * A profile row is guaranteed to exist for every user — {@link com.ihya.api.identity.UserService#register}
     * creates it in the same transaction as the user row, with no lazy-create path.
     * A miss here means that invariant was violated, so it's an unchecked failure
     * (logged 500 via the shared catch-all), not a typed domain exception.
     */
    @Override
    public Profile getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No profile row for user " + userId));
    }

    @Override
    public List<String> getInterestSlugs(UUID userId) {
        return userInterestRepository.findAllByUserId(userId).stream()
                .map(UserInterest::getCategorySlug)
                .toList();
    }

    @Override
    public Profile updateProfile(UUID userId, String name, Boolean personalizePromptDismissed, List<String> interests) {
        Profile profile = getProfile(userId);
        if (name != null) {
            profile.setName(name);
        }
        if (personalizePromptDismissed != null) {
            profile.setPersonalizePromptDismissed(personalizePromptDismissed);
        }
        Profile savedProfile = profileRepository.save(profile);

        if (interests != null) {
            userInterestRepository.deleteAllByUserId(userId);
            List<UserInterest> userInterests = interests.stream()
                    .distinct()
                    .map(slug -> new UserInterest(userId, slug))
                    .toList();
            userInterestRepository.saveAll(userInterests);
        }

        return savedProfile;
    }
}