package com.minnminn.user_registration_system.dto;

import com.minnminn.user_registration_system.entity.SystemTypeCode;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ResultNoticeForm {

    private Long financialInstitutionId;
    private LocalDate noticeDate = LocalDate.now();

    private String vpnStatus = "Updated";
    private String fidesLoginStatus = "Updated";

    /** Display name matching Excel Category dropdown. */
    private String userCategory = "Mobile and Internet Banking";

    private String vpnUserId;
    private String vpnPassword;
    private String vpnPsk;

    private String loginUserId;
    private String loginPassword;

    private boolean regenerateVpnPassword;
    private boolean regenerateVpnPsk;
    private boolean regenerateLoginPassword;

    public SystemTypeCode resolveCategoryCode() {
        if (userCategory == null || userCategory.isBlank() || "N/A".equalsIgnoreCase(userCategory)) {
            return null;
        }
        return switch (userCategory) {
            case "CBM-NET related" -> SystemTypeCode.PSS2;
            case "Inter-Bank Reporting" -> SystemTypeCode.INTER_BANK;
            case "Mobile Wallet" -> SystemTypeCode.MOBILE_WALLET;
            case "Bank Account and Fraud Report" -> SystemTypeCode.BANK_FRAUD;
            case "Mobile and Internet Banking" -> SystemTypeCode.MIB;
            default -> null;
        };
    }
}
