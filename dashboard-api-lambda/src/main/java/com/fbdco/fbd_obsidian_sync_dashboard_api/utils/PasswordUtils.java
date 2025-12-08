package com.fbdco.fbd_obsidian_sync_dashboard_api.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits

    public static String hashPassword(String password) throws Exception {
        // Generate random salt
        byte[] salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);

        // Hash using PBKDF2
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        return ITERATIONS + "$" +
                Base64.getEncoder().encodeToString(salt) + "$" +
                Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    public static boolean verifyPassword(String password, String stored) throws Exception {
        String[] parts = stored.split("\\$");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] storedHash = Base64.getDecoder().decode(parts[2]);

        // Hash incoming password using same salt + iterations
        byte[] hash = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);

        // Constant-time comparison
        if (hash.length != storedHash.length) return false;
        int result = 0;
        for (int i = 0; i < hash.length; i++) {
            result |= hash[i] ^ storedHash[i];
        }
        return result == 0;
    }
}

