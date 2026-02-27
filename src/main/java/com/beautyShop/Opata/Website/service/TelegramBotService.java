package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.Config.TelegramConfig;
import com.beautyShop.Opata.Website.dto.TelegramAdminStore;
import com.beautyShop.Opata.Website.dto.TelegramMessageStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramConfig telegramConfig;
    private final TelegramMessageStore messageStore;
    private final TelegramAdminStore adminStore;

    // Tracks users who are currently in "image inquiry" mode
    // Key: userChatId, Value: fileId of the image they sent
    private final Map<Long, String> pendingImageInquiries = new ConcurrentHashMap<>();

    private Long adminChatId = null;

    public TelegramBotService(TelegramConfig telegramConfig,
                              TelegramMessageStore messageStore,
                              TelegramAdminStore adminStore) {
        super(telegramConfig.getBot().getToken());
        this.telegramConfig = telegramConfig;
        this.messageStore   = messageStore;
        this.adminStore     = adminStore;
    }

    // ═══════════════════════════════════════════════════════════
    // STARTUP VALIDATION
    // ═══════════════════════════════════════════════════════════

    @PostConstruct
    public void validateConfig() {
        String channelId = telegramConfig.getChannelId();
        String token     = telegramConfig.getBot().getToken();
        String username  = telegramConfig.getBot().getUsername();

        boolean valid = true;

        if (token == null || token.isBlank()) {
            log.error("❌ telegram.bot.token is not set!");
            valid = false;
        }
        if (username == null || username.isBlank()) {
            log.error("❌ telegram.bot.username is not set!");
            valid = false;
        }
        if (channelId == null || channelId.isBlank()) {
            log.error("❌ telegram.channel.id is not set! Broadcasts will NOT work.");
            valid = false;
        }

        if (valid) {
            log.info("✅ Telegram configured — bot: @{} | channel: {}", username, channelId);
        }

        // Load persisted admin chatId from store on startup
        Long persistedAdminId = adminStore.getAdminChatId();
        if (persistedAdminId != null) {
            adminChatId = persistedAdminId;
            log.info("✅ Admin Telegram chatId loaded from store: {}", adminChatId);
        } else {
            log.warn("⚠️ No admin Telegram chatId registered yet. Call /api/admin/telegram/register first.");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BOT USERNAME
    // ═══════════════════════════════════════════════════════════

    @Override
    public String getBotUsername() {
        return telegramConfig.getBot().getUsername();
    }

    // ═══════════════════════════════════════════════════════════
    // INCOMING UPDATE HANDLER
    // ═══════════════════════════════════════════════════════════

    @Override
    public void onUpdateReceived(Update update) {

        // ── Inline button callback ──
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        Long    chatId  = message.getChatId();
        String  text    = message.hasText() ? message.getText() : "";

        // ── ADMIN ─────────────────────────────────────────────
        if (isAdmin(chatId)) {
            handleAdminMessage(message, chatId, text);
            return;
        }

        // ── USER ──────────────────────────────────────────────
        handleUserMessage(message, chatId, text);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN MESSAGE HANDLER
    // ═══════════════════════════════════════════════════════════

    private void handleAdminMessage(Message message, Long chatId, String text) {

        // Admin replying to a forwarded message
        if (message.isReply()) {
            handleAdminReply(message, chatId);
            return;
        }

        switch (text) {
            case "/start"      -> sendAdminWelcome(chatId);
            case "/stats"      -> sendStats(chatId);
            case "/help"       -> sendAdminHelp(chatId);
            case "/pending"    -> sendText(chatId, "📋 Fetching pending orders... (wire to OrderService)");
            case "/delivered"  -> sendText(chatId, "✅ Fetching delivered orders... (wire to OrderService)");
            case "/users"      -> sendText(chatId, "👥 Fetching user list... (wire to UserService)");
            case "/deliveries" -> sendText(chatId, "🚚 Fetching active deliveries... (wire to DeliveryService)");
            case "/inquiries"  -> sendPendingInquiries(chatId);
            default -> {
                if (text.startsWith("/broadcast ")) {
                    String broadcastMsg = text.substring("/broadcast ".length());
                    broadcastToChannel(broadcastMsg);
                    sendText(chatId, "✅ Broadcast sent to channel!");
                } else {
                    sendText(chatId, "ℹ️ Unknown command. Type /help for available commands.");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // USER MESSAGE HANDLER
    // ═══════════════════════════════════════════════════════════

    private void handleUserMessage(Message message, Long chatId, String text) {

        // ── User sent a PHOTO ─────────────────────────────────
        if (message.hasPhoto()) {
            handleUserImageInquiry(message, chatId);
            return;
        }

        // ── User sent a DOCUMENT that might be an image ───────
        if (message.hasDocument()) {
            String mimeType = message.getDocument().getMimeType();
            if (mimeType != null && mimeType.startsWith("image/")) {
                handleUserDocumentImageInquiry(message, chatId);
                return;
            }
        }

        // ── Standard commands ─────────────────────────────────
        switch (text) {
            case "/start" -> { sendUserWelcome(message, chatId); return; }
            case "/help"  -> { sendUserHelp(chatId); return; }
            case "/orders" -> {
                sendText(chatId, "📦 Visit our website to track your orders:\nhttps://esuosbeautyempires.vercel.app");
                return;
            }
            case "/track" -> {
                sendText(chatId, "🔍 Please reply with your tracking number to track your delivery.");
                return;
            }
            case "/inquiry" -> {
                sendText(chatId,
                        "🖼️ *Product Image Inquiry*\n\n" +
                                "Please send me a photo of the product you're looking for and I'll check if we have it in stock! 📦",
                        ParseMode.MARKDOWN);
                return;
            }
        }

        // ── User typed a message after sending an image ───────
        if (pendingImageInquiries.containsKey(chatId)) {
            handleInquiryFollowUpText(message, chatId, text);
            return;
        }

        // ── Auto-reply + forward to admin ─────────────────────
        sendText(chatId,
                "👋 Thanks for reaching out to *Esuo's Beauty Empire!* 🛍️\n\n" +
                        "We've received your message and our team will get back to you shortly.\n\n" +
                        "⏰ Our response time is usually within *1–2 hours*.\n\n" +
                        "Meanwhile you can:\n" +
                        "📦 /orders — Check your orders\n" +
                        "🖼️ /inquiry — Ask if a product is available by image\n" +
                        "🌐 Visit us: https://esuosbeautyempires.vercel.app",
                ParseMode.MARKDOWN);

        forwardUserMessageToAdmin(message, chatId);
    }

    // ═══════════════════════════════════════════════════════════
    // IMAGE INQUIRY
    // ═══════════════════════════════════════════════════════════

    private void handleUserImageInquiry(Message message, Long chatId) {
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize bestPhoto = photos.stream()
                .max((a, b) -> Integer.compare(a.getFileSize(), b.getFileSize()))
                .orElse(photos.get(photos.size() - 1));

        String fileId   = bestPhoto.getFileId();
        String userName = message.getFrom().getFirstName();

        pendingImageInquiries.put(chatId, fileId);

        sendText(chatId,
                "📸 Got your image, *" + userName + "*!\n\n" +
                        "What would you like to know about this product?\n\n" +
                        "You can ask things like:\n" +
                        "• _\"Is this available?\"_\n" +
                        "• _\"What sizes do you have?\"_\n" +
                        "• _\"What's the price?\"_\n" +
                        "• _\"Do you have this in red?\"_\n\n" +
                        "Just type your question below 👇",
                ParseMode.MARKDOWN);

        log.info("📸 User [{}] sent image inquiry, fileId: {}", chatId, fileId);
    }

    private void handleUserDocumentImageInquiry(Message message, Long chatId) {
        String fileId = message.getDocument().getFileId();
        pendingImageInquiries.put(chatId, fileId);

        sendText(chatId,
                "📸 Got your image!\n\n" +
                        "What would you like to know about this product? Just type your question below 👇",
                ParseMode.MARKDOWN);
    }

    private void handleInquiryFollowUpText(Message message, Long chatId, String text) {
        String fileId = pendingImageInquiries.remove(chatId);

        if (fileId == null) {
            forwardUserMessageToAdmin(message, chatId);
            return;
        }

        if (adminChatId == null) {
            sendText(chatId, "⚠️ Our team is currently unavailable. Please try again later.");
            return;
        }

        String userName = message.getFrom().getFirstName()
                + (message.getFrom().getLastName() != null ? " " + message.getFrom().getLastName() : "");

        String caption = "🖼️ *Product Image Inquiry*\n\n" +
                "👤 *From:* " + userName + "\n" +
                "🆔 *Chat ID:* `" + chatId + "`\n" +
                "❓ *Question:* " + text;

        try {
            SendPhoto photoMsg = SendPhoto.builder()
                    .chatId(adminChatId.toString())
                    .photo(new InputFile(fileId))
                    .caption(caption)
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup(buildReplyButton(chatId, userName))
                    .build();

            Message sentMessage = execute(photoMsg);
            messageStore.registerMessage(sentMessage.getMessageId(), chatId, userName);

            sendText(chatId,
                    "✅ Your inquiry has been sent to our team!\n\n" +
                            "We'll check if this product is available and get back to you soon. 😊\n\n" +
                            "⏰ Expected reply: within *1–2 hours*",
                    ParseMode.MARKDOWN);

            log.info("📨 Image inquiry from [{}] forwarded to admin", userName);

        } catch (TelegramApiException e) {
            log.error("❌ Failed to forward image inquiry to admin: {}", e.getMessage());
            sendText(chatId, "❌ Sorry, something went wrong. Please try again.");
        }
    }

    private void sendPendingInquiries(Long chatId) {
        if (pendingImageInquiries.isEmpty()) {
            sendText(chatId, "✅ No pending image inquiries right now.");
            return;
        }

        StringBuilder sb = new StringBuilder("🖼️ *Pending Image Inquiries*\n\n");
        sb.append("These users sent an image but haven't typed their question yet:\n\n");

        for (Long userChatId : pendingImageInquiries.keySet()) {
            String name = messageStore.getUserName(userChatId);
            sb.append("• ").append(name != null ? name : "Unknown")
                    .append(" (`").append(userChatId).append("`)\n");
        }

        sb.append("\n_They are waiting to type their question._");
        sendText(chatId, sb.toString(), ParseMode.MARKDOWN);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN REPLY → FORWARD BACK TO USER
    // ═══════════════════════════════════════════════════════════

    private void handleAdminReply(Message message, Long adminChatId) {
        Message repliedTo = message.getReplyToMessage();
        if (repliedTo == null) return;

        Integer originalMessageId = repliedTo.getMessageId();
        Long    userChatId        = messageStore.getUserChatId(originalMessageId);

        if (userChatId == null) {
            sendText(adminChatId, "⚠️ Could not find the original user for this message.");
            return;
        }

        String userName = messageStore.getUserName(userChatId);

        // Admin replied with a photo
        if (message.hasPhoto()) {
            handleAdminPhotoReply(message, userChatId, userName, adminChatId);
            return;
        }

        // Text reply
        String replyText = message.getText();
        sendText(userChatId,
                "💬 *Reply from Esuo's Beauty Empire:*\n\n" + replyText,
                ParseMode.MARKDOWN);

        sendText(adminChatId, "✅ Reply sent to *" + userName + "*", ParseMode.MARKDOWN);
        log.info("📨 Admin replied to user [chatId: {}]: {}", userChatId, replyText);
    }

    private void handleAdminPhotoReply(Message message, Long userChatId,
                                       String userName, Long adminChatId) {
        List<PhotoSize> photos = message.getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        String caption = message.getCaption() != null
                ? "💬 *Reply from Esuo's Beauty Empire:*\n\n" + message.getCaption()
                : "💬 *Reply from Esuo's Beauty Empire:*";

        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(userChatId.toString())
                    .photo(new InputFile(fileId))
                    .caption(caption)
                    .parseMode(ParseMode.MARKDOWN)
                    .build();
            execute(photo);
            sendText(adminChatId, "✅ Photo reply sent to *" + userName + "*", ParseMode.MARKDOWN);
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send photo reply to user: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FORWARD USER MESSAGE TO ADMIN
    // ═══════════════════════════════════════════════════════════

    private void forwardUserMessageToAdmin(Message message, Long userChatId) {
        if (adminChatId == null) {
            sendText(userChatId, "⚠️ Our team is currently unavailable. Please try again later.");
            return;
        }

        String userName = message.getFrom().getFirstName()
                + (message.getFrom().getLastName() != null ? " " + message.getFrom().getLastName() : "");

        String adminNotification = "📩 *New message from user:*\n"
                + "👤 *Name:* " + userName + "\n"
                + "🆔 *Chat ID:* `" + userChatId + "`\n"
                + "💬 *Message:* " + message.getText();

        try {
            SendMessage adminMsg = SendMessage.builder()
                    .chatId(adminChatId.toString())
                    .text(adminNotification)
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup(buildReplyButton(userChatId, userName))
                    .build();

            Message sentMessage = execute(adminMsg);
            messageStore.registerMessage(sentMessage.getMessageId(), userChatId, userName);

            log.info("📨 User [{}] message forwarded to admin", userName);

        } catch (TelegramApiException e) {
            log.error("❌ Failed to forward message to admin: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CALLBACK QUERY HANDLER
    // ═══════════════════════════════════════════════════════════

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data   = callbackQuery.getData();
        Long   chatId = callbackQuery.getMessage().getChatId();

        if (data.startsWith("dismiss_")) {
            Integer messageId = callbackQuery.getMessage().getMessageId();
            messageStore.clear(messageId);
            answerCallback(callbackQuery.getId(), "Message dismissed.");
        }

        log.info("🔘 Callback received: {} from chatId: {}", data, chatId);
    }

    // ═══════════════════════════════════════════════════════════
    // PRODUCT ANNOUNCEMENTS
    // imageUrl comes from Cloudinary (uploaded by the controller).
    // productId is auto-generated by the controller.
    // These methods simply format & post — no manual input needed.
    // ═══════════════════════════════════════════════════════════

    public void announceNewProduct(String name, String description, String price,
                                   String category, String subCategory,
                                   String sizes, String colors,
                                   String material, String style,
                                   String imageUrl, Long productId) {

        String caption = String.format("""
                ✨ *NEW ARRIVAL!*

                👗 *%s*
                💰 Price: *₵%s*

                📝 %s

                🏷 Category: %s
                👚 Sub-Category: %s
                📐 Sizes: %s
                🎨 Colors: %s
                🧵 Material: %s
                💃 Style: %s

                🛍️ Shop now: https://esuosbeautyempires.vercel.app/shop.html
                """,
                name, price, description != null ? description : "",
                category, subCategory != null ? subCategory : "N/A",
                sizes, colors,
                material != null ? material : "N/A",
                style != null ? style : "N/A",
                productId);

        if (imageUrl != null && !imageUrl.isBlank()) {
            sendPhotoToChannel(imageUrl, caption);
        } else {
            sendToChannel(caption);
        }

        log.info("📢 Clothing product [{}] announced on Telegram channel (ID: {})", name, productId);
    }

    public void announceNewGeneralProduct(String name, String description, String price,
                                          String category, String subCategory,
                                          String tags, String imageUrl, Long productId) {

        String caption = String.format("""
                🛒 *NEW PRODUCT!*

                📦 *%s*
                💰 Price: *₵%s*

                📝 %s

                🏷 Category: %s
                📂 Sub-Category: %s
                🔖 Tags: %s

                🛍️ View product: https://esuosbeautyempires.vercel.app/shop.html
                """,
                name, price, description != null ? description : "",
                category, subCategory != null ? subCategory : "N/A",
                tags != null ? tags : "N/A",
                productId);

        if (imageUrl != null && !imageUrl.isBlank()) {
            sendPhotoToChannel(imageUrl, caption);
        } else {
            sendToChannel(caption);
        }

        log.info("📢 General product [{}] announced on Telegram channel (ID: {})", name, productId);
    }

    // ═══════════════════════════════════════════════════════════
    // ORDER & DELIVERY NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════

    public void notifyOrderStatusUpdate(String customerName, Long orderId,
                                        String oldStatus, String newStatus) {
        if (adminChatId == null) {
            log.warn("⚠️ Cannot send order notification — no admin chatId registered.");
            return;
        }
        String msg = String.format("""
                📦 *Order Status Update*

                👤 Customer: *%s*
                🔢 Order ID: *#%d*
                📌 Previous: %s
                ✅ New Status: *%s*
                """, customerName, orderId, oldStatus, newStatus);
        sendText(adminChatId, msg, ParseMode.MARKDOWN);
        log.info("📦 Order status notification sent for order #{}", orderId);
    }

    public void notifyNewDeliveryRequest(String customerName, Long deliveryId,
                                         String address, String city) {
        if (adminChatId == null) {
            log.warn("⚠️ Cannot send delivery notification — no admin chatId registered.");
            return;
        }
        String msg = String.format("""
                🚚 *New Delivery Request!*

                👤 Customer: *%s*
                🔢 Delivery ID: *#%d*
                📍 Address: %s
                🏙 City: %s

                Reply /deliveries to view all active deliveries.
                """, customerName, deliveryId, address, city != null ? city : "N/A");
        sendText(adminChatId, msg, ParseMode.MARKDOWN);
        log.info("🚚 Delivery request notification sent for delivery #{}", deliveryId);
    }

    public void notifyDeliveryStatusUpdate(String customerName, Long deliveryId,
                                           String newStatus, String trackingNumber) {
        if (adminChatId == null) {
            log.warn("⚠️ Cannot send delivery status notification — no admin chatId registered.");
            return;
        }
        String msg = String.format("""
                📦 *Delivery Update*

                👤 Customer: *%s*
                🔢 Delivery ID: *#%d*
                ✅ New Status: *%s*
                🔍 Tracking: %s
                """, customerName, deliveryId, newStatus,
                trackingNumber != null ? trackingNumber : "N/A");
        sendText(adminChatId, msg, ParseMode.MARKDOWN);
        log.info("📦 Delivery status notification sent for delivery #{}", deliveryId);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN REGISTRATION
    // ═══════════════════════════════════════════════════════════

    public void registerAdmin(Long chatId, String secret) {
        String expectedSecret = "OPATA_ADMIN_2024"; // move to application.properties

        if (secret.equals(expectedSecret)) {
            adminChatId = chatId;
            adminStore.setAdminChatId(chatId);
            sendText(chatId, "✅ Admin registered successfully! You'll now receive all notifications here.");
            log.info("✅ Admin registered with chatId: {}", chatId);
        } else {
            sendText(chatId, "❌ Invalid secret. Access denied.");
            log.warn("⚠️ Failed admin registration attempt from chatId: {}", chatId);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BROADCAST
    // ═══════════════════════════════════════════════════════════

    public void broadcastToChannel(String messageText) {
        sendToChannel("📢 *Announcement from Esuo's Beauty Empire*\n\n" + messageText);
        log.info("📢 Broadcast sent to channel");
    }

    public void broadcastSaleToChannel(String saleName, String discount, String endDate) {
        String msg = String.format("""
                🔥 *SALE ALERT!*

                🎉 *%s*
                💥 Get up to *%s OFF!*
                ⏰ Ends: %s

                🛍️ Shop now: https://esuosbeautyempires.vercel.app
                """, saleName, discount, endDate);
        sendToChannel(msg);
        log.info("🔥 Sale announcement sent: {} ({})", saleName, discount);
    }

    // ═══════════════════════════════════════════════════════════
    // WELCOME & HELP
    // ═══════════════════════════════════════════════════════════

    private void sendAdminWelcome(Long chatId) {
        String msg = """
                👋 Welcome back, *Admin!*

                Here are your available commands:

                📦 *Orders*
                /pending — View pending orders
                /delivered — View delivered orders

                🚚 *Deliveries*
                /deliveries — View active deliveries

                👥 *Users*
                /users — View registered users

                📊 *Stats*
                /stats — Dashboard summary

                🖼️ *Inquiries*
                /inquiries — View pending image inquiries

                📢 *Channel*
                /broadcast <message> — Send message to channel

                💬 *Support*
                Reply to any forwarded user message to respond directly.
                You can also reply with a *photo* to show a product to the user.
                """;
        sendText(chatId, msg, ParseMode.MARKDOWN);
    }

    private void sendAdminHelp(Long chatId) {
        sendAdminWelcome(chatId);
    }

    private void sendUserWelcome(Message message, Long chatId) {
        String firstName = message.getFrom().getFirstName();
        String msg = String.format("""
                👋 Hi *%s*! Welcome to *Esuo's Beauty Empire* 🛍️

                Here's what I can do for you:

                📦 /orders — Check your orders
                🔍 /track — Track your delivery
                🖼️ /inquiry — Ask if a product is available by image
                ❓ /help — Show this menu

                💬 Or just type a message and our team will reply to you directly!
                """, firstName);
        sendText(chatId, msg, ParseMode.MARKDOWN);
    }

    private void sendUserHelp(Long chatId) {
        String msg = """
                ❓ *Help Menu*

                📦 /orders — View your orders on our website
                🔍 /track — Track your delivery
                🖼️ /inquiry — Send a product image to ask if it's available
                💬 Send a message — Our support team will reply

                🌐 Website: https://esuosbeautyempires.vercel.app
                """;
        sendText(chatId, msg, ParseMode.MARKDOWN);
    }

    private void sendStats(Long chatId) {
        String msg = """
                📊 *Dashboard Stats*

                Wire this to your services for live data:
                • AdminOrderService.getOrderSummary()
                • UserService.getAllUsers()
                • DeliveryService.getActiveDeliveries()
                """;
        sendText(chatId, msg, ParseMode.MARKDOWN);
    }

    // ═══════════════════════════════════════════════════════════
    // SEND HELPERS
    // ═══════════════════════════════════════════════════════════

    public void sendToChannel(String text) {
        String channelId = telegramConfig.getChannelId();
        if (channelId == null || channelId.isBlank()) {
            log.error("❌ Cannot send to channel: telegram.channel.id is not configured in application.properties!");
            return;
        }
        sendText(channelId, text, ParseMode.MARKDOWN);
    }

    public void sendPhotoToChannel(String imageUrl, String caption) {
        String channelId = telegramConfig.getChannelId();
        if (channelId == null || channelId.isBlank()) {
            log.error("❌ Cannot send photo to channel: telegram.channel.id is not configured!");
            return;
        }
        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(channelId)
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .parseMode(ParseMode.MARKDOWN)
                    .build();
            execute(photo);
            log.info("📸 Photo sent to channel successfully");
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send photo to channel: {}", e.getMessage());
        }
    }

    public void sendText(Long chatId, String text) {
        sendText(chatId.toString(), text, null);
    }

    public void sendText(Long chatId, String text, String parseMode) {
        sendText(chatId.toString(), text, parseMode);
    }

    public void sendText(String chatId, String text, String parseMode) {
        if (chatId == null || chatId.isBlank()) {
            log.error("❌ Cannot send message: chatId is null or blank. Text was: {}", text);
            return;
        }
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                    .chatId(chatId)
                    .text(text);
            if (parseMode != null) builder.parseMode(parseMode);
            execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send message to {}: {}", chatId, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private void answerCallback(String callbackId, String text) {
        try {
            execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("❌ Failed to answer callback: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup buildReplyButton(Long userChatId, String userName) {
        InlineKeyboardButton replyBtn = InlineKeyboardButton.builder()
                .text("💬 Reply to " + userName)
                .callbackData("reply_" + userChatId)
                .build();

        InlineKeyboardButton dismissBtn = InlineKeyboardButton.builder()
                .text("❌ Dismiss")
                .callbackData("dismiss_" + userChatId)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(replyBtn, dismissBtn))
                .build();
    }

    private boolean isAdmin(Long chatId) {
        return chatId.equals(adminChatId) || chatId.equals(adminStore.getAdminChatId());
    }
}