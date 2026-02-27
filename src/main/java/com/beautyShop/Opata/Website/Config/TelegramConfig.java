package com.beautyShop.Opata.Website.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfig {

    private Bot bot = new Bot();
    private String channelId;

    @Data
    public static class Bot {
        private String token;
        private String username;
    }
}