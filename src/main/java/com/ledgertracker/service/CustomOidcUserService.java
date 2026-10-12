package com.ledgertracker.service;

import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public CustomOidcUserService(UserRepository userRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        try {
            return processOidcUser(userRequest, oidcUser);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process Google OIDC user", ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("oidc_processing_error"),
                    "Failed to process Google authentication: " + ex.getMessage());
        }
    }

    private OidcUser processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        Map<String, Object> attributes = oidcUser.getAttributes();

        String googleSub = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_provided"),
                    "Email not provided by Google account.");
        }

        if (emailVerified != null && !emailVerified) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"),
                    "Your Google email is unverified. Please verify your email with Google before signing in.");
        }

        Optional<User> userBySub = userRepository.findByGoogleSub(googleSub);
        User user;

        if (userBySub.isPresent()) {
            user = userBySub.get();
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
            log.info("Google OIDC login for existing user id={}", user.getId());
        } else {
            Optional<User> userByEmail = userRepository.findByEmail(email);

            if (userByEmail.isPresent()) {
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
                user = new User();
                user.setEmail(email.toLowerCase());
                user.setPasswordHash(null);
                user.setAuthProvider("GOOGLE");
                user.setGoogleSub(googleSub);
                user.setDisplayName(name != null && !name.isBlank() ? name : email.split("@")[0]);
                user.setAvatarUrl(picture);
                user.setEmailVerified(true);

                user = userRepository.create(user);
                categoryRepository.seedDefaultCategoriesForUser(user.getId());
                log.info("Auto-created new user from Google OIDC id={}", user.getId());
            }
        }

        UserPrincipal principal = UserPrincipal.create(user, attributes);
        principal.setIdToken(oidcUser.getIdToken());
        principal.setUserInfo(oidcUser.getUserInfo());
        return principal;
    }
}
