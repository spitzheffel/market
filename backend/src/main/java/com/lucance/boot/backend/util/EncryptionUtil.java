package com.lucance.boot.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具
 * 用于加密存储 API Key 和 Secret
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    @Value("${encryption.secret-key:default-secret-key-change-in-production}")
    private String secretKeyPassword;

    @Value("${encryption.salt:default-salt-change-me}")
    private String salt;

    /**
     * 加密字符串
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKey secretKey = generateSecretKey();
            byte[] iv = generateIV();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

            // 将 IV 和密文组合
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密字符串
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);

            // 提取 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // 提取密文
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            SecretKey secretKey = generateSecretKey();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * 检查是否已加密
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return decoded.length > GCM_IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成密钥
     */
    private SecretKey generateSecretKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                secretKeyPassword.toCharArray(),
                salt.getBytes(),
                ITERATION_COUNT,
                KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * 生成随机 IV
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
