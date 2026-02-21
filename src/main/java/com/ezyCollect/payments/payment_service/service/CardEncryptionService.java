package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.util.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
public class CardEncryptionService {
    private final SecretKey secretKey;

    public CardEncryptionService(@Value("${aes.secret.key}") String secretKeyString) {
        this.secretKey = AESUtil.decodeKeyFromBase64(secretKeyString);
    }

    public EncryptedCardInfo encryptCard(String cardNumber) throws EncryptionException {
        byte[] iv = AESUtil.generateRandomIV();
        String encryptedCard = AESUtil.encrypt(cardNumber, secretKey, iv);
        String ivBase64 = Base64.getEncoder().encodeToString(iv);

        return new EncryptedCardInfo(encryptedCard, ivBase64);
    }
}
