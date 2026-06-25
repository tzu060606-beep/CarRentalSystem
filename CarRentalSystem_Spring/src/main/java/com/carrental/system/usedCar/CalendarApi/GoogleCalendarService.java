package com.carrental.system.usedCar.CalendarApi;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

@Service
public class GoogleCalendarService {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    // 重點 1：將參數改為 OAuth2AuthorizedClient，不再依賴 Authentication 物件
    public void createEvent(OAuth2AuthorizedClient client, CalendarEventDto dto) throws Exception {

        // 1. 檢查授權用戶端與 Token 是否存在
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("GOOGLE_TOKEN_EXPIRED");
        }

        // 2. 取得 access token
        String accessToken = client.getAccessToken().getTokenValue();

        // 3. 建立 Google credential
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        // 4. 初始化 Google Calendar Service
        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), 
                credential)
                .setApplicationName("Used Car Management System")
                .build();

        // 5. 建立 Event 物件
        Event event = new Event()
                .setSummary(dto.getTitle()) // 這裡確認你的 DTO 欄位名稱是否為 title
                .setDescription(dto.getDescription())
                .setLocation(dto.getLocation());

        // 6. 設定開始與結束時間 (使用 Asia/Taipei)
        // 假設 dto.getStartTime() 回傳的是 LocalDateTime
        DateTime startDateTime = new DateTime(dto.getStartTime().atZone(ZoneId.of("Asia/Taipei")).toInstant().toEpochMilli());
        event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Taipei"));

        DateTime endDateTime = new DateTime(dto.getEndTime().atZone(ZoneId.of("Asia/Taipei")).toInstant().toEpochMilli());
        event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Taipei"));

        // 7. 執行插入
        try {
            service.events().insert("primary", event).execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_EXPIRED");
            }
            throw e;
        }
    }
}