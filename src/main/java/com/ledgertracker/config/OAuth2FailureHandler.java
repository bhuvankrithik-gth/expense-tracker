package com.ledgertracker.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 Authentication failed: {}", exception.getMessage());

        String message = "Authentication failed. Please try again.";
        String exceptionMsg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        if (exceptionMsg.contains("access_denied") || exceptionMsg.contains("cancelled") || exceptionMsg.contains("canceled")) {
            message = "Google Sign-In was cancelled.";
        } else if (exceptionMsg.contains("unverified_email") || exceptionMsg.contains("unverified")) {
            message = "Your Google email is unverified. Please verify your Google email address or register with a password.";
        } else if (exceptionMsg.contains("invalid_token") || exceptionMsg.contains("expired")) {
            message = "The authentication session expired. Please attempt to sign in again.";
        }

        String targetUrl = "/login.html?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
