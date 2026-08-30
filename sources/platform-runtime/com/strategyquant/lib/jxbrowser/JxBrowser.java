/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.jxbrowser;

import com.strategyquant.lib.app.MainApp;

public class JxBrowser {
    private static String version = "84.0.4147.135";

    public static String binPath() {
        return MainApp.getDataPath() + "internal/jxbrowser/" + version;
    }

    public static String dataPath(String string) {
        return MainApp.getDataPath() + "internal/jxbrowser/" + version + "/data/" + MainApp.getProduct() + "/" + string;
    }
}

