package org.tavall.couriers.web.view.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.web.service.user.UserAccountService;

import java.util.Locale;

@Component
public class UserSessionListener {

    private final UserAccountService userAccountService;

    public UserSessionListener(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event == null) {
            return;
        }
        Authentication authentication = event.getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return;
        }
        String subject = "local:" + username.toLowerCase(Locale.ROOT);
        userAccountService.getOrCreateFromOAuthSubject(subject, username);
        Log.info("User login cached: " + username);
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        if (event == null) {
            return;
        }
        Authentication authentication = event.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        String username = authentication.getName();
        userAccountService.flushUser(username);
        Log.info("User logout flushed: " + username);
    }
}
