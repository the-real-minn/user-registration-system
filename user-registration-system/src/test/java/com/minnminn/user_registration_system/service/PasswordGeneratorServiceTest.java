package com.minnminn.user_registration_system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorServiceTest {

    private final PasswordGeneratorService service = new PasswordGeneratorService();

    @Test
    void generateHasExpectedLengthAndCharset() {
        String password = service.generate(8);
        assertEquals(8, password.length());
        for (char c : password.toCharArray()) {
            assertTrue(PasswordGeneratorService.FULL_SET.indexOf(c) >= 0,
                    "Unexpected char: " + c);
        }
    }

    @Test
    void generateUserIdUsesLeft4PlusSuffix() {
        assertEquals("CBMYPSS", service.generateUserId("CBMYMMMY", "PSS"));
    }
}
