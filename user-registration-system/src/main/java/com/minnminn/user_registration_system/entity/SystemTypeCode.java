package com.minnminn.user_registration_system.entity;

/**
 * Maps Excel column groups on the User ID sheet.
 */
public enum SystemTypeCode {
    VPN("VPN", "VPN"),
    PSS2("PSS2", "CBM-NET related"),
    INTER_BANK("INTER_BANK", "Inter-Bank Reporting"),
    MOBILE_WALLET("MOBILE_WALLET", "Mobile Wallet"),
    BANK_FRAUD("BANK_FRAUD", "Bank Account and Fraud Report"),
    MIB("MIB", "Mobile and Internet Banking");

    private final String code;
    private final String displayName;

    SystemTypeCode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
