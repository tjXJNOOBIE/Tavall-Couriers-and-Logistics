package org.tavall.couriers.web.view.controller.register;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tavall.couriers.api.web.service.utils.RateLimitService;

import java.util.Map;

@RestController
public class RegistrationPageController {


    @Autowired
    private RateLimitService rateLimitService;

    // We pull this from your ButtonConfig logic (usually 4000ms)
    private static final int REGISTER_COOLDOWN = 4000;

    @PostMapping("/api/register")
    public ResponseEntity<?> handleRegistration(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);

        if (!rateLimitService.isAllowed(clientIp, REGISTER_COOLDOWN)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(REGISTER_COOLDOWN / 1000))
                    .body(Map.of("error", "registration is coming soon"));
        }

        return ResponseEntity.ok(Map.of("message", "registration is coming soon"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "client, proxy1, proxy2" -> take the first
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}