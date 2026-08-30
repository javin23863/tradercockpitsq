/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.G8SyrBEfO8;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.sourcecode.CheckMacroExistsMethod;
import com.strategyquant.lib.sourcecode.ConditionsFixerDirective;
import com.strategyquant.lib.sourcecode.CustomBlocksAnalyzer;
import com.strategyquant.lib.sourcecode.ListFilesMethod;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import freemarker.core.StringArraySequence;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class SourceCodeGenerator {
    public static final String MetaTrader4 = L.tsq("Expert Advisor for MetaTrader4 (*.MQ4)");
    public static final String MetaTrader5 = L.tsq("Expert Advisor for MetaTrader5 (*.MQ5)");
    public static final String MetaTrader5BR = L.tsq("Expert Advisor for MetaTrader5 (Brazil) (*.MQ5)");
    public static final String EasyLanguage = L.tsq("EasyLanguage for Tradestation / MultiCharts (*.el)");
    public static final String PseudoCode = L.tsq("Pseudo Code(*.TXT)");
    public static final String JForex = L.tsq("Expert Advisor for JForex (*.java)");
    public static final String StrategyXML = L.tsq("Strategy XML");
    public static final Logger Log = LoggerFactory.getLogger((String)"SourceCodeGenerator");
    public static final int TYPE_ENTRY = 1;
    public static final int TYPE_ORDER_TYPE = 2;
    public static final int TYPE_EXIT = 3;
    public boolean putValuesToParameters = false;
    File codeDir;
    String name = null;
    String[] extension = null;
    String description = "";
    private Configuration cfg;
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean isBrazilianVersion = false;

    public SourceCodeGenerator(File file, Configuration configuration) {
        this.codeDir = file;
        this.cfg = configuration;
        this.name = "Not specified [" + this.codeDir.getName() + "]";
        this.extension = new String[2];
        this.extension[0] = "Unknown";
        this.extension[1] = "txt";
        this.parseMainTemplate();
    }

    private void parseMainTemplate() {
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(this.codeDir.getAbsoluteFile() + "/Main.tpl")));
        }
        catch (FileNotFoundException fileNotFoundException) {
            return;
        }
        String string = "";
        int n = 0;
        while (++n <= 20) {
            String string2;
            block8: {
                try {
                    string2 = bufferedReader.readLine();
                    if (string2 != null && string2.equalsIgnoreCase("")) {
                        string2 = bufferedReader.readLine();
                    }
                    if (string2 == null || !string2.equalsIgnoreCase("")) break block8;
                    string2 = bufferedReader.readLine();
                }
                catch (IOException iOException) {
                    Log.error(iOException.getMessage(), (Throwable)iOException);
                    break;
                }
            }
            if (string2 == null) break;
            string = string + string2;
        }
        try {
            bufferedReader.close();
        }
        catch (IOException iOException) {
            return;
        }
        this.parseTags(string);
    }

    private void parseTags(String string) {
        this.name = this.getStringBetween(string, "<name>", "</name>");
        this.extension[0] = this.getStringBetween(string, "<extensionName>", "</extensionName>");
        if (this.extension[0] == null) {
            this.extension[0] = this.name;
        }
        this.extension[1] = this.getStringBetween(string, "<extension>", "</extension>");
        this.description = this.getStringBetween(string, "<description>", "</description>");
        if (this.description != null) {
            this.description = this.description.replaceAll("( +)", " ");
            this.description = this.description.replaceAll("&lt;", "<");
            this.description = this.description.replaceAll("&gt;", ">");
        }
    }

    private String getStringBetween(String string, String string2, String string3) {
        int n = string.indexOf(string2) + string2.length();
        if (n < 0) {
            return null;
        }
        int n2 = string.indexOf(string3, n);
        if (n2 < 0 || n2 <= n) {
            return null;
        }
        return string.substring(n, n2);
    }

    public String[] getFileExtension() {
        return this.extension;
    }

    public String getSource(String string, Element element) throws Exception {
        return this.getSource(string, element, 1.0, 0.0);
    }

    public String getSource(String string, Element element, double d, double d2) throws Exception {
        Object[] objectArray;
        Object object;
        String string2;
        Element element2;
        Object object2;
        Object object3;
        String string3 = this.codeDir.getName();
        Template template = this.cfg.getTemplate(string3 + File.separator + "Main.tpl");
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("checkMacroExists", new CheckMacroExistsMethod());
        hashMap.put("listFiles", new ListFilesMethod());
        hashMap.put("conditionsFixer", new ConditionsFixerDirective());
        hashMap.put("orderSizeMultiplier", new SimpleScalar("" + d));
        hashMap.put("orderSizeStep", new SimpleScalar("" + d2));
        Element element3 = element.clone();
        Element element4 = element3.getChild("Strategy");
        element4.setAttribute("StrategyName", string);
        element4.setAttribute("Version", "4 Alpha version 1");
        element4.setAttribute("Engine", "MetaTrader4");
        try {
            object3 = XMLUtil.getChildElem(element4, "Variables");
            object2 = object3.getChildren("variable");
            for (int i = object2.size() - 1; i >= 0; --i) {
                element2 = (Element)object2.get(i);
                string2 = null;
                try {
                    string2 = XMLUtil.getNodeValue(element2, "paramType");
                }
                catch (Exception exception) {
                    // empty catch block
                }
                if (string2 == null || !string2.equals("ParamTypeTradingOptions")) continue;
                object3.removeContent((Content)element2);
            }
        }
        catch (Exception exception) {
            Log.error("Error while trying to remove trading options variables", (Throwable)exception);
        }
        object3 = new Date();
        object2 = sdfDate.format((Date)object3);
        element4.setAttribute("Date", (String)object2);
        XMLIndicatorsAnalyzer.fixItemAttributes(element4);
        CustomBlocksAnalyzer.fixCBParameters(element4);
        element3.setAttribute("hasHeikenAshi", XMLUtil.elementToString(element3).contains("HeikenAshi") ? "true" : "false");
        element3.setAttribute("isBrazilianVersion", this.isBrazilianVersion ? "true" : "false");
        Document document = new Document(element3);
        element2 = new XMLOutputter(Format.getPrettyFormat());
        string2 = element2.outputString(document);
        string2 = string2.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ").replace("> <", "><");
        SQUtils.ensureDirExists(MainApp.getDataPath() + "/internal/tmp");
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(new StringReader(string2));
        hashMap.put("doc", NodeModel.parse((InputSource)inputSource));
        String[] stringArray = null;
        try {
            object = new File(SQStructure.INTERNAL_DIR_PATH + "/extend/Code/" + string3 + "/CustomFunctions").listFiles();
            objectArray = new File(SQPaths.userDirPath + "/extend/Code/" + string3 + "/CustomFunctions").listFiles();
            File[] fileArray = (File[])ArrayUtils.addAll((Object[])object, (Object[])objectArray);
            if (fileArray != null && fileArray.length > 0) {
                stringArray = new String[fileArray.length];
                for (int i = 0; i < fileArray.length; ++i) {
                    stringArray[i] = "/" + string3 + "/CustomFunctions/" + fileArray[i].getName();
                }
            }
        }
        catch (Exception exception) {
            Log.error("Cannot include custom functions", (Throwable)exception);
        }
        hashMap.put("includeFiles", new StringArraySequence(stringArray != null ? stringArray : new String[]{}));
        object = new StringWriter();
        template.process(hashMap, (Writer)object);
        objectArray = ((StringWriter)object).toString();
        if (string3.equals("MetaTrader4")) {
            return this.lockForCurrentBroker((String)objectArray, true);
        }
        if (string3.equals("MetaTrader5")) {
            return this.lockForCurrentBroker((String)objectArray, false);
        }
        return objectArray;
    }

    private void printVar(StringBuffer stringBuffer, HashMap<String, String> hashMap, ArrayList<String> arrayList, String string, String string2) {
        String string3;
        if (hashMap.containsKey(string)) {
            string3 = hashMap.get(string);
        } else if (string.equals("Enter_Stop")) {
            string3 = hashMap.get("Enter_Type");
            if (string3.equals("2")) {
                string3 = "0";
            }
        } else if (string.equals("Enter_Limit")) {
            string3 = hashMap.get("Enter_Type");
            if (string3.equals("1")) {
                string3 = "0";
            } else if (string3.equals("2")) {
                string3 = "1";
            }
        } else {
            string3 = "!!!!!!!!!!!!!!!!!!Variable '" + string + "' not found!";
        }
        stringBuffer.append("\t");
        for (int i = 0; i < arrayList.size(); ++i) {
            if (!arrayList.get(i).contains(string)) continue;
            stringBuffer.append("//");
        }
        stringBuffer.append(string);
        stringBuffer.append("(");
        stringBuffer.append(string3);
        stringBuffer.append("),");
        stringBuffer.append(string2);
        stringBuffer.append("\n\t");
    }

    private HashMap<String, String> getVarValuesFromStr(Element element) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        List list = element.getChild("Strategy").getChild("Variables").getChildren("variable");
        for (Element element2 : list) {
            String string = element2.getChild("name").getText();
            String string2 = element2.getChild("value").getText();
            hashMap.put(string, string2);
        }
        return hashMap;
    }

    private String lockForCurrentBroker(String string, boolean bl) throws Exception {
        AbstractBroker abstractBroker = G8SyrBEfO8.getInstance().getBroker();
        if (abstractBroker != null && abstractBroker.getCheckType() != -1) {
            int n = string.indexOf("void OnTick() {");
            if (n < 0) {
                throw new Exception(L.t("Cannot find start/OnTick method in generated MQL code", new Object[0]));
            }
            String string2 = string.substring(0, n += "void OnTick() {".length());
            String string3 = string.substring(n);
            String string4 = abstractBroker.getName();
            String string5 = "";
            if (abstractBroker.getCheckType() == 1) {
                string5 = "\nif(StringFind(" + (bl ? "AccountCompany()" : "AccountInfoString(ACCOUNT_COMPANY)") + ", \"" + abstractBroker.getCheckString() + "\") < 0) {\n";
                string5 = string5 + "if(firstCall) { firstCall = false; Print(\"This EA can be traded only with " + string4 + " broker !\"); Alert(\"This EA can be traded only with " + string4 + " broker, your broker is \" + " + (bl ? "AccountCompany()" : "AccountInfoString(ACCOUNT_COMPANY)") + " + \" !\"); }\n";
                string5 = string5 + "return;}\n";
            } else {
                string5 = "\nif(StringFind(" + (bl ? "AccountServer()" : "AccountInfoString(ACCOUNT_SERVER)") + ", \"" + abstractBroker.getCheckString() + "\") < 0) {\n";
                string5 = string5 + "if(firstCall) { firstCall = false; Print(\"This EA can be traded only with " + string4 + " broker !\"); Alert(\"This EA can be traded only with " + string4 + " broker, your broker is \" + " + (bl ? "AccountServer()" : "AccountInfoString(ACCOUNT_SERVER)") + " + \" !\"); }\n";
                string5 = string5 + "return;}\n";
            }
            Log.info("Protection code: " + string5);
            return string2 + string5 + string3;
        }
        return string;
    }

    public void setName(String string) {
        this.name = string;
    }

    public String getName() {
        return this.name;
    }

    public String getDirName() {
        return this.codeDir.getName();
    }

    public void addDirectoryToName() {
        this.name = this.name + " [" + this.codeDir.getName() + "]";
    }

    public String getDescription() {
        return this.description;
    }

    public String getKey() {
        return this.getDirName();
    }

    public void setBrazilianVersion(boolean bl) {
        this.isBrazilianVersion = bl;
    }

    public boolean isBrazilianVersion() {
        return this.isBrazilianVersion;
    }
}

