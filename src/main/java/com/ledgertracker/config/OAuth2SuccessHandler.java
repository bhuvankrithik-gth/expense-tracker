package com.ledgertracker.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    public OAuth2SuccessHandler() {
        setDefaultTargetUrl("/index.html");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession sess = request.getSession(false);
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        logger.warn("OAUTH2-SUCCESS-HANDLER-CALLED auth={} principalType={} sessionExists={} sessionId={}",
                authentication != null ? authentication.getClass().getSimpleName() : "null",
                principal != null ? principal.getClass().getName() : "null",
                sess != null,
                sess != null ? sess.getId() : "null");
        logger.warn("OAUTH2-SUCCESS-HANDLER-CALLED holderAuth={}",
                SecurityContextHolder.getContext().getAuthentication() != null ? "present" : "null");
        // Explicitly persist like password login does
        org.springframework.security.core.context.SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(authentication);
        SecurityContextHolder.setContext(sc);
        HttpSession s2 = request.getSession(true);
        s2.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        logger.warn("OAUTH2-SUCCESS-HANDLER-CALLED manualSaveDone sessionId={}", s2.getId());
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "/index.html");
    }
}
