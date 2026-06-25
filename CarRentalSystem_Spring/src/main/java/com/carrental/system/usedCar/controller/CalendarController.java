//package com.carrental.system.usedCar.controller;
//
//import java.io.IOException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.carrental.system.usedCar.DTO.EventRequest;
//import com.google.api.client.auth.oauth2.BearerToken;
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.util.DateTime;
//import com.google.api.services.calendar.Calendar;
//import com.google.api.services.calendar.model.Event;
//import com.google.api.services.calendar.model.EventDateTime;
//
//
//@RestController
//@RequestMapping("/api/calendar")
//public class CalendarController {
//
//    @Autowired
//    private OAuth2AuthorizedClientService authorizedClientService;
//
//    @PostMapping("/addEvent")
//    public String addEvent(Authentication authentication, @RequestBody EventRequest eventReq) {
//        // 1. 取得該使用者的授權用戶端
//        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
//        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
//            oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
//
//        String accessToken = client.getAccessToken().getTokenValue();
//
//        // 2. 設定 Credential
//        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
//            .setAccessToken(accessToken);
//
//        // 3. 建立 Google Calendar 服務
//        Calendar service = new Calendar.Builder(
//                new NetHttpTransport(),
//                GsonFactory.getDefaultInstance(),
//                credential)
//                .setApplicationName("CarRental").build();
//
//        // 4. 建立並寫入活動
//        Event event = new Event()
//            .setSummary(eventReq.getCarName())
//            .setStart(new EventDateTime().setDateTime(new DateTime(eventReq.getStartTime())))
//            .setEnd(new EventDateTime().setDateTime(new DateTime(eventReq.getEndTime())));
//
//        try {
//            // 4. 寫入行事曆
//            service.events().insert("primary", event).execute();
//            return "成功寫入行事曆！";
//        } catch (IOException e) {
//            // 發生錯誤時的處理方式
//            e.printStackTrace(); // 將錯誤印在 Console 方便除錯
//            return "同步失敗，請稍後再試。錯誤內容：" + e.getMessage();
//        }
//        
//    }
//}