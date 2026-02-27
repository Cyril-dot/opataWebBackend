package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "shopOwner")
@Builder
@Getter
@Setter
public class ShopOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String email;
    private String phone;
    private String shopAddress;
    private String shopName;
    private String password;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.ADMIN;

    // Products added by this admin
    @OneToMany(mappedBy = "addedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;
}