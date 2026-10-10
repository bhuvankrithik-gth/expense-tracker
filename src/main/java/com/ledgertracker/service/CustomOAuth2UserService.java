package com.ledgertracker.service;

import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public CustomOAuth2UserService(UserRepository userRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process Google OAuth2 user", ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("oauth2_processing_error"),
                    "Failed to process Google authentication: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleSub = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_provided"),
                    "Email not provided by Google account.");
        }

        // Only trust the email if Google reports email_verified = true
        if (emailVerified != null && !emailVerified) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"),
                    "Your Google email is unverified. Please verify your email with Google before signing in.");
        }

        // 1. Look up Google user by google_sub first
        Optional<User> userBySub = userRepository.findByGoogleSub(googleSub);
        User user;

        if (userBySub.isPresent()) {
            user = userBySub.get();
            // Sync display name or avatar if updated on Google
            boolean updated = false;
            if (name != null && !name.isBlank() && !name.equals(user.getDisplayName())) {
                user.setDisplayName(name);
                updated = true;
            }
            if (picture != null && !picture.isBlank() && !picture.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(picture);
                updated = true;
            }
            if (updated) {
                userRepository.update(user);
            }
            log.info("Google OAuth login for existing user id={}", user.getId());
        } else {
            // 2. If no user matches google_sub, check if an existing user matches the verified email
            Optional<User> userByEmail = userRepository.findByEmail(email);

            if (userByEmail.isPresent()) {
                // Link Google to existing password account
                user = userByEmail.get();
                userRepository.linkGoogleAccount(user.getId(), googleSub, picture);
                user.setGoogleSub(googleSub);
                user.setAuthProvider(user.getPasswordHash() != null ? "BOTH" : "GOOGLE");
                user.setEmailVerified(true);
                if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                    user.setAvatarUrl(picture);
                }
                log.info("Linked Google account to existing user id={}", user.getId());
            } else {
                // 3. First-time Google user -> Auto-create account
                user = new User();
                user.setEmail(email.toLowerCase());
                user.setPasswordHash(null);
                user.setAuthProvider("GOOGLE");
                user.setGoogleSub(googleSub);
                user.setDisplayName(name != null && !name.isBlank() ? name : email.split("@")[0]);
                user.setAvatarUrl(picture);
                user.setEmailVerified(true);

                user = userRepository.create(user);
                // Seed standard starting categories for the new user
                categoryRepository.seedDefaultCategoriesForUser(user.getId());
                log.info("Auto-created new user from Google OAuth id={}", user.getId());
            }
        }

        return UserPrincipal.create(user, attributes);
    }
}
