/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.historyData;

public class DataSources {
    public static String DOWNLOAD_TYPE_STANDARD = "standard";
    public static String DOWNLOAD_TYPE_CDN = "cdn";
    public static String DOWNLOAD_TYPE_CDN_CN = "cdn-cn";
    public static String NAME_FILE = "File";
    public static String NAME_DUKASCOPY = "Dukascopy";
    public static String NAME_Yahoo = "Yahoo";
    public static String NAME_DARWINEX = "Darwinex";
    public static String NAME_SQEquityData = "SQ Equity data";
    public static String NAME_SQFuturesData = "SQ Futures data";
    public static String NAME_Cryptocurrency = "Cryptocurrency";
    public static String NAME_MT5_API = "MT5 Api";
    public static final int File = 1;
    public static final int Dukascopy = 2;
    public static final int Yahoo = 3;
    public static final int Darwinex = 4;
    public static final int SQEquityData = 5;
    public static final int SQFuturesData = 6;
    public static final int Cryptocurrency = 7;
    public static final int Mt5Api = 8;

    public static String toString(int n) {
        switch (n) {
            case 1: {
                return NAME_FILE;
            }
            case 2: {
                return NAME_DUKASCOPY;
            }
            case 3: {
                return NAME_Yahoo;
            }
            case 4: {
                return NAME_DARWINEX;
            }
            case 5: {
                return NAME_SQEquityData;
            }
            case 6: {
                return NAME_SQFuturesData;
            }
            case 7: {
                return NAME_Cryptocurrency;
            }
            case 8: {
                return NAME_MT5_API;
            }
        }
        return n + "";
    }

    public static boolean isCDN(String string) {
        return string.equals(DOWNLOAD_TYPE_CDN) || string.equals(DOWNLOAD_TYPE_CDN_CN);
    }
}

