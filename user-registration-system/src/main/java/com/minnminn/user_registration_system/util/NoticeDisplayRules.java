package com.minnminn.user_registration_system.util;

import com.minnminn.user_registration_system.entity.ResultNotice;

/**
 * Display rules for Result Notice print/PDF (match Excel behavior).
 */
public final class NoticeDisplayRules {

    public static final String NA = "N/A";
    public static final String VPN_PSK_CHANGE = "Pre-Shared Key Change";
    public static final String NO_CHANGE = "No Change";

    private NoticeDisplayRules() {
    }

    public static boolean isVpnPskChange(String vpnStatus) {
        return VPN_PSK_CHANGE.equalsIgnoreCase(vpnStatus);
    }

    public static boolean isVpnNoChange(String vpnStatus) {
        return NO_CHANGE.equalsIgnoreCase(vpnStatus);
    }

    public static boolean isFidesNoChange(String fidesLoginStatus) {
        return NO_CHANGE.equalsIgnoreCase(fidesLoginStatus);
    }

    public static String vpnUserId(ResultNotice notice) {
        if (isVpnNoChange(notice.getVpnStatus()) || isVpnPskChange(notice.getVpnStatus())) {
            return NA;
        }
        return valueOrEmpty(notice.getVpnUserId());
    }

    public static String vpnPassword(ResultNotice notice) {
        if (isVpnNoChange(notice.getVpnStatus()) || isVpnPskChange(notice.getVpnStatus())) {
            return NA;
        }
        return valueOrEmpty(notice.getVpnPassword());
    }

    public static String vpnPsk(ResultNotice notice) {
        if (isVpnNoChange(notice.getVpnStatus())) {
            return NA;
        }
        return valueOrEmpty(notice.getVpnPsk());
    }

    public static String loginUserId(ResultNotice notice) {
        return isFidesNoChange(notice.getFidesLoginStatus()) ? NA : valueOrEmpty(notice.getLoginUserId());
    }

    public static String loginPassword(ResultNotice notice) {
        return isFidesNoChange(notice.getFidesLoginStatus()) ? NA : valueOrEmpty(notice.getLoginPassword());
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
