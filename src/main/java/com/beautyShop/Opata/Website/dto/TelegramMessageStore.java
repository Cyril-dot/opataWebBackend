package com.beautyShop.Opata.Website.dto;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which Telegram message came from which user.
 * When admin replies to a forwarded message, we look up
 * the original sender's chat ID and forward the reply back.
 *
 * Key:   Telegram message ID of the forwarded message in admin chat
 * Value: Original user's Telegram chat ID
 */
@Component
public class TelegramMessageStore {

    // messageId → user chatId
    private final Map<Integer, Long> messageToUserChat = new ConcurrentHashMap<>();

    // user chatId → user's first name (for display)
    private final Map<Long, String> chatIdToName = new ConcurrentHashMap<>();

    // user chatId → their last message ID sent to admin
    private final Map<Long, Integer> userChatToLastMessage = new ConcurrentHashMap<>();

    public void registerMessage(Integer forwardedMessageId, Long userChatId, String userName) {
        messageToUserChat.put(forwardedMessageId, userChatId);
        chatIdToName.put(userChatId, userName);
        userChatToLastMessage.put(userChatId, forwardedMessageId);
    }

    public Long getUserChatId(Integer forwardedMessageId) {
        return messageToUserChat.get(forwardedMessageId);
    }

    public String getUserName(Long userChatId) {
        return chatIdToName.getOrDefault(userChatId, "Unknown User");
    }

    public void clear(Integer messageId) {
        Long chatId = messageToUserChat.remove(messageId);
        if (chatId != null) {
            chatIdToName.remove(chatId);
            userChatToLastMessage.remove(chatId);
        }
    }
}