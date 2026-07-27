package com.minnminn.user_registration_system.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Password generator matching Excel Password Reference character sets.
 */
@Service
public class PasswordGeneratorService {

    public static final String ALPHANUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static final String SPECIAL = "!@#$%^&*()-_+[]{}|";

    public static final String FULL_SET = ALPHANUMERIC + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }
        StringBuilder sb = new StringBuilder(length);
        // Ensure at least one from each set
        sb.append(ALPHANUMERIC.charAt(random.nextInt(26))); // upper
        sb.append(ALPHANUMERIC.charAt(26 + random.nextInt(26))); // lower
        sb.append(ALPHANUMERIC.charAt(52 + random.nextInt(10))); // digit
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        for (int i = 4; i < length; i++) {
            sb.append(FULL_SET.charAt(random.nextInt(FULL_SET.length())));
        }
        return shuffle(sb.toString());
    }

    public String generateVpnPassword() {
        return generate(8);
    }

    public String generateUserPassword() {
        return generate(8);
    }

    public String generatePsk() {
        return generate(8);
    }

    /**
     * Excel rule: LEFT(fiCode, 4) + suffix.
     */
    public String generateUserId(String fiCode, String suffix) {
        if (fiCode == null || fiCode.length() < 4) {
            throw new IllegalArgumentException("FI Code must be at least 4 characters");
        }
        String safeSuffix = suffix == null ? "" : suffix;
        return fiCode.substring(0, 4) + safeSuffix;
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
