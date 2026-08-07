package com.minnminn.user_registration_system.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Password generator matching Excel Password Reference character set.
 */
@Service
public class PasswordGeneratorService {

    /** Exact Excel Password Reference string. */
    public static final String PASSWORD_REFERENCE =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz123456789!@#$%^&*()-+[]_";

    public static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    public static final String DIGITS = "123456789";
    public static final String SPECIAL = "!@#$%^&*()-+[]_";
    public static final String FULL_SET = UPPER + LOWER + DIGITS + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
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
