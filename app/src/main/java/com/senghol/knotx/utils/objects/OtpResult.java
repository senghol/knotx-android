package com.senghol.knotx.utils.objects;

public final class OtpResult {
    public static final String SUCCESS = "SUCCESS";
    public static final String INVALID_OTP = "INVALID_OTP";
    public static final String INVALID_SESSION = "INVALID_SESSION";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String LIMIT_REACHED = "TOO_MANY_ATTEMPT";
    public static final String ERROR = "ERROR";

    private OtpResult() {
        // Prevent instantiation
    }
}