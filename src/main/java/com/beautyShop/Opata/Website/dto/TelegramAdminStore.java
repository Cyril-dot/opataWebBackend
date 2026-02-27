package com.beautyShop.Opata.Website.dto;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelegramAdminStore {

    private final AtomicLong adminChatId = new AtomicLong(0L);

    public void setAdminChatId(Long chatId) {
        adminChatId.set(chatId);
    }

    public Long getAdminChatId() {
        long id = adminChatId.get();
        return id == 0L ? null : id;
    }

    public boolean isRegistered() {
        return adminChatId.get() != 0L;
    }
}