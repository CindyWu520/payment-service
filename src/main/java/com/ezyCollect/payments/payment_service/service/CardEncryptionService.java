package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Slf4j
@Service
public class CardEncryptionService {
    private final SecretKey secretKey;

    public CardEncryptionService(@Value("${aes.secret.key}") String secretKeyString) {
        if (secretKeyString == null || secretKeyString.isBlank()) {
            throw new IllegalStateException("AES secret key must not be blank");
        }
        this.secretKey = AESUtil.decodeKeyFromBase64(secretKeyString);
        log.info("CardEncryptionService initialized successfully");
    }

    public EncryptedCardInfo encryptCard(String cardNumber) {
        try {
            byte[] iv = AESUtil.generateRandomIV();
            String encryptedCard = AESUtil.encrypt(cardNumber, secretKey, iv);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            return new EncryptedCardInfo(encryptedCard, ivBase64);
        } catch (Exception e) {
            log.error("Failed to encrypt card number: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt card number", e);
        }
    }
}
