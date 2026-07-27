package com.minnminn.user_registration_system.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Result Notice input — like Excel dropdowns, for one or many banks.
 */
@Data
public class BatchNoticeRequest {

    /** Selected FI ids. Empty + selectAll=true means all banks. */
    private List<Long> institutionIds = new ArrayList<>();

    private boolean selectAll;

    private LocalDate noticeDate = LocalDate.now();

    private String vpnStatus = "Updated";
    private String fidesLoginStatus = "Updated";
    private String userCategory = "Mobile and Internet Banking";

    private boolean regenerateVpnPassword;
    private boolean regenerateVpnPsk;
    private boolean regenerateLoginPassword;
}
