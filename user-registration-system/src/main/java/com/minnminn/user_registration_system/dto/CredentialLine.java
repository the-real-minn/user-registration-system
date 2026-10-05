package com.minnminn.user_registration_system.dto;

/**
 * One system credential row on FI create/edit form.
 */
public class CredentialLine {

    private Long systemTypeId;
    private String systemCode;
    private String displayName;
    private String userId;
    private String password;
    private String preSharedKey;
    private String notes;
    private boolean vpn;

    public Long getSystemTypeId() {
        return systemTypeId;
    }

    public void setSystemTypeId(Long systemTypeId) {
        this.systemTypeId = systemTypeId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPreSharedKey() {
        return preSharedKey;
    }

    public void setPreSharedKey(String preSharedKey) {
        this.preSharedKey = preSharedKey;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isVpn() {
        return vpn;
    }

    public void setVpn(boolean vpn) {
        this.vpn = vpn;
    }
}
