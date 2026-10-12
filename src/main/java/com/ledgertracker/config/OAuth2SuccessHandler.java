package com.ledgertracker.config;

import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import com.ledgertracker.service.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public OAuth2SuccessHandler(UserRepository userRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        setDefaultTargetUrl("/index.html");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            // Convert any OAuth2/OIDC principal to our UserPrincipal
            UserPrincipal userPrincipal = toUserPrincipal(authentication);
            if (userPrincipal == null) {
                logger.error("OAUTH2-SUCCESS: could not convert principal to UserPrincipal");
                getRedirectStrategy().sendRedirect(request, response, "/login.html?error=Authentication+failed");
                return;
            }

            // Create new authentication with UserPrincipal
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                    userPrincipal,
                    userPrincipal.getAuthorities(),
                    oauthToken.getAuthorizedClientRegistrationId()
            );

            // Save to SecurityContextHolder and session (same as password login)
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(newAuth);
            SecurityContextHolder.setContext(sc);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            logger.warn("OAUTH2-SUCCESS: userPrincipal created for userId={} sessionId={}",
                    userPrincipal.getId(), session.getId());
        } catch (Exception e) {
            logger.error("OAUTH2-SUCCESS: failed to process", e);
            getRedirectStrategy().sendRedirect(request, response, "/login.html?error=Authentication+failed");
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "/index.html");
    }

    private UserPrincipal toUserPrincipal(Authentication authentication) {
        if (authentication == null) return null;

        Object principal = authentication.getPrincipal();

        // Already a UserPrincipal (e.g., from CustomOAuth2UserService)
        if (principal instanceof UserPrincipal up) {
            return up;
        }

        // Extract attributes from OIDC or OAuth2 user
        Map<String, Object> attributes = null;
        if (principal instanceof DefaultOidcUser oidc) {
            attributes = oidc.getAttributes();
        } else if (principal instanceof DefaultOAuth2User oauth2) {
            attributes = oauth2.getAttributes();
        } else if (principal instanceof OAuth2User oauth2User) {
            attributes = oauth2User.getAttributes();
        }

        if (attributes == null) {
            logger.error("OAUTH2-SUCCESS: unknown principal type {}", principal.getClass().getName());
            return null;
        }

        String googleSub = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        if (email == null || email.isBlank()) {
            logger.error("OAUTH2-SUCCESS: no email in attributes");
            return null;
        }

        // Find or create user (same logic as CustomOAuth2UserService)
        Optional<User> userBySub = userRepository.findByGoogleSub(googleSub);
        User user;
        if (userBySub.isPresent()) {
            user = userBySub.get();
        } else {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                userRepository.linkGoogleAccount(user.getId(), googleSub, picture);
                user.setGoogleSub(googleSub);
                user.setAuthProvider(user.getPasswordHash() != null ? "BOTH" : "GOOGLE");
                user.setEmailVerified(true);
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
            }
        }

        return UserPrincipal.create(user, attributes);
    }
}
