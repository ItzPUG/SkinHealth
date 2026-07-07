package com.example.skincancerai;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class DataCipher {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "skincancerai_profile_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PREFIX = "ENC::";
    private static final int GCM_TAG_BITS = 128;

    private DataCipher() {}

    public static String encrypt(String raw) {
        if (raw == null || raw.trim().isEmpty()) return raw;
        if (isEncrypted(raw)) return raw;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(4 + iv.length + encrypted.length);
            buffer.putInt(iv.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            return raw;
        }
    }

    public static String decrypt(String value) {
        if (value == null || value.trim().isEmpty()) return value;
        if (!isEncrypted(value)) return value;
        try {
            byte[] packed = Base64.decode(value.substring(PREFIX.length()), Base64.NO_WRAP);
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            int ivLength = buffer.getInt();
            if (ivLength <= 0 || ivLength > 32 || buffer.remaining() <= ivLength) return value;

            byte[] iv = new byte[ivLength];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] clear = cipher.doFinal(encrypted);
            return new String(clear, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static boolean isEncrypted(String value) {
        return value.startsWith(PREFIX);
    }

    private static SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        android.security.keystore.KeyGenParameterSpec spec =
                new android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                | android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
}
