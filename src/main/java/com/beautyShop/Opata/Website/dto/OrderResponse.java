package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String customerName;
    private String customerEmail;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private String status;
    private String deliveryAddress;
    private LocalDateTime createdAt;
}