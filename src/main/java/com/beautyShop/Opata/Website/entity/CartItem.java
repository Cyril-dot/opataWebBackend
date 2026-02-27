package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Linked to the customer who owns this cart item ──────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── The product added to cart ───────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    // Price snapshot when item was added to cart
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Computed: quantity * unitPrice
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
        computeSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        computeSubtotal();
    }

    // Auto-compute subtotal whenever quantity or price changes
    public void computeSubtotal() {
        if (unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}