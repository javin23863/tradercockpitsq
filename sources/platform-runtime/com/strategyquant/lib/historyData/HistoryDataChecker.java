/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.historyData;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.ICryptable;

public class HistoryDataChecker {
    public static void checkDataExport(ICryptable iCryptable, int n) throws Exception {
        if ((n == 6 || n == 5 || iCryptable.isCrypted()) && !MainApp.v571hfnsHw().eHvc2JguAd().equals("DATAEXP")) {
            throw new Exception(L.t("SQ Equities/Futures Data cannot be used outside SQ, they cannot be exported", new Object[0]));
        }
    }
}

