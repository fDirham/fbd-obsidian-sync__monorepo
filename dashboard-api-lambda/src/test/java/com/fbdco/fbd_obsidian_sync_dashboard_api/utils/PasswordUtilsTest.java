package com.fbdco.fbd_obsidian_sync_dashboard_api.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilsTest {

    @Test
    void hashPassword_ReturnsHashedPassword() throws Exception {
        String password = "myPassword123";
        String hashed = PasswordUtils.hashPassword(password);

        assertNotNull(hashed);
        assertNotEquals(password, hashed);
        assertTrue(hashed.contains("$"));
    }

    @Test
    void verifyPassword_CorrectPassword_ReturnsTrue() throws Exception {
        String password = "myPassword123";
        String hashed = PasswordUtils.hashPassword(password);

        assertTrue(PasswordUtils.verifyPassword(password, hashed));
    }

    @Test
    void verifyPassword_IncorrectPassword_ReturnsFalse() throws Exception {
        String password = "myPassword123";
        String hashed = PasswordUtils.hashPassword(password);

        assertFalse(PasswordUtils.verifyPassword("wrongPassword", hashed));
    }
}


