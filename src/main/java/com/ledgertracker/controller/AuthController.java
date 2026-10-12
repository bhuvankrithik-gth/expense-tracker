package com.ledgertracker.controller;

import com.ledgertracker.dto.ApiResponse;
import com.ledgertracker.dto.LoginRequest;
import com.ledgertracker.dto.RegisterRequest;
import com.ledgertracker.dto.UserDto;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
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
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.error("Unauthenticated"));
        }
        UserDto userDto = authService.getUserDto(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                                          HttpServletRequest httpRequest) {
        HttpSession sess = httpRequest.getSession(false);
        org.springframework.security.core.Authentication ctxAuth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        logger.warn("AUTH-STATUS-CHECK sessionExists={} sessionId={} principalPresent={} ctxAuthType={} ctxPrincipalType={}",
                sess != null,
                sess != null ? sess.getId() : "null",
                principal != null,
                ctxAuth != null ? ctxAuth.getClass().getSimpleName() : "null",
                ctxAuth != null && ctxAuth.getPrincipal() != null ? ctxAuth.getPrincipal().getClass().getName() : "null");
        Map<String, Object> status = new HashMap<>();
        if (principal != null) {
            status.put("authenticated", true);
            status.put("user", authService.getUserDto(principal.getId()));
        } else {
            status.put("authenticated", false);
            status.put("user", null);
        }
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
