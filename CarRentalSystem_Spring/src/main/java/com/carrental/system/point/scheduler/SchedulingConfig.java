package com.carrental.system.point.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 空的就好，只是為了啟用排程功能
    // 【為什麼這樣做】不動主程式，把排程設定隔離在 F3 模組裡
}