package com.ledgertracker.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public OAuth2SuccessHandler() {
        setDefaultTargetUrl("/index.html");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Rely on Spring Security's built-in session persistence
        // (AbstractAuthenticationProcessingFilter saves via SecurityContextRepository).
        // Custom manual save removed to isolate the issue.
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "/index.html");
    }
}
