package com.carrental.system.usedCar.CalendarApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @RequestBody CalendarEventDto dto,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {

        if (client == null) {
            return ResponseEntity.status(401).body("GOOGLE_AUTH_REQUIRED");
        }

        try {
            googleCalendarService.createEvent(client, dto);
            return ResponseEntity.ok("EVENT_CREATED");

        } catch (org.springframework.web.server.ResponseStatusException e) {

            // ✅ token 過期 → 回 401（讓前端重新授權）
            if (e.getStatusCode().value() == 401) {
                return ResponseEntity.status(401).body("GOOGLE_TOKEN_EXPIRED");
            }

            throw e;

        } catch (Exception e) {
            return ResponseEntity.status(500).body("SYNC_FAILED");
        }
    }
}