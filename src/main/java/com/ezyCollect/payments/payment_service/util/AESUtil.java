package com.ezyCollect.payments.payment_service.util;

import com.ezyCollect.payments.payment_service.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {
    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int KEY_SIZE = 128;
    private static final SecureRandom secureRandom = new SecureRandom();

    // Generate a new AES key
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES);
        keyGen.init(KEY_SIZE, secureRandom);
        return keyGen.generateKey();
    }

    // Load AES key from a byte array
    public static SecretKey loadKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, AES);
    }

    // Encrypt a plain text string
    public static String encrypt(String plainText, SecretKey key, byte[] iv) {
        try{
            if (iv.length != IV_LENGTH) {
                throw new IllegalArgumentException("IV must be 12 bytes for AES-GCM");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(KEY_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    // Decrypt a cipher text string
    public static String decrypt(String cipherText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(KEY_SIZE, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decoded = Base64.getDecoder().decode(cipherText);
        return new String(cipher.doFinal(decoded));
    }

    // Generate a random IV
    public static byte[] generateRandomIV() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    // Encode key to Base64 (for storing in yml or env)
    public static String encodeKeyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Decode Base64 key to SecretKey
    public static SecretKey decodeKeyFromBase64(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return loadKeyFromBytes(decoded);
    }
}
