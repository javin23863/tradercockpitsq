/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.SQUtils;
import java.util.List;

public class CompilationUtils {
    private CompilationUtils() {
    }

    public static String dependenciesToString(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = System.getProperty("os.name").toLowerCase().contains("windows");
        String string = bl ? ";" : ":";
        for (String string2 : list) {
            if (string2.endsWith("SQLib.jar")) continue;
            stringBuilder.append(string2).append(string);
        }
        return SQUtils.replaceLast(stringBuilder.toString(), string, "");
    }
}

