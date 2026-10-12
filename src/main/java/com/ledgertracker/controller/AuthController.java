package com.ledgertracker.controller;

import com.ledgertracker.dto.ApiResponse;
import com.ledgertracker.dto.LoginRequest;
import com.ledgertracker.dto.RegisterRequest;
import com.ledgertracker.dto.UserDto;
import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import com.ledgertracker.service.AuthService;
import com.ledgertracker.service.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService,
                          UserRepository userRepository, CategoryRepository categoryRepository) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserDto userDto = authService.getUserDto(principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", userDto));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        UserDto registeredUser = authService.register(request);

        // Auto authenticate after successful registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.ok(ApiResponse.success("Account created successfully", registeredUser));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal,
                                                               HttpServletRequest httpRequest) {
        UserPrincipal effectivePrincipal = principal;
        if (effectivePrincipal == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                effectivePrincipal = provisionOidcUser(auth, httpRequest);
            }
        }
        if (effectivePrincipal == null) {
            return ResponseEntity.ok(ApiResponse.error("Unauthenticated"));
        }
        UserDto userDto = authService.getUserDto(effectivePrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                                          HttpServletRequest httpRequest) {
        HttpSession sess = httpRequest.getSession(false);
        Authentication ctxAuth = SecurityContextHolder.getContext().getAuthentication();

        // Lazy provisioning: if principal is OIDC/OAuth2 default type, convert to UserPrincipal
        UserPrincipal effectivePrincipal = principal;
        if (effectivePrincipal == null && ctxAuth != null) {
            effectivePrincipal = provisionOidcUser(ctxAuth, httpRequest);
        }

        Map<String, Object> status = new HashMap<>();
        if (effectivePrincipal != null) {
            status.put("authenticated", true);
            status.put("user", authService.getUserDto(effectivePrincipal.getId()));
        } else {
            status.put("authenticated", false);
            status.put("user", null);
        }
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * If the authentication principal is a default OIDC/OAuth2 user (not our UserPrincipal),
     * provision the user in our DB and upgrade the session to use UserPrincipal.
     */
    private UserPrincipal provisionOidcUser(Authentication auth, HttpServletRequest request) {
        try {
            Object p = auth.getPrincipal();
            Map<String, Object> attributes = null;
            if (p instanceof org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser oidc) {
                attributes = oidc.getAttributes();
            } else if (p instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2) {
                attributes = oauth2.getAttributes();
            }
            if (attributes == null) return null;

            String googleSub = (String) attributes.get("sub");
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String picture = (String) attributes.get("picture");
            if (email == null || email.isBlank()) return null;

            User user;
            Optional<User> bySub = userRepository.findByGoogleSub(googleSub);
            if (bySub.isPresent()) {
                user = bySub.get();
            } else {
                Optional<User> byEmail = userRepository.findByEmail(email);
                if (byEmail.isPresent()) {
                    user = byEmail.get();
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

            UserPrincipal up = UserPrincipal.create(user, attributes);

            // Upgrade the session authentication to use UserPrincipal
            OAuth2AuthenticationToken oldToken = (OAuth2AuthenticationToken) auth;
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                    up, up.getAuthorities(), oldToken.getAuthorizedClientRegistrationId());
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(newAuth);
            SecurityContextHolder.setContext(sc);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            logger.warn("AUTH-STATUS: provisioned OIDC user id={}", user.getId());
            return up;
        } catch (Exception e) {
            logger.error("AUTH-STATUS: failed to provision OIDC user", e);
            return null;
        }
    }
}
