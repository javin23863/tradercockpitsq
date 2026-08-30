/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.settings;

import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;

public class ParametrizationSettings {
    public static final String TYPE_STRATEGY_XML = "Strategy XML";
    public static final String TYPE_STRATEGY_XML_KEY = "StrategyXML";

    public static String getSettingKey(String string) {
        String string2 = "SourceCodeParams";
        if (string.equals(TYPE_STRATEGY_XML)) {
            return string2 + TYPE_STRATEGY_XML_KEY;
        }
        SourceCodeGenerator sourceCodeGenerator = SourceCodeGenerators.getInstance().getGeneratorFromName(string);
        if (sourceCodeGenerator != null) {
            return string2 + sourceCodeGenerator.getKey();
        }
        return string2;
    }
}

