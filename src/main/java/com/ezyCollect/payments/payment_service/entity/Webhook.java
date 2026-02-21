package com.ezyCollect.payments.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Webhook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url",nullable = false, length = 255)
    private String url;

    @Column(name = "active",nullable = false)
    private boolean active; // only active webhooks are called

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
