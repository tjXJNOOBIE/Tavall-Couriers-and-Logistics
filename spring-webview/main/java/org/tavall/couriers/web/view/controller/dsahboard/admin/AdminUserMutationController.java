package org.tavall.couriers.web.view.controller.dsahboard.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.UserAccountEntity;

import java.util.Locale;
import java.util.UUID;

@Controller
public class AdminUserMutationController {

    private final UserAccountService userAccountService;

    public AdminUserMutationController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping(Routes.AUTH_PROMOTE_USER)
    @PreAuthorize("hasAuthority('PERM_USER_PROMOTE_TO_DRIVER')")
    public String promote(@RequestParam("userId") String userId,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        UUID targetId = parseUserId(userId, redirectAttributes);
        UUID actorId = resolveActorId(authentication, redirectAttributes);
        if (targetId == null || actorId == null) {
            return resolveRedirect(redirect);
        }
        try {
            UserAccountEntity updated = userAccountService.promoteToDriver(actorId, targetId);
            redirectAttributes.addFlashAttribute("userStatus", "Promoted " + updated.getUsername() + " to driver.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return resolveRedirect(redirect);
    }

    @PostMapping(Routes.AUTH_DEMOTE_USER)
    @PreAuthorize("hasAuthority('PERM_USER_DEMOTE_FROM_DRIVER')")
    public String demote(@RequestParam("userId") String userId,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        UUID targetId = parseUserId(userId, redirectAttributes);
        UUID actorId = resolveActorId(authentication, redirectAttributes);
        if (targetId == null || actorId == null) {
            return resolveRedirect(redirect);
        }
        try {
            UserAccountEntity updated = userAccountService.demoteFromDriver(actorId, targetId);
            redirectAttributes.addFlashAttribute("userStatus", "Demoted " + updated.getUsername() + ".");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return resolveRedirect(redirect);
    }

    @PostMapping(Routes.AUTH_DELETE_USER)
    @PreAuthorize("hasAuthority('PERM_ADMIN_DELETE_USERS')")
    public String delete(@RequestParam("userId") String userId,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        UUID targetId = parseUserId(userId, redirectAttributes);
        UUID actorId = resolveActorId(authentication, redirectAttributes);
        if (targetId == null || actorId == null) {
            return resolveRedirect(redirect);
        }
        try {
            UserAccountEntity deleted = userAccountService.deleteUser(actorId, targetId);
            redirectAttributes.addFlashAttribute("userStatus", "Deleted " + deleted.getUsername() + ".");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return resolveRedirect(redirect);
    }

    private UUID parseUserId(String userId, RedirectAttributes redirectAttributes) {
        if (userId == null || userId.isBlank()) {
            redirectAttributes.addFlashAttribute("userError", "Missing target user.");
            return null;
        }
        try {
            return UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("userError", "Invalid user identifier.");
            return null;
        }
    }

    private UUID resolveActorId(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("userError", "Active user not resolved.");
            return null;
        }
        String username = authentication.getName().trim();
        UserAccountEntity actor = userAccountService.findByUsername(username);
        if (actor == null) {
            String subject = "local:" + username.toLowerCase(Locale.ROOT);
            actor = userAccountService.getOrCreateFromOAuthSubject(subject, username);
        }
        if (actor == null || actor.getUserUUID() == null) {
            redirectAttributes.addFlashAttribute("userError", "Active user not resolved.");
            return null;
        }
        return actor.getUserUUID();
    }

    private String resolveRedirect(String redirect) {
        if (redirect != null && redirect.startsWith("/")) {
            return "redirect:" + redirect;
        }
        return "redirect:" + Routes.dashboardAdminUsers();
    }
}
