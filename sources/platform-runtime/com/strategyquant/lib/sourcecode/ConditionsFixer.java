/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.sourcecode.ComparatorStringLength;
import com.strategyquant.lib.sourcecode.VarAndIndex;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionsFixer {
    public static final Logger Log = LoggerFactory.getLogger((String)"ConditionsFixer");
    public ArrayList<String> variables;
    public String condition = "";
    private String originalText;
    private static final Comparator<? super VarAndIndex> compStringLength = new ComparatorStringLength();

    private void processCondition(String string, String string2) {
        int n = 0;
        int n2 = 1;
        String string3 = null;
        String string4 = null;
        String string5 = string;
        int n3 = 0;
        if ((string = this.modifyRisingFallingWithPriceBlocks(string)).startsWith("{CustomBlock:")) {
            n3 = string.indexOf("}");
        }
        if ((n = string.indexOf("<>", n3)) >= 0) {
            n2 = 2;
        } else {
            n = string.indexOf("!=", n3);
        }
        if (n >= 0) {
            n2 = 2;
        } else {
            n = string.indexOf(">", n3);
        }
        if (n < 0) {
            n = string.indexOf("<", n3);
        }
        if (n < 0) {
            n = string.indexOf("=", n3);
        }
        if (n >= 0) {
            string3 = string.substring(0, n);
        }
        if ((n = string.lastIndexOf("=")) < 0) {
            n = string.lastIndexOf("<");
        }
        if (n < 0) {
            n = string.lastIndexOf(">");
        }
        if (n >= 0) {
            string4 = string.substring(n + n2);
        }
        if (string3 != null && (string3.contains("false") || string3.contains("true") || string3.contains("MarketPosition") || string3.contains("IsRising") || string3.contains("IsFalling"))) {
            string3 = null;
        }
        if (string4 != null && (string4.contains("false") || string4.contains("true") || string4.contains("MarketPosition") || string4.contains("IsRising") || string4.contains("IsFalling"))) {
            string4 = null;
        }
        String string6 = string;
        if (string3 != null && string4 != null) {
            if (string3.matches("(.*)[a-zA-Z]+(.*)")) {
                string6 = this.addUniqueVar(string3, string6);
            }
            if (string4.matches("(.*)[a-zA-Z]+(.*)")) {
                string6 = this.addUniqueVar(string4, string6);
            }
            if (!string6.equals(string)) {
                this.originalText = this.originalText.replace(string5, string6);
            }
        } else if (!string6.equals(string5)) {
            this.originalText = this.originalText.replace(string5, string6);
        }
    }

    private String addUniqueVar(String string, String string2) {
        if (this.variables == null) {
            this.variables = new ArrayList();
        }
        String string3 = this.ensureSameBrackets(string).trim();
        string3 = string3.replaceAll("\\{.*?\\}", "");
        String string4 = string;
        for (int i = 0; i < this.variables.size(); ++i) {
            if (!this.variables.get(i).equals(string3)) continue;
            String string5 = this.getUniqFromVar(string3);
            string4 = string4.replace(string3, string5);
            return string2.replace(string, string4);
        }
        string3 = this.fixDFunctions(string3);
        this.variables.add(string3);
        String string6 = this.getUniqFromVar(string3);
        string4 = string4.replace(string3, string6);
        return string2.replace(string, string4);
    }

    private String getUniqFromVar(String string) {
        StringBuilder stringBuilder = new StringBuilder("__#");
        stringBuilder.append(string.hashCode());
        stringBuilder.append("#__");
        return stringBuilder.toString();
    }

    private String fixDFunctions(String string) {
        String string2 = this.fixDFunction("OpenD", string);
        if (string2 != null) {
            this.originalText = this.originalText.replace(string, string2);
            string = string2;
        }
        if ((string2 = this.fixDFunction("HighD", string)) != null) {
            this.originalText = this.originalText.replace(string, string2);
            string = string2;
        }
        if ((string2 = this.fixDFunction("LowD", string)) != null) {
            this.originalText = this.originalText.replace(string, string2);
            string = string2;
        }
        if ((string2 = this.fixDFunction("CloseD", string)) != null) {
            this.originalText = this.originalText.replace(string, string2);
            string = string2;
        }
        return string;
    }

    private String fixDFunction(String string, String string2) {
        int n;
        StringBuilder stringBuilder = new StringBuilder();
        int n2 = string2.indexOf(string);
        if (n2 < 0) {
            return null;
        }
        if (string2.contains("SQ_IsRisingD") || string2.contains("SQ_IsFallingD")) {
            return null;
        }
        String string3 = "SQ_IsRising";
        int n3 = string2.indexOf(string3);
        if (n3 < 0) {
            string3 = "SQ_IsFalling";
            n3 = string2.indexOf(string3);
        }
        if (n3 < 0) {
            return null;
        }
        if (n3 >= n2) {
            return null;
        }
        stringBuilder.append(string2.substring(0, n3 + string3.length()));
        stringBuilder.append("D(\"");
        int n4 = string2.indexOf("(", n2);
        stringBuilder.append(string2.substring(n2, n4));
        stringBuilder.append("\", ");
        int n5 = string2.indexOf(")", n4 + 1);
        if (n5 < 0) {
            return null;
        }
        String string4 = string2.substring(n4 + 1, n5);
        stringBuilder.append(string4);
        stringBuilder.append(",");
        int n6 = string2.indexOf("[", n4 + 1);
        if (n6 < 0) {
            return null;
        }
        string4 = string2.substring(n5 + 2, n6 - 1);
        String string5 = null;
        int n7 = string4.indexOf("of Data");
        if (n7 >= 0) {
            n = string4.indexOf(",");
            if (n < 0) {
                stringBuilder.append(string4);
            } else {
                stringBuilder.append(string4.substring(n + 1));
                string5 = " " + string4.substring(n7, n);
            }
        } else {
            stringBuilder.append(string4);
        }
        stringBuilder.append(", ");
        n = string2.indexOf("]", n6);
        if (n < 0) {
            return null;
        }
        string4 = string2.substring(n6 + 1, n);
        stringBuilder.append(string4);
        stringBuilder.append(")");
        if (string5 != null) {
            stringBuilder.append(string5);
        }
        return stringBuilder.toString();
    }

    private String ensureSameBrackets(String string) {
        int n;
        int n2 = this.countBrackets("(", string);
        if (n2 == (n = this.countBrackets(")", string))) {
            return string;
        }
        if (n2 > n) {
            return this.removeBrackets(string, "(", n2 - n);
        }
        return this.removeBrackets(string, ")", n - n2);
    }

    private String removeBrackets(String string, String string2, int n) {
        for (int i = 0; i < n; ++i) {
            int n2 = string2.equals("(") ? string.indexOf(string2) : string.lastIndexOf(string2);
            if (n2 < 0) continue;
            string = string.substring(0, n2) + string.substring(n2 + 1);
        }
        return string;
    }

    private int countBrackets(String string, String string2) {
        int n = 0;
        int n2 = 0;
        while (n2 <= string2.length()) {
            int n3 = string2.indexOf(string, n2);
            if (n3 < 0) {
                return n;
            }
            ++n;
            n2 = n3 + 1;
        }
        return n;
    }

    public void replaceOriginalCondition(String string) {
        if (this.variables != null && this.variables.size() > 0) {
            VarAndIndex varAndIndex;
            int n;
            ArrayList<? super VarAndIndex> arrayList = new ArrayList<VarAndIndex>();
            for (n = 0; n < this.variables.size(); ++n) {
                varAndIndex = new VarAndIndex(n, this.variables.get(n));
                arrayList.add(varAndIndex);
            }
            arrayList.sort(compStringLength);
            for (n = 0; n < arrayList.size(); ++n) {
                varAndIndex = (VarAndIndex)arrayList.get(n);
                String string2 = this.getUniqFromVar(varAndIndex.var);
                string = string.replace(string2, "Value" + (varAndIndex.index + 1));
            }
        }
        this.condition = string;
    }

    public String processConditionsTS(String string) {
        string = string.replace("__NBSP1__", " ");
        string = string.replace("__BR__", " ");
        string = string.replaceAll("\n", "");
        string = string.replaceAll("\r", "");
        string = string.replaceAll("true = true", "1 = 1");
        string = string.replaceAll("  ", " ");
        String[] stringArray = string.split(";");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stringArray.length; ++i) {
            stringBuilder.append(this.convertCondition(stringArray[i]));
        }
        String string2 = stringBuilder.toString();
        string2 = string2.replace("__CFBR__", "\r\n");
        return string2;
    }

    private String convertCondition(String string) {
        String string2;
        int n;
        String string3;
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = false;
        if ((string = string.trim()).startsWith("if")) {
            string3 = "if";
            n = string.indexOf(string3);
            if (n < 0) {
                return string;
            }
            string2 = string.substring(n + string3.length());
        } else {
            bl = true;
            n = string.indexOf("=");
            if (n < 0) {
                return string;
            }
            string3 = string.substring(0, n);
            string2 = string.substring(n + 1).trim();
        }
        this.analyze(string2);
        if (this.variables != null) {
            for (n = 0; n < this.variables.size(); ++n) {
                stringBuilder.append("Value");
                stringBuilder.append(n + 1);
                stringBuilder.append("= ");
                stringBuilder.append(this.fixMultiTF(this.variables.get(n)));
                stringBuilder.append(";");
                stringBuilder.append("\r\n");
            }
        }
        if (bl) {
            stringBuilder.append(string3);
            stringBuilder.append("= ");
            stringBuilder.append(this.condition);
            stringBuilder.append(";");
        } else {
            stringBuilder.append(string3);
            stringBuilder.append(this.condition);
        }
        stringBuilder.append("\r\n");
        return stringBuilder.toString();
    }

    private String fixMultiTF(String string) {
        if (string.startsWith("Month(") || string.startsWith("MinList(") || string.startsWith("MaxList(")) {
            return string;
        }
        for (int i = 0; i < 20; ++i) {
            string = this.fixOneMultiTF(i, string);
        }
        return string;
    }

    private String fixOneMultiTF(int n, String string) {
        String string2 = " of Data" + n;
        if (string.contains(string2)) {
            string = string.replaceAll(string2, "");
            return string + string2;
        }
        return string;
    }

    private void analyze(String string) {
        String string2;
        this.clear();
        this.originalText = string;
        while (true) {
            String string3 = "and";
            int n = string.indexOf(" and ");
            int n2 = string.indexOf(" or ");
            int n3 = -1;
            if (n >= 0) {
                n3 = n;
            }
            if (n2 >= 0) {
                if (n3 < 0) {
                    n3 = n2;
                    string3 = "or";
                } else if (n2 < n3) {
                    n3 = n2;
                    string3 = "or";
                }
            }
            if (n3 < 0) break;
            string2 = string.substring(0, n3);
            this.processCondition(string2, string3);
            string = string.substring(n3 + string3.length() + 2);
        }
        string2 = string;
        this.processCondition(string2, "");
        this.replaceOriginalCondition(this.originalText);
    }

    private void clear() {
        if (this.variables != null) {
            this.variables.clear();
        }
    }

    public void process(TemplateDirectiveBody templateDirectiveBody, Writer writer) throws IOException, TemplateException {
        StringWriter stringWriter = new StringWriter();
        templateDirectiveBody.render((Writer)stringWriter);
        String string = stringWriter.toString();
        String string2 = this.processConditionsTS(string);
        writer.write(string2);
    }

    private String modifyRisingFallingWithPriceBlocks(String string) {
        String string2;
        boolean bl = false;
        if (string.contains("SQ_IsRising(")) {
            bl = true;
        } else if (!string.contains("SQ_IsFalling(")) {
            return string;
        }
        String[] stringArray = this.getRFParameters(string, bl ? "SQ_IsRising" : "SQ_IsFalling");
        if (stringArray != null && this.isRFProblematicBlock(string2 = stringArray[0])) {
            int n = string2.indexOf("(");
            if (n < 0) {
                return string;
            }
            int n2 = string2.indexOf(")");
            if (n2 < 0) {
                return string;
            }
            String string3 = string2.substring(0, n);
            String string4 = string2.substring(n + 1, n2);
            int n3 = Integer.parseInt(stringArray[1]);
            String string5 = stringArray[2];
            String string6 = stringArray[3];
            String string7 = String.format("%s(\"%s\", %s, %d, %s, %s) = 1", bl ? "SQ_IsRisingD" : "SQ_IsFallingD", string3, string4, n3, string5, string6);
            int n4 = string.indexOf("SQ_Is");
            int n5 = string.indexOf("= 1");
            String string8 = string.substring(n4, n5 + 3);
            String string9 = string.replace(string8, string7);
            return string9;
        }
        return string;
    }

    private String[] getRFParameters(String string, String string2) {
        int n = string.indexOf(string2) + string2.length();
        if ((n = string.indexOf("(", n)) < 0) {
            return null;
        }
        int n2 = string.indexOf("[", n);
        if (n2 < 0) {
            return null;
        }
        String string3 = string.substring(n2 + 1);
        string = string.substring(n + 1, n2 - 1);
        String[] stringArray = new String[4];
        int n3 = string.lastIndexOf(",");
        int n4 = 0;
        while (n3 > 0 && n4 < 2) {
            stringArray[2 - n4++] = string.substring(n3 + 1).trim();
            string = string.substring(0, n3);
            n3 = string.lastIndexOf(",");
        }
        if (n4 < 2) {
            return null;
        }
        stringArray[0] = string;
        int n5 = string3.indexOf("]");
        if (n5 > 0) {
            stringArray[3] = string3.substring(0, n5);
        }
        return stringArray;
    }

    private boolean isRFProblematicBlock(String string) {
        if (string == null) {
            return false;
        }
        if (string.startsWith("OpenD")) {
            return true;
        }
        if (string.startsWith("HighD")) {
            return true;
        }
        if (string.startsWith("LowD")) {
            return true;
        }
        if (string.startsWith("CloseD")) {
            return true;
        }
        if (string.startsWith("OpenW")) {
            return true;
        }
        if (string.startsWith("HighW")) {
            return true;
        }
        if (string.startsWith("LowW")) {
            return true;
        }
        if (string.startsWith("CloseW")) {
            return true;
        }
        if (string.startsWith("OpenM")) {
            return true;
        }
        if (string.startsWith("HighM")) {
            return true;
        }
        if (string.startsWith("LowM")) {
            return true;
        }
        return string.startsWith("CloseM");
    }
}

