/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.indicators;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorBase;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorParam;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorParserException;
import java.io.File;

public class IndicatorParser {
    private String IMPORT_STATEMENT = "import ";

    public IndicatorBase parse(File file) throws Exception {
        String string = SQUtils.fileToString(file);
        if (!this.prvFileIsIndicator(string)) {
            return null;
        }
        IndicatorBase indicatorBase = new IndicatorBase();
        indicatorBase.setName(SQUtils.stripExtension(file.getName()));
        this.prvParameters(string, "@Parameter", indicatorBase);
        this.prvImports(string, indicatorBase);
        this.prvOutputs(string, "@Output", indicatorBase);
        indicatorBase.getImportsList().add(this.prvRecognizeIndicatorPackage(file, string));
        return indicatorBase;
    }

    private void prvImports(String string, IndicatorBase indicatorBase) {
        int n;
        int n2 = 0;
        while ((n = string.indexOf(this.IMPORT_STATEMENT, n2)) != -1) {
            n2 = string.indexOf(";", n);
            String string2 = string.substring(n, n2);
            if ((string2 = string2.replaceFirst(this.IMPORT_STATEMENT, "").trim()).startsWith("com.strategyquant.lib.indicator.annotations") || string2.startsWith("org.") || string2.startsWith("java.") || string2.contains("SQ.Internal")) continue;
            indicatorBase.getImportsList().add(string2);
        }
    }

    private void prvParameters(String string, String string2, IndicatorBase indicatorBase) {
        int n;
        int n2 = 0;
        while ((n = string.indexOf(string2, n2)) != -1) {
            String string3;
            if (string2.equals("@Parameter") && (string3 = string.substring(n, n + 15)).contains("@ParameterSet")) {
                n2 = n + 15;
                continue;
            }
            n2 = string.indexOf(";", n);
            String string4 = string.substring(n, n2);
            if (this.isCommented(n, string)) continue;
            string4 = string.substring(n, n2);
            String[] stringArray = string4.split(" ");
            if (string4.equals("Shift")) continue;
            indicatorBase.getParamList().add(new IndicatorParam(stringArray[stringArray.length - 2], stringArray[stringArray.length - 1]));
        }
    }

    private boolean isCommented(int n, String string) {
        String string2;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = n - 1; i > 0; --i) {
            stringBuilder.insert(0, string.charAt(i));
            if (string.charAt(i) == '\n') break;
        }
        return (string2 = stringBuilder.toString().trim()).startsWith("//") || string2.startsWith("/*") || string2.startsWith("*");
    }

    private void prvOutputs(String string, String string2, IndicatorBase indicatorBase) {
        int n;
        int n2 = 0;
        while ((n = string.indexOf(string2, n2)) != -1) {
            n2 = string.indexOf(";", n);
            if (this.isCommented(n, string)) continue;
            String string3 = string.substring(n, n2);
            String[] stringArray = string3.split(" ");
            indicatorBase.getOutputsList().add(stringArray[stringArray.length - 1]);
        }
    }

    private boolean prvFileIsIndicator(String string) {
        int n = string.indexOf(" extends ");
        if (n < 0) {
            return false;
        }
        int n2 = string.indexOf("{", n);
        if (n2 < 0) {
            return false;
        }
        String string2 = string.substring(n, n2);
        return string2.contains("Indicator");
    }

    private String prvRecognizeIndicatorPackage(File file, String string) throws IndicatorParserException {
        int n = string.indexOf("package");
        if (n < 0) {
            throw new IndicatorParserException("File " + file.getName() + " is not correct indicator implementation, package is missing!");
        }
        int n2 = string.indexOf(";", n);
        if (n2 < 0) {
            throw new IndicatorParserException("File " + file.getName() + " is not correct indicator implementation, package is missing (2)!");
        }
        String string2 = string.substring(n + 8, n2);
        string2 = string2.trim();
        String string3 = file.getName().replace(".java", "");
        return string2 + "." + string3;
    }
}

