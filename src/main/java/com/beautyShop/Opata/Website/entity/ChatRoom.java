package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Product this chat is about (nullable for order-only chats)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Order this chat is about (nullable for product-only chats)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order linkedOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_owner_id", nullable = false)
    private ShopOwner shopOwner;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    // PRODUCT = started by user about a product
    // ORDER   = started by admin about an order
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChatRoomType roomType = ChatRoomType.PRODUCT;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Add to ChatRoom entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery linkedDelivery;

}