package com.ezyCollect.payments.payment_service.entity;

import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "webhook_log",
        indexes = {
                @Index(name = "idx_webhook_id", columnList = "webhook_id"),
                @Index(name = "idx_event_status", columnList = "event_status"),
                @Index(name = "idx_direction", columnList = "direction")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id")
    private Long webhookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private WebhookDirection direction;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Lob
    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private WebhookEventStatus eventStatus;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    // only set when sending
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // only set when receiving
    @Column(name = "receive_at")
    private LocalDateTime receiveAt;
}
