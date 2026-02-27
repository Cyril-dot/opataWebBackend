package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository    orderRepository;
    private final UserRepo           userRepository;
    private final AdminRepo          adminRepository;
    private final ChatService        chatService;
    private final EmailService       emailService;

    // ═══════════════════════════════════════════════════════════
    // USER — REQUEST A DELIVERY
    // ═══════════════════════════════════════════════════════════

    /**
     * User submits a delivery request for one of their orders.
     * Only one delivery request allowed per order.
     */
    @Transactional
    public DeliveryResponse requestDelivery(UUID userId, DeliveryRequest request) {

        // Validate order belongs to this user
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This order does not belong to you");
        }

        // Block duplicate delivery requests for the same order
        if (deliveryRepository.existsByOrderId(request.getOrderId())) {
            throw new RuntimeException("A delivery request already exists for this order");
        }

        // Block delivery request on cancelled orders
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot request delivery for a cancelled order");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Assign to the admin who owns the product (from the first order item)
        ShopOwner assignedAdmin = order.getOrderItems()
                .get(0).getProduct().getAddedBy();

        Delivery delivery = Delivery.builder()
                .order(order)
                .user(user)
                .assignedAdmin(assignedAdmin)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .city(request.getCity())
                .region(request.getRegion())
                .country(request.getCountry())
                .deliveryNotes(request.getDeliveryNotes())
                .status(DeliveryStatus.REQUESTED)
                .build();

        Delivery saved = deliveryRepository.save(delivery);

        // Email admin about the new delivery request
        emailService.notifyAdminOfDeliveryRequest(
                assignedAdmin.getEmail(),
                assignedAdmin.getName(),
                user.getFirstName() + " " + user.getLastName(),
                saved.getId(),
                request.getDeliveryAddress()
        );

        System.out.println("🚚 Delivery requested: #" + saved.getId()
                + " | Order: #" + order.getId()
                + " | User: " + user.getEmail());

        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN — UPDATE DELIVERY STATUS
    // ═══════════════════════════════════════════════════════════

    /**
     * Admin updates the delivery status.
     * Optionally sends a message to the user via chat when status changes.
     */
    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long deliveryId,
                                                  UUID adminId,
                                                  DeliveryStatusUpdateRequest request) {

        Delivery delivery = findDeliveryById(deliveryId);
        DeliveryStatus oldStatus = delivery.getStatus();

        if (oldStatus == DeliveryStatus.DELIVERED) {
            throw new RuntimeException("Delivery already completed.");
        }
        if (oldStatus == DeliveryStatus.CANCELLED) {
            throw new RuntimeException("Cannot update a cancelled delivery.");
        }

        // Update fields
        delivery.setStatus(request.getStatus());

        if (request.getTrackingNumber()       != null) delivery.setTrackingNumber(request.getTrackingNumber());
        if (request.getCourierName()          != null) delivery.setCourierName(request.getCourierName());
        if (request.getDeliveryFee()          != null) delivery.setDeliveryFee(request.getDeliveryFee());
        if (request.getEstimatedDeliveryTime()!= null) delivery.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());

        // Mark actual delivery time when status is DELIVERED
        if (request.getStatus() == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryTime(LocalDateTime.now());
        }

        Delivery updated = deliveryRepository.save(delivery);

        System.out.println("📦 Delivery #" + deliveryId + " status: "
                + oldStatus + " → " + request.getStatus());

        // If admin included a message, send it via the delivery chat
        if (request.getMessageToUser() != null && !request.getMessageToUser().isBlank()) {
            openOrSendDeliveryChat(adminId, updated, request.getMessageToUser());
        }

        // Email user about status change
        emailService.notifyUserOfDeliveryStatusUpdate(
                delivery.getUser().getEmail(),
                delivery.getUser().getFirstName(),
                deliveryId,
                oldStatus.name(),
                request.getStatus().name(),
                delivery.getTrackingNumber()
        );

        return mapToResponse(updated);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN — OPEN / GET DELIVERY CHAT
    // ═══════════════════════════════════════════════════════════

    /**
     * Admin opens a chat room linked to a delivery.
     * If a chat room already exists for this delivery, it is returned.
     * The user is notified by email that the admin has reached out.
     */
    @Transactional
    public ChatRoomResponse openDeliveryChat(UUID adminId, Long deliveryId) {
        Delivery delivery = findDeliveryById(deliveryId);
        return openOrSendDeliveryChat(adminId, delivery, null);
    }

    // ═══════════════════════════════════════════════════════════
    // READ — USER
    // ═══════════════════════════════════════════════════════════

    // All deliveries for the logged-in user
    public List<DeliveryResponse> getMyDeliveries(UUID userId) {
        return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Single delivery by ID (user can only see their own)
    public DeliveryResponse getMyDeliveryById(UUID userId, Long deliveryId) {
        Delivery delivery = findDeliveryById(deliveryId);
        if (!delivery.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This delivery does not belong to you");
        }
        return mapToResponse(delivery);
    }

    // Track by tracking number (public — no auth needed)
    public DeliveryResponse trackDelivery(String trackingNumber) {
        Delivery delivery = deliveryRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("No delivery found with tracking number: " + trackingNumber));
        return mapToResponse(delivery);
    }

    // ═══════════════════════════════════════════════════════════
    // READ — ADMIN
    // ═══════════════════════════════════════════════════════════

    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DeliveryResponse> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DeliveryResponse> getActiveDeliveries() {
        return deliveryRepository.findActiveDeliveries()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DeliveryResponse> getOverdueDeliveries() {
        return deliveryRepository.findOverdueDeliveries(LocalDateTime.now())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DeliveryResponse> getMyAssignedDeliveries(UUID adminId) {
        return deliveryRepository.findByAssignedAdminIdOrderByCreatedAtDesc(adminId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public DeliveryResponse getDeliveryById(Long deliveryId) {
        return mapToResponse(findDeliveryById(deliveryId));
    }

    public List<DeliveryResponse> getDeliveriesForOrder(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .map(d -> List.of(mapToResponse(d)))
                .orElse(List.of());
    }

    // ═══════════════════════════════════════════════════════════
    // CANCEL DELIVERY — USER OR ADMIN
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public DeliveryResponse cancelDelivery(Long deliveryId, UUID requesterId) {
        Delivery delivery = findDeliveryById(deliveryId);

        // Only the user who owns it or the assigned admin can cancel
        boolean isOwner = delivery.getUser().getId().equals(requesterId);
        boolean isAdmin = delivery.getAssignedAdmin().getId().equals(requesterId);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized: You cannot cancel this delivery");
        }
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel an already delivered order");
        }
        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new RuntimeException("Delivery is already cancelled");
        }
        // Block cancellation once picked up
        if (delivery.getStatus() == DeliveryStatus.PICKED_UP
                || delivery.getStatus() == DeliveryStatus.IN_TRANSIT
                || delivery.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new RuntimeException("Cannot cancel a delivery that is already in transit");
        }

        delivery.setStatus(DeliveryStatus.CANCELLED);
        deliveryRepository.save(delivery);

        System.out.println("❌ Delivery #" + deliveryId + " cancelled by: " + requesterId);
        return mapToResponse(delivery);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Opens a delivery chat room (or retrieves existing one)
     * and optionally sends an initial message from the admin.
     */
    private ChatRoomResponse openOrSendDeliveryChat(UUID adminId,
                                                      Delivery delivery,
                                                      String initialMessage) {

        // If chat room already exists for this delivery, reuse it
        if (delivery.getChatRoom() != null) {
            // Send message into existing room if provided
            if (initialMessage != null && !initialMessage.isBlank()) {
                SendMessageRequest msg = new SendMessageRequest();
                msg.setContent(initialMessage);
                msg.setMessageType("TEXT");
                try {
                    chatService.adminSendMessage(adminId, delivery.getChatRoom().getId(), msg, null);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to send message in existing delivery chat: " + e.getMessage());
                }
            }
            return chatService.getChatRoomById(delivery.getChatRoom().getId());
        }

        // Create new delivery chat room via ChatService
        ChatRoomResponse chatRoom = chatService.startDeliveryChat(adminId, delivery.getId());

        // Link the chat room back to the delivery
        ChatRoom room = new ChatRoom();
        room.setId(chatRoom.getChatRoomId());
        delivery.setChatRoom(room);
        deliveryRepository.save(delivery);

        // Send initial message if provided
        if (initialMessage != null && !initialMessage.isBlank()) {
            SendMessageRequest msg = new SendMessageRequest();
            msg.setContent(initialMessage);
            msg.setMessageType("TEXT");
            try {
                chatService.adminSendMessage(adminId, chatRoom.getChatRoomId(), msg, null);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to send initial delivery chat message: " + e.getMessage());
            }
        }

        return chatRoom;
    }

    private Delivery findDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
    }

    private DeliveryResponse mapToResponse(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .orderId(d.getOrder().getId())
                .orderStatus(d.getOrder().getStatus().name())
                .orderTotal(d.getOrder().getTotalAmount())
                .customerName(d.getUser().getFirstName() + " " + d.getUser().getLastName())
                .customerEmail(d.getUser().getEmail())
                .recipientName(d.getRecipientName())
                .recipientPhone(d.getRecipientPhone())
                .deliveryAddress(d.getDeliveryAddress())
                .city(d.getCity())
                .region(d.getRegion())
                .country(d.getCountry())
                .deliveryNotes(d.getDeliveryNotes())
                .status(d.getStatus())
                .trackingNumber(d.getTrackingNumber())
                .courierName(d.getCourierName())
                .deliveryFee(d.getDeliveryFee())
                .estimatedDeliveryTime(d.getEstimatedDeliveryTime())
                .actualDeliveryTime(d.getActualDeliveryTime())
                .chatRoomId(d.getChatRoom() != null ? d.getChatRoom().getId() : null)
                .assignedAdminName(d.getAssignedAdmin().getName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}