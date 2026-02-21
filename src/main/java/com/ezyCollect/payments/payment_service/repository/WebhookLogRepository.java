package com.ezyCollect.payments.payment_service.repository;

import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
}
