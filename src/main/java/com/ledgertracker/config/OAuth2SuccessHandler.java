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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    public OAuth2SuccessHandler() {
        setDefaultTargetUrl("/index.html");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // DIAGNOSTIC (temporary)
        HttpSession sessionBefore = request.getSession(false);
        log.info("OAUTH-SUCCESS: sessionBefore exists={}, id={}",
            sessionBefore != null,
            sessionBefore != null ? sessionBefore.getId() : "null");
        log.info("OAUTH-SUCCESS: authentication={}, authenticated={}",
            authentication.getClass().getSimpleName(),
            authentication.isAuthenticated());
        log.info("OAUTH-SUCCESS: SecurityContextHolder auth={}",
            SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getClass().getSimpleName() : "null");

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

        log.info("OAUTH-SUCCESS: saved to session id={}, attribute set={}",
            session.getId(),
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) != null);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "/index.html");
    }
}
