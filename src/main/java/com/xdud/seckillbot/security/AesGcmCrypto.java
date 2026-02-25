package com.xdud.seckillbot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具，用于安全存储 credential_json 和 auth_context。
 * 每次加密使用随机 IV，输出格式：Base64(iv + ciphertext + tag)
 */
@Component
public class AesGcmCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    @Value("${app.crypto.aes-key}")
    private String base64Key;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH_BYTES + ciphertextWithTag.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertextWithTag, 0, combined, IV_LENGTH_BYTES, ciphertextWithTag.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Ciphertext);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] ciphertextWithTag = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, IV_LENGTH_BYTES, ciphertextWithTag, 0, ciphertextWithTag.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertextWithTag), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 解密失败", e);
        }
    }
}
