package com.ledgertracker.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
        // Explicitly persist the SecurityContext into the HttpSession, mirroring
        // the working password-login flow in AuthController. Spring Security 6's
        // requireExplicitSave mode relies on the repository save in
        // AbstractAuthenticationProcessingFilter, which was not surviving the
        // OAuth2 redirect round-trip in some browsers/proxies.
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null) {
            context.setAuthentication(authentication);
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "/index.html");
    }
}
