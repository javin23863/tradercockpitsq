/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

public class Eob3xULZVY
extends Exception {
    public static final int ERROR_INET_CONNECTION_ERROR = 2;
    public static final int ERROR_INVALID_LICENSE = 3;
    public static final int ERROR_TRIAL_EXPIRED = 5;
    public static final int ERROR_LICENSE_FILE_MISSING = 6;
    public static final int ERROR_INVALID_PRODUCT_CODE = 7;
    public static final int ERROR_INVALID_SIGNING = 8;
    public static final int ERROR_INVALID_SERVER_RESPONSE = 9;
    public static final int ERROR_MISSING_SECRET_KEY_FOR_LICENSE_FILE = 10;
    public static final int ERROR_LOAD_LICENSE_FILE = 11;
    public static final int ERROR_SAVE_LICENSE_FILE = 12;
    public static final int ERROR_GET_CURRENT_TIME_FROM_NIST_SERVER = 13;
    public static final int ERROR_LOAD_KEY_FROM_FILE = 14;
    public static final int ERROR_MISSING_KEY_FILE = 15;
    public static final int ERROR_GENERATE_SECRET_KEY = 16;
    public static final int ERROR_CREATE_CONTROL_HASH = 17;
    public static final int ERROR_MISSING_PUBLIC_KEY = 18;
    public static final int ERROR_VERIFY_DIGITAL_SIGNING = 19;
    public static final int ERROR_VERIFY_LICENSE = 20;
    public static final int ERROR_FIND_LOCALHOST = 21;
    public static final int ERROR_NO_PRODUCT_SET = 22;
    public static final int ERROR_INVALID_RESELLER_ID = 23;
    public static final int ERROR_INVALID_UNIQID = 24;
    public static final int ERROR_INVALID_EDITION_TYPE = 26;
    public static final int ERROR_CORUPT_LICENSE_FILE = 27;
    public static final int ERROR_NOT_VALID_LICENSE_FILE_VERSION = 28;
    public static final int ERROR_INVALID_HARDWARE_ID = 29;
    public static final int ERROR_LICENSE_FILE_NOT_VALID_FOR_THIS_BUILD = 30;
    public static final int ERROR_LICENSE_FILE_VALIDATION_EXPIRED = 31;
    public static final int ERROR_LICENSE_SUPPORT_EXPIRED = 40;
    private final int errorCode;

    public Eob3xULZVY(int n, String string) {
        super(string);
        this.errorCode = n;
    }

    public Eob3xULZVY(int n) {
        super(Eob3xULZVY.getCodeAsStr(n));
        this.errorCode = n;
    }

    private static String getCodeAsStr(int n) {
        switch (n) {
            case 2: {
                return "Error - Program cannot connect to internet";
            }
            case 3: {
                return "Error - Invalid license file (1).";
            }
            case 5: {
                return "License is invalid - Trial expired.";
            }
            case 6: {
                return "License is invalid - Trial expired.";
            }
            case 7: {
                return "Error - Invalid license file (2).";
            }
            case 8: {
                return "Error verifying license on server, check internet connection.";
            }
            case 9: {
                return "Error - Invalid license server response.";
            }
            case 10: {
                return "License is invalid - Trial expired.";
            }
            case 11: {
                return "Error loading license file.";
            }
            case 12: {
                return "Error saving license file.";
            }
            case 13: {
                return "Error getting current date.";
            }
            case 14: {
                return "Error - Invalid license file (3).";
            }
            case 15: {
                return "Error - Invalid license file (4).";
            }
            case 16: {
                return "Error - Invalid license file (5).";
            }
            case 17: {
                return "Error - Invalid license file (6).";
            }
            case 18: {
                return "Error - Invalid license file (7).";
            }
            case 19: {
                return "Error - Invalid license file (8).";
            }
            case 20: {
                return "Error - verification of product license on the server failed.";
            }
            case 21: {
                return "Error - cannot find network, are you connected to the internet?";
            }
            case 22: {
                return "Internal Error - no product set!";
            }
            case 23: {
                return "Error - Invalid license file (9).";
            }
            case 24: {
                return "Error - Invalid license file (10).";
            }
            case 26: {
                return "Invalid license (26).";
            }
            case 27: {
                return "Invalid or corrupt license file.";
            }
            case 28: {
                return "Error - license is not valid for this program version.";
            }
            case 29: {
                return "Error - cannot run program on another computer.";
            }
            case 30: {
                return "License file is not valid for this build.";
            }
            case 31: {
                return "License file validation has expired.";
            }
        }
        return "Unknown reason: " + n;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}

