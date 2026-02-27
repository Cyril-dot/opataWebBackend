package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository     chatRoomRepository;
    private final ChatMessageRepository  chatMessageRepository;
    private final ProductRepository      productRepository;
    private final OrderRepository        orderRepository;
    private final DeliveryRepository     deliveryRepository;
    private final UserRepo               userRepository;
    private final AdminRepo              adminRepository;
    private final EmailService           emailService;
    private final CloudinaryService      cloudinaryService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ═══════════════════════════════════════════════════════════
    // START CHAT — USER initiates about a PRODUCT
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ChatRoomResponse startProductChat(UUID userId, StartChatRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShopOwner admin = product.getAddedBy();

        return chatRoomRepository.findByUserIdAndProductId(userId, request.getProductId())
                .map(existing -> {
                    System.out.println("💬 Existing product chat room found: #" + existing.getId());
                    return mapRoomToResponse(existing);
                })
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .title("Enquiry: " + product.getName())
                            .product(product)
                            .user(user)
                            .shopOwner(admin)
                            .roomType(ChatRoomType.PRODUCT)
                            .build();

                    ChatRoom savedRoom = chatRoomRepository.save(room);

                    chatMessageRepository.save(ChatMessage.builder()
                            .chatRoom(savedRoom)
                            .senderType(SenderType.USER)
                            .senderId(userId.toString())
                            .senderName(user.getFirstName() + " " + user.getLastName())
                            .content(buildProductCardContent(product))
                            .messageType(MessageType.TEXT)
                            .isProductCard(true)
                            .build());

                    System.out.println("💬 Product chat created: #" + savedRoom.getId()
                            + " | Product: " + product.getName());
                    return mapRoomToResponse(savedRoom);
                });
    }

    // ═══════════════════════════════════════════════════════════
    // START CHAT — ADMIN initiates about an ORDER
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ChatRoomResponse startOrderChat(UUID adminId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ShopOwner admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        User user = order.getUser();

        return chatRoomRepository.findByUserIdAndLinkedOrderId(user.getId(), orderId)
                .map(existing -> {
                    System.out.println("💬 Existing order chat room found: #" + existing.getId());
                    return mapRoomToResponse(existing);
                })
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .title("Order #" + orderId + " Update")
                            .linkedOrder(order)
                            .user(user)
                            .shopOwner(admin)
                            .roomType(ChatRoomType.ORDER)
                            .build();

                    ChatRoom savedRoom = chatRoomRepository.save(room);

                    chatMessageRepository.save(ChatMessage.builder()
                            .chatRoom(savedRoom)
                            .senderType(SenderType.ADMIN)
                            .senderId(adminId.toString())
                            .senderName(admin.getName())
                            .content(buildOrderCardContent(order))
                            .messageType(MessageType.TEXT)
                            .isOrderCard(true)
                            .linkedOrder(order)
                            .build());

                    emailService.notifyUserOfOrderChat(
                            user.getEmail(), user.getFirstName(),
                            admin.getName(), orderId, savedRoom.getId());

                    System.out.println("💬 Order chat created: #" + savedRoom.getId()
                            + " | Order: #" + orderId + " | User: " + user.getEmail());
                    return mapRoomToResponse(savedRoom);
                });
    }

    // ═══════════════════════════════════════════════════════════
    // START CHAT — ADMIN initiates about a DELIVERY
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ChatRoomResponse startDeliveryChat(UUID adminId, Long deliveryId) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        ShopOwner admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        User user = delivery.getUser();

        return chatRoomRepository.findByLinkedDeliveryId(deliveryId)
                .map(existing -> {
                    System.out.println("💬 Existing delivery chat room found: #" + existing.getId());
                    return mapRoomToResponse(existing);
                })
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .title("Delivery #" + deliveryId + " Update")
                            .linkedDelivery(delivery)
                            .user(user)
                            .shopOwner(admin)
                            .roomType(ChatRoomType.DELIVERY)
                            .build();

                    ChatRoom savedRoom = chatRoomRepository.save(room);

                    chatMessageRepository.save(ChatMessage.builder()
                            .chatRoom(savedRoom)
                            .senderType(SenderType.ADMIN)
                            .senderId(adminId.toString())
                            .senderName(admin.getName())
                            .content(buildDeliveryCardContent(delivery))
                            .messageType(MessageType.TEXT)
                            .isProductCard(false)
                            .isOrderCard(false)
                            .build());

                    emailService.notifyUserOfDeliveryChat(
                            user.getEmail(), user.getFirstName(),
                            admin.getName(), deliveryId, savedRoom.getId());

                    System.out.println("💬 Delivery chat created: #" + savedRoom.getId()
                            + " | Delivery: #" + deliveryId + " | User: " + user.getEmail());
                    return mapRoomToResponse(savedRoom);
                });
    }

    // ═══════════════════════════════════════════════════════════
    // USER SENDS A MESSAGE (text, image, or video)
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ChatMessageResponse userSendMessage(UUID userId,
                                               Long chatRoomId,
                                               SendMessageRequest request,
                                               MultipartFile mediaFile) throws IOException {

        ChatRoom room = findRoomById(chatRoomId);

        if (!room.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This chat does not belong to you");
        }

        User user = room.getUser();
        MessageType type = resolveMessageType(request.getMessageType());
        String[] media = uploadMedia(mediaFile, type);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderType(SenderType.USER)
                .senderId(userId.toString())
                .senderName(user.getFirstName() + " " + user.getLastName())
                .content(request.getContent())
                .messageType(type)
                .mediaUrl(media[0])
                .mediaPublicId(media[1])
                .isProductCard(false)
                .isOrderCard(false)
                .build());

        ChatMessageResponse response = mapMessageToResponse(saved);

        messagingTemplate.convertAndSend("/topic/admin/chat/" + chatRoomId, response);

        emailService.notifyAdminOfUserMessage(
                room.getShopOwner().getEmail(),
                room.getShopOwner().getName(),
                user.getFirstName() + " " + user.getLastName(),
                room.getTitle(),
                type == MessageType.TEXT ? request.getContent() : "📎 Sent a " + type.name().toLowerCase(),
                chatRoomId);

        System.out.println("📨 User [" + user.getEmail() + "] sent " + type.name() + " in chat #" + chatRoomId);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN SENDS A MESSAGE (text, image, or video)
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ChatMessageResponse adminSendMessage(UUID adminId,
                                                Long chatRoomId,
                                                SendMessageRequest request,
                                                MultipartFile mediaFile) throws IOException {

        ChatRoom room = findRoomById(chatRoomId);
        ShopOwner admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        MessageType type = resolveMessageType(request.getMessageType());
        String[] media = uploadMedia(mediaFile, type);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderType(SenderType.ADMIN)
                .senderId(adminId.toString())
                .senderName(admin.getName())
                .content(request.getContent())
                .messageType(type)
                .mediaUrl(media[0])
                .mediaPublicId(media[1])
                .isProductCard(false)
                .isOrderCard(false)
                .build());

        ChatMessageResponse response = mapMessageToResponse(saved);

        messagingTemplate.convertAndSend("/topic/user/chat/" + chatRoomId, response);

        User user = room.getUser();
        emailService.notifyUserOfAdminReply(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                admin.getName(),
                room.getTitle(),
                type == MessageType.TEXT ? request.getContent() : "📎 Sent a " + type.name().toLowerCase(),
                chatRoomId);

        System.out.println("📨 Admin [" + admin.getName() + "] sent " + type.name() + " in chat #" + chatRoomId);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE A MESSAGE
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public void deleteMessage(Long messageId, UUID requesterId) throws IOException {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(requesterId.toString())) {
            throw new RuntimeException("Unauthorized: You can only delete your own messages");
        }

        if (message.getMediaPublicId() != null) {
            cloudinaryService.deleteImage(message.getMediaPublicId());
        }

        chatMessageRepository.delete(message);
        System.out.println("🗑️  Message #" + messageId + " deleted by: " + requesterId);
    }

    // ═══════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════

    public ChatRoomResponse getChatRoomById(Long chatRoomId) {
        return mapRoomToResponse(findRoomById(chatRoomId));
    }

    public List<ChatMessageResponse> getChatHistory(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId)
                .stream().map(this::mapMessageToResponse).collect(Collectors.toList());
    }

    // ── User chat room queries ────────────────────────────────
    public List<ChatRoomResponse> getUserChatRooms(UUID userId) {
        return chatRoomRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getUserProductChats(UUID userId) {
        return chatRoomRepository.findByUserIdAndRoomTypeOrderByCreatedAtDesc(userId, ChatRoomType.PRODUCT)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getUserOrderChats(UUID userId) {
        return chatRoomRepository.findByUserIdAndRoomTypeOrderByCreatedAtDesc(userId, ChatRoomType.ORDER)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getUserDeliveryChats(UUID userId) {
        return chatRoomRepository.findByUserIdAndRoomTypeOrderByCreatedAtDesc(userId, ChatRoomType.DELIVERY)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    // ── Admin chat room queries ───────────────────────────────
    public List<ChatRoomResponse> getAdminChatRooms(UUID adminId) {
        return chatRoomRepository.findByShopOwnerIdOrderByCreatedAtDesc(adminId)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getAdminOrderChats(UUID adminId) {
        return chatRoomRepository.findByShopOwnerIdAndRoomTypeOrderByCreatedAtDesc(adminId, ChatRoomType.ORDER)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getAdminDeliveryChats(UUID adminId) {
        return chatRoomRepository.findByShopOwnerIdAndRoomTypeOrderByCreatedAtDesc(adminId, ChatRoomType.DELIVERY)
                .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Uploads media file to Cloudinary. Returns [mediaUrl, mediaPublicId] or [null, null]. */
    private String[] uploadMedia(MultipartFile file, MessageType type) throws IOException {
        if (file == null || file.isEmpty()) return new String[]{null, null};
        String folder = type == MessageType.VIDEO
                ? "beautyShop/chat/videos"
                : "beautyShop/chat/images";
        Map<String, Object> result = cloudinaryService.uploadImage(file, folder);
        return new String[]{
                (String) result.get("secure_url"),
                (String) result.get("public_id")
        };
    }

    private MessageType resolveMessageType(String type) {
        if (type == null || type.isBlank()) return MessageType.TEXT;
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }

    private ChatRoom findRoomById(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chat room not found with id: " + id));
    }

    // ── Single mapRoomToResponse — handles ALL room types ────
    private ChatRoomResponse mapRoomToResponse(ChatRoom room) {
        ChatRoomResponse.ChatRoomResponseBuilder builder = ChatRoomResponse.builder()
                .chatRoomId(room.getId())
                .title(room.getTitle())
                .roomType(room.getRoomType().name())
                .customerName(room.getUser().getFirstName() + " " + room.getUser().getLastName())
                .customerEmail(room.getUser().getEmail())
                .adminName(room.getShopOwner().getName())
                .createdAt(room.getCreatedAt());

        switch (room.getRoomType()) {
            case PRODUCT -> {
                if (room.getProduct() != null) {
                    builder.productId(room.getProduct().getId())
                            .productName(room.getProduct().getName())
                            .productImage(room.getProduct().getPrimaryImageUrl())
                            .productPrice("₵" + room.getProduct().getPrice());
                }
            }
            case ORDER -> {
                if (room.getLinkedOrder() != null) {
                    builder.orderId(room.getLinkedOrder().getId())
                            .orderStatus(room.getLinkedOrder().getStatus().name())
                            .orderTotal("₵" + room.getLinkedOrder().getTotalAmount());
                }
            }
            case DELIVERY -> {
                if (room.getLinkedDelivery() != null) {
                    builder.deliveryId(room.getLinkedDelivery().getId())
                            .deliveryStatus(room.getLinkedDelivery().getStatus().name())
                            .deliveryAddress(room.getLinkedDelivery().getDeliveryAddress());
                }
            }
        }

        return builder.build();
    }

    private ChatMessageResponse mapMessageToResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .messageId(msg.getId())
                .chatRoomId(msg.getChatRoom().getId())
                .senderType(msg.getSenderType().name())
                .senderName(msg.getSenderName())
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .mediaUrl(msg.getMediaUrl())
                .isProductCard(msg.isProductCard())
                .isOrderCard(msg.isOrderCard())
                .linkedOrderId(msg.getLinkedOrder() != null ? msg.getLinkedOrder().getId() : null)
                .sentAt(msg.getSentAt())
                .build();
    }

    // ── Card content builders ─────────────────────────────────
    private String buildProductCardContent(Product product) {
        return String.format("PRODUCT_CARD::%s::%s::%s::%s",
                product.getName(), product.getPrice(),
                product.getDescription() != null ? product.getDescription() : "No description",
                product.getPrimaryImageUrl() != null ? product.getPrimaryImageUrl() : "");
    }

    private String buildOrderCardContent(Order order) {
        return String.format("ORDER_CARD::#%d::%s::%s::%s",
                order.getId(), order.getStatus().name(), order.getTotalAmount(),
                order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "N/A");
    }

    private String buildDeliveryCardContent(Delivery delivery) {
        return String.format("DELIVERY_CARD::#%d::%s::%s::%s::%s",
                delivery.getId(), delivery.getStatus().name(),
                delivery.getDeliveryAddress(), delivery.getRecipientName(),
                delivery.getTrackingNumber() != null ? delivery.getTrackingNumber() : "Not yet assigned");
    }
}