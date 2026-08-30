/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sqxbusiness.EAOption;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildResult;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildSettings;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import com.strategyquant.lib.sqxbusiness.MQLMarketLoggable;
import com.strategyquant.lib.sqxbusiness.SQXBusinessMainSettings;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.sf.image4j.codec.ico.ICOEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQLMarketBuilder {
    private static final Logger Log = LoggerFactory.getLogger((String)"MQLMarketBuilder");
    private static final String iCustomStr = "iCustom";
    private static final String volumeCheckFn = " bool CheckVolumeValue(double volume)\r\n  {\r\n//--- minimal allowed volume for trade operations\r\n   double min_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN);\r\n   if(volume<min_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is less than the minimal allowed SYMBOL_VOLUME_MIN=%.2f\",min_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- maximal allowed volume of trade operations\r\n   double max_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MAX);\r\n   if(volume>max_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is greater than the maximal allowed SYMBOL_VOLUME_MAX=%.2f\",max_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- get minimal step of volume changing\r\n   double volume_step=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_STEP);\r\n\r\n   int ratio=(int)MathRound(volume/volume_step);\r\n   if(MathAbs(ratio*volume_step-volume)>0.0000001)\r\n     {\r\n      Verbose(StringFormat(\"Volume is not a multiple of the minimal step SYMBOL_VOLUME_STEP=%.2f, the closest correct volume is %.2f\",\r\n                               volume_step,ratio*volume_step));\r\n      return(false);\r\n     }\r\n   return(true);\r\n  }";
    private static final String MQL4TestOrderCode = "   if(IsTesting() && CheckMoneyForTrade(Symbol(),SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN),OP_BUY)){\r\n      Print(\"Opening MQL market restriction test order...\");\r\n      int ticket=OrderSend(Symbol(),OP_BUY,SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN),Ask,10,NormalizeDouble(Bid-50*_Point,_Digits),NormalizeDouble(Ask+50*_Point,_Digits));\r\n      if(ticket<0){\r\n         PrintFormat(\"OrderSend error %d\",GetLastError());\r\n      }\r\n      Print(\"  --------------------------- \");\r\n    }";
    private static final String MarginCheckFn_MT4 = "bool CheckMoneyForTrade(string symb, double lots,int type)\r\n  {\r\n   int correctedType = (type==OP_BUY || type==OP_BUYSTOP || type==OP_BUYLIMIT) ? OP_BUY : OP_SELL;\r\n   double free_margin=AccountFreeMarginCheck(symb,correctedType, lots);\r\n   //-- if there is not enough money\r\n   if(free_margin<0)\r\n     {\r\n      string oper=(correctedType==OP_BUY)? \"Buy\":\"Sell\";\r\n      Print(\"Not enough money for \", oper,\" \",lots, \" \", symb, \" Error code=\",GetLastError());\r\n      return(false);\r\n     }\r\n   //--- checking successful\r\n   return(true);\r\n  }";
    private static final String MarginCheckFn_MT5 = "bool CheckMoneyForTrade(string symb,double lots,ENUM_ORDER_TYPE type)\r\n  {\r\n//--- Getting the opening price\r\n   MqlTick mqltick;\r\n   SymbolInfoTick(symb,mqltick);\r\n   double price=mqltick.ask;\r\n   if(type==ORDER_TYPE_SELL)\r\n      price=mqltick.bid;\r\n//--- values of the required and free margin\r\n   double margin,free_margin=AccountInfoDouble(ACCOUNT_MARGIN_FREE);\r\n   //--- call of the checking function\r\n   if(!OrderCalcMargin(type,symb,lots,price,margin))\r\n     {\r\n      //--- something went wrong, report and return false\r\n      Print(\"Error in \",__FUNCTION__,\" code=\",GetLastError());\r\n      return(false);\r\n     }\r\n   //--- if there are insufficient funds to perform the operation\r\n   if(margin>free_margin)\r\n     {\r\n      //--- report the error and return false\r\n      Print(\"Not enough money for \",EnumToString(type),\" \",lots,\" \",symb,\" Error code=\",GetLastError());\r\n      return(false);\r\n     }\r\n//--- checking successful\r\n   return(true);\r\n  }";
    final String iconFileName = "ea_icon.ico";
    private String jobID;
    private AtomicBoolean paused = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    public MQLMarketBuilder(String string) {
        this.jobID = string;
    }

    public MQLMarketBuildResult build(MQLMarketBuildSettings mQLMarketBuildSettings, MQLMarketLoggable mQLMarketLoggable) {
        Object object;
        String string;
        boolean bl;
        String string2;
        if (!MainApp.v571hfnsHw().aDm88fRJB2() || MainApp.v571hfnsHw().a1wUchdumV()) {
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception("SQX business functionality is available for ultimate users only!"));
        }
        Log.info(String.format("Building MQLMarket EA - %s->%s (%s) / demoOnly: %s, fixedSize: %f, expirationDate: %s, account: %d", mQLMarketBuildSettings.projectName, mQLMarketBuildSettings.outputName, mQLMarketBuildSettings.platform, String.valueOf(mQLMarketBuildSettings.demoOnly), mQLMarketBuildSettings.fixedSize, mQLMarketBuildSettings.expirationDate, mQLMarketBuildSettings.account));
        switch (mQLMarketBuildSettings.platform) {
            case "mql4": {
                string2 = SourceCodeGenerator.MetaTrader4;
                bl = false;
                break;
            }
            case "mql5": {
                string2 = SourceCodeGenerator.MetaTrader5;
                bl = true;
                break;
            }
            default: {
                String string3 = "No generator found for platform '" + mQLMarketBuildSettings.platform + "'";
                Log.error(string3);
                return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception(string3));
            }
        }
        mQLMarketLoggable._printToLog(this.jobID, L.t("Getting source generator...", new Object[0]));
        Object object2 = SourceCodeGenerators.getInstance().getGeneratorFromName(string2);
        if (object2 == null) {
            String string4 = L.t("Source code generator '%s' not found", string2);
            Log.error(string4);
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception(string4));
        }
        String string5 = MQLMarketConst.getProjectDirPath(mQLMarketBuildSettings.projectName);
        String string6 = string = bl ? SQXBusinessMainSettings.getMT5Path() : SQXBusinessMainSettings.getMT4Path();
        if (string == null || string.isEmpty()) {
            String string7 = L.t("MetaTrader %d installation path not set", bl ? 5 : 4);
            Log.error(string7);
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception(string7));
        }
        if (!new File(string).exists()) {
            String string8 = L.t("MetaTrader %d installation directory doesn't exist. Please check your settings", bl ? 5 : 4);
            Log.error(string8);
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception(string8));
        }
        String[] stringArray = null;
        File file = new File(string5 + "/resources");
        if (file.exists()) {
            object = file.listFiles();
            stringArray = new String[((File[])object).length];
            for (int i = 0; i < ((File[])object).length; ++i) {
                File file2 = object[i];
                stringArray[i] = file2.getName();
            }
        }
        if (this.stopped.get()) {
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception("Stopped by user"));
        }
        this.checkPaused();
        try {
            mQLMarketLoggable._printToLog(this.jobID, L.t("Generating source code...", new Object[0]));
            object = ((SourceCodeGenerator)object2).getSource(mQLMarketBuildSettings.outputName, mQLMarketBuildSettings.strategyXML);
            if (this.stopped.get()) {
                return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception("Stopped by user"));
            }
            this.checkPaused();
            mQLMarketLoggable._printToLog(this.jobID, L.t("Converting to MQL market format...", new Object[0]));
            String string9 = this.generateMTSourceCode((String)object, bl, stringArray, mQLMarketBuildSettings.copyright, mQLMarketBuildSettings.link, mQLMarketBuildSettings.version, mQLMarketBuildSettings.description, mQLMarketBuildSettings.comment, mQLMarketBuildSettings.eaOptions);
            if (this.stopped.get()) {
                return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception("Stopped by user"));
            }
            this.checkPaused();
            mQLMarketLoggable._printToLog(this.jobID, L.t("Trying to lock the EA based on settings...", new Object[0]));
            string9 = this.lockEACode(bl, string9, mQLMarketBuildSettings.fixedSize, mQLMarketBuildSettings.demoOnly, mQLMarketBuildSettings.expirationDate, mQLMarketBuildSettings.account);
            string9 = string9.replace("bool sqDisplayInfoPanel = MQLInfoInteger(MQL_TESTER) == 0 && MQLInfoInteger(MQL_OPTIMIZATION) == 0;", "bool sqDisplayInfoPanel = false;");
            if (this.stopped.get()) {
                return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, new Exception("Stopped by user"));
            }
            this.checkPaused();
            mQLMarketLoggable._printToLog(this.jobID, L.t("Compiling the EA...", new Object[0]));
            this.compileEA(string9, bl, string5, string, mQLMarketBuildSettings.outputName);
        }
        catch (Exception exception) {
            Log.error("Generating MQL market strategy failed - " + exception.getClass().getSimpleName() + ": " + exception);
            return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, exception);
        }
        return new MQLMarketBuildResult(mQLMarketBuildSettings.projectName, null);
    }

    public void pause() {
        this.paused.set(true);
    }

    public void resume() {
        this.paused.set(false);
    }

    public void stop() {
        this.stopped.set(true);
    }

    private void checkPaused() {
        while (!this.stopped.get() && this.paused.get()) {
            try {
                Thread.sleep(100L);
            }
            catch (Exception exception) {}
        }
    }

    private String generateMTSourceCode(String string, boolean bl, String[] stringArray, String string2, String string3, String string4, String string5, String string6, ArrayList<EAOption> arrayList) throws Exception {
        int n;
        int n2;
        String string7;
        int n3;
        Object object;
        int n4 = string.indexOf("#property");
        if (n4 < 0) {
            n4 = 0;
        }
        string = string.replaceAll("(?m)^#property.*", "");
        String[] stringArray2 = new String[]{"copyright", "link", "version", "description"};
        String[] stringArray3 = new String[]{string2, string3, string4, string5};
        String string8 = "";
        for (int i = 0; i < stringArray2.length; ++i) {
            object = this.removeSpecialCharacters(stringArray3[i]);
            if (stringArray2[i].equals("version")) {
                object = ((String)object).replace(",", ".");
            }
            string8 = string8 + "#property " + stringArray2[i] + " \"" + (String)object + "\"\n";
        }
        string8 = string8 + "#property strict\n";
        string = string.substring(0, n4) + string8 + string.substring(n4);
        String string9 = bl ? "" : "extern ";
        string = string.replace(string9 + "bool sqDisplayInfoPanel = true;", "bool sqDisplayInfoPanel = false;");
        object = this.getUsedIndicators(string);
        String string10 = "\n#property icon \"ea_icon.ico\"\n";
        int n5 = string.indexOf("#property strict") + "#property strict".length();
        if (n5 < 0) {
            throw new Exception("Cannot add resources into the EA code - #property strict line not found");
        }
        if (stringArray != null) {
            for (n3 = 0; n3 < stringArray.length; ++n3) {
                string7 = stringArray[n3].trim();
                if (!string7.endsWith(".ex5") && !string7.trim().endsWith(".ex4")) {
                    throw new Exception(String.format("Invalid resource '%s', only .ex4 and .ex5 files are supported", string7));
                }
                if (bl && string7.trim().endsWith(".ex5")) {
                    string7 = string7.substring(0, string7.length() - 4);
                    ((ArrayList)object).add(string7);
                    continue;
                }
                if (!string7.trim().endsWith(".ex4")) continue;
                string7 = string7.substring(0, string7.length() - 4);
                ((ArrayList)object).add(string7);
            }
        }
        for (n3 = 0; n3 < ((ArrayList)object).size(); ++n3) {
            string7 = (String)((ArrayList)object).get(n3) + "." + (bl ? "ex5" : "ex4");
            string10 = string10 + "\n#resource \"" + string7 + "\"";
            string = string.replace("\"" + (String)((ArrayList)object).get(n3) + "\"", "\"::" + string7 + "\"");
        }
        n3 = (string = string.substring(0, n5) + string10 + string.substring(n5)).indexOf(bl ? "ulong openPosition(" : "int sqOpenOrder(");
        if (n3 < 0) {
            throw new Exception("Cannot find " + (bl ? "openPosition" : "sqOpenOrder") + " function");
        }
        string7 = string.substring(0, n3);
        String string11 = string.substring(n3);
        string7 = string7 + " bool CheckVolumeValue(double volume)\r\n  {\r\n//--- minimal allowed volume for trade operations\r\n   double min_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN);\r\n   if(volume<min_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is less than the minimal allowed SYMBOL_VOLUME_MIN=%.2f\",min_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- maximal allowed volume of trade operations\r\n   double max_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MAX);\r\n   if(volume>max_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is greater than the maximal allowed SYMBOL_VOLUME_MAX=%.2f\",max_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- get minimal step of volume changing\r\n   double volume_step=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_STEP);\r\n\r\n   int ratio=(int)MathRound(volume/volume_step);\r\n   if(MathAbs(ratio*volume_step-volume)>0.0000001)\r\n     {\r\n      Verbose(StringFormat(\"Volume is not a multiple of the minimal step SYMBOL_VOLUME_STEP=%.2f, the closest correct volume is %.2f\",\r\n                               volume_step,ratio*volume_step));\r\n      return(false);\r\n     }\r\n   return(true);\r\n  }\n" + (bl ? MarginCheckFn_MT5 : MarginCheckFn_MT4);
        string11 = bl ? string11.replace("if(volume <= 0) return (0);", "if(!CheckVolumeValue(volume) || !CheckMoneyForTrade(correctSymbol(symbol), volume, type)) return (0);") : string11.replace("if(size <= 0) return (0);", "if(!CheckVolumeValue(size) || !CheckMoneyForTrade(correctSymbol(symbol), size, orderType)) return (0);");
        string = string7 + string11;
        if (!bl) {
            n2 = string.indexOf("int OnInit() {");
            if (n2 <= 0) {
                throw new Exception("Cannot find OnInit function in MQL");
            }
            string = string.replace("int OnInit() {", "int OnInit() {\n\n   if(IsTesting() && CheckMoneyForTrade(Symbol(),SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN),OP_BUY)){\r\n      Print(\"Opening MQL market restriction test order...\");\r\n      int ticket=OrderSend(Symbol(),OP_BUY,SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN),Ask,10,NormalizeDouble(Bid-50*_Point,_Digits),NormalizeDouble(Ask+50*_Point,_Digits));\r\n      if(ticket<0){\r\n         PrintFormat(\"OrderSend error %d\",GetLastError());\r\n      }\r\n      Print(\"  --------------------------- \");\r\n    }");
        }
        if ((n2 = string.indexOf("string CustomComment = \"")) > 0 && (n = string.indexOf("\"", n2 += "string CustomComment = \"".length())) > 0) {
            string = string.substring(0, n2) + string6 + string.substring(n);
        }
        if (arrayList != null && !arrayList.isEmpty()) {
            for (n = 0; n < arrayList.size(); ++n) {
                int n6;
                EAOption eAOption = arrayList.get(n);
                String string12 = bl ? "input" : "extern";
                String string13 = string12 + " " + eAOption.type + " " + eAOption.name + " ";
                int n7 = string.indexOf(string13);
                if (n7 < 0 || (n6 = string.indexOf(";", n7)) < 0) continue;
                String string14 = String.format("%s %s %s = %s", eAOption.enabled ? string12 : "", eAOption.type, eAOption.name, this.correctValue(eAOption.name, eAOption.type, eAOption.value));
                string = string.substring(0, n7) + string14 + string.substring(n6);
            }
        }
        return string;
    }

    private String correctValue(String string, String string2, String string3) throws Exception {
        switch (string2) {
            case "int": {
                try {
                    Integer.parseInt(string3);
                    return string3;
                }
                catch (Exception exception) {
                    throw new Exception("Invalid EA Option " + string + " = " + string3 + " -> " + string2 + " type expected");
                }
            }
            case "double": {
                try {
                    string3 = string3.replace(",", ".");
                    Double.parseDouble(string3);
                    return string3;
                }
                catch (Exception exception) {
                    throw new Exception("Invalid EA Option " + string + " = " + string3 + " -> " + string2 + " type expected");
                }
            }
            case "bool": {
                if ("true".equals(string3) || "false".equals(string3)) {
                    return string3;
                }
                throw new Exception("Invalid EA Option " + string + " = " + string3 + " -> true/false expected");
            }
            case "string": {
                return "\"" + string3.replace("\"", "") + "\"";
            }
        }
        return string3;
    }

    private String removeSpecialCharacters(String string) {
        return string.replace("\n", " ").replace("\r", "").replace("\"", "'");
    }

    private void compileEA(String string, boolean bl, String string2, String string3, String string4) throws Exception {
        File file = null;
        Log.info("Compiling MQL Market MT4 EA...");
        Log.info(" - projectPath: " + string2);
        Log.info(" - mtInstallPath: " + string3);
        Log.info(" - eaName: " + string4);
        String string5 = bl ? "mq5" : "mq4";
        String string6 = bl ? "ex5" : "ex4";
        String string7 = null;
        try {
            Object object;
            Object object2;
            File file2;
            if (string4.endsWith("." + string6)) {
                string4 = string4.replace("." + string6, "");
            }
            if (!(file2 = new File(string2)).exists()) {
                throw new Exception("Project directory (" + string2 + ") does not exist");
            }
            Path path = Files.createTempDirectory("wd-", new FileAttribute[0]);
            file = path.toFile();
            if (!file.exists() && !file.mkdirs()) {
                throw new Exception("Cannot create temp folder in project directory");
            }
            this.createIconFile(file2, file);
            File file3 = new File(MainApp.getDataPath() + "custom_indicators/" + (bl ? "MetaTrader5" : "MetaTrader4") + "/Indicators/");
            if (!file3.exists()) {
                throw new Exception("SQ Indicators folder '" + file3.getAbsolutePath() + "' not found");
            }
            if (this.stopped.get()) {
                throw new Exception("Stopped by user");
            }
            this.checkPaused();
            File[] fileArray = file3.listFiles();
            for (int i = 0; i < fileArray.length; ++i) {
                object2 = fileArray[i];
                object = ((File)object2).getName();
                Log.info("Copying indicator " + ((File)object2).getName() + "...");
                Files.copy(((File)object2).toPath(), new File(file.getAbsolutePath() + "/" + (String)object).toPath(), new CopyOption[0]);
            }
            string7 = file.getAbsolutePath() + "/" + string4 + "." + string5;
            SQUtils.stringToFile(string7, string);
            String string8 = string3 + "/metaeditor.exe";
            if (!new File(string8).exists() && !new File(string8 = string3 + "/metaeditor64.exe").exists()) {
                throw new Exception("Metaeditor not found. Please check if MetaTrader installation folder path is correct");
            }
            if (this.stopped.get()) {
                throw new Exception("Stopped by user");
            }
            this.checkPaused();
            object2 = string8 + " /compile:\"" + string7 + "\" /log";
            Log.info("---- Running command: " + (String)object2);
            object = Runtime.getRuntime().exec((String)object2);
            if (!((Process)object).waitFor(60L, TimeUnit.SECONDS)) {
                Log.error("Command '" + (String)object2 + "' failed!");
                throw new Exception("MQL Generation TIMED OUT (compile command)");
            }
            File file4 = new File(file.getAbsolutePath() + "/" + string4 + "." + string6);
            if (!file4.exists()) {
                String string9 = new File(string2 + "/" + "output").getAbsolutePath();
                string9 = string9.substring(new File(MainApp.getDataPath()).getAbsolutePath().length());
                throw new Exception(String.format("EA compilation failed, output file is missing. Check the compilation log at: %s", string9));
            }
            File file5 = new File(string2 + "/" + "output" + "/" + string4 + "." + string6);
            file5.getParentFile().mkdirs();
            try {
                Files.copy(file4.toPath(), file5.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception exception) {
                throw new Exception("Cannot copy the compiled EA into project folder");
            }
            Log.info("MQL Market EA created successfully");
        }
        catch (Exception exception) {
            Log.error("Build failed", (Throwable)exception);
            try {
                if (string7 != null) {
                    File file6 = new File(string7);
                    File file7 = new File(string2 + "/" + "output" + "/" + file6.getName());
                    File file8 = new File(string7.substring(0, string7.length() - 4) + ".log");
                    File file9 = new File(string2 + "/" + "output" + "/" + file8.getName().substring(0, file8.getName().length() - 4) + (bl ? "-mt5" : "-mt4") + ".log");
                    if (file8.exists()) {
                        try {
                            file9.getParentFile().mkdirs();
                            Files.copy(file8.toPath(), file9.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        catch (Exception exception2) {
                            Log.error("Failed to copy log file into project folder", (Throwable)exception2);
                        }
                    }
                }
            }
            catch (Exception exception3) {
                Log.error("Saving MQL and log file failed", (Throwable)exception3);
            }
            throw exception;
        }
        finally {
            if (file != null && !SQUtils.deleteDirectory(file.getAbsolutePath())) {
                Log.warn("Temp directory was not fully removed");
            }
        }
    }

    private ArrayList<String> getUsedIndicators(String string) {
        int n;
        if (string == null) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        int n2 = string.indexOf(iCustomStr, 0);
        while (n2 > 0 && (n = string.indexOf("(", n2)) >= 0) {
            int n3 = Math.min(n2 + 100, string.length() - 1);
            String string2 = string.substring(n, n3);
            String[] stringArray = string2.split(",");
            String string3 = stringArray[2].trim().substring(1);
            if ((n3 = string3.indexOf("\"")) >= 0 && !arrayList.contains(string3 = string3.substring(0, n3).replace("'", ""))) {
                arrayList.add(string3);
            }
            n2 = string.indexOf(iCustomStr, n2 + 1);
        }
        Log.info("Used custom indicators, #iCustom(" + arrayList.size() + "): " + String.join((CharSequence)",", arrayList));
        return arrayList;
    }

    private String lockEACode(boolean bl, String string, double d, boolean bl2, String string2, int n) throws Exception {
        if (bl) {
            return this.lockMT5(string, d, bl2, string2, n);
        }
        return this.lockMT4(string, d, bl2, string2, n);
    }

    private String lockMT4(String string, double d, boolean bl, String string2, int n) throws Exception {
        int n2;
        if (d > 0.0) {
            n2 = string.indexOf("extern double mmLots");
            if (n2 < 0) {
                throw new Exception("mmLots setting not found");
            }
            int n3 = string.indexOf(";", n2);
            if (n3 < 0) {
                throw new Exception("Cannot find the end of mmLots line");
            }
            string = string.substring(0, n2) + "double mmLots = " + d + ";" + string.substring(n3 + 1);
        }
        if ((n2 = string.indexOf("VerboseLog(\"Starting the EA\");")) < 0) {
            throw new Exception("Lock begin position not found");
        }
        String string3 = string.substring(0, (n2 += "VerboseLog(\"Starting the EA\");".length()) + 1);
        String string4 = string.substring(n2);
        if (bl) {
            string3 = string3 + "\nif(!IsTesting() && !IsDemo()){ Alert(\"This EA can trade only on demo accounts! Please contact strategy developer\"); return(INIT_FAILED); }";
        }
        if (string2 != null) {
            int n4 = string3.indexOf("void OnTick() {");
            if (n4 < 0) {
                throw new Exception("OnTick function not found");
            }
            String string5 = "if(!IsTesting() && iTime(NULL, 0, 0) >= StringToTime(\"" + string2.replace("-", ".") + " 00:00\")){ Alert(\"This EA is allowed to trade only until " + string2 + "! Please contact strategy developer\"); ExpertRemove(); return; }";
            string3 = string3.substring(0, n4 += "void OnTick() {".length()) + "\n" + string5 + string3.substring(n4);
        }
        if (n > 0) {
            String string6 = "if(!IsTesting() && AccountNumber() != " + n + "){ Alert(\"This EA is locked to account " + n + "! Please contact strategy developer\"); return(INIT_FAILED); };";
            string3 = string3 + "\n" + string6;
        }
        return string3 + string4;
    }

    private String lockMT5(String string, double d, boolean bl, String string2, int n) throws Exception {
        int n2;
        if (d > 0.0) {
            n2 = string.indexOf("input double mmLots");
            if (n2 < 0) {
                throw new Exception("mmLots setting not found");
            }
            int n3 = string.indexOf(";", n2);
            if (n3 < 0) {
                throw new Exception("Cannot find the end of mmLots line");
            }
            string = string.substring(0, n2) + "double mmLots = " + d + ";" + string.substring(n3 + 1);
        }
        if ((n2 = string.indexOf("VerboseLog(\"Starting the EA\");")) < 0) {
            throw new Exception("Lock begin position not found");
        }
        String string3 = string.substring(0, (n2 += "VerboseLog(\"Starting the EA\");".length()) + 1);
        String string4 = string.substring(n2);
        if (bl) {
            string3 = string3 + "\nif(MQLInfoInteger(MQL_TESTER) == 0 && AccountInfoInteger(ACCOUNT_TRADE_MODE) != ACCOUNT_TRADE_MODE_DEMO){ Alert(\"This EA can trade only on demo accounts! Please contact strategy developer\"); return(INIT_FAILED); }";
        }
        if (string2 != null) {
            int n4 = string3.indexOf("void OnTick() {");
            if (n4 < 0) {
                throw new Exception("OnTick function not found");
            }
            String string5 = "if(MQLInfoInteger(MQL_TESTER) == 0 && getTime(0) >= StringToTime(\"" + string2.replace("-", ".") + " 00:00\")){ Alert(\"This EA is allowed to trade only until " + string2 + "! Please contact strategy developer\"); ExpertRemove(); return; }";
            string3 = string3.substring(0, n4 += "void OnTick() {".length()) + "\n" + string5 + string3.substring(n4);
        }
        if (n > 0) {
            String string6 = "if(MQLInfoInteger(MQL_TESTER) == 0 && AccountInfoInteger(ACCOUNT_LOGIN) != " + n + "){ Alert(\"This EA is locked to account " + n + "! Please contact strategy developer\"); return(INIT_FAILED); };";
            string3 = string3 + "\n" + string6;
        }
        return string3 + string4;
    }

    private void createIconFile(File file, File file2) throws Exception {
        File[] fileArray = file.listFiles();
        File file3 = null;
        for (int i = 0; i < fileArray.length; ++i) {
            File file4 = fileArray[i];
            if (!SQUtils.stripExtension(file4.getName()).equals("logo")) continue;
            file3 = file4;
            break;
        }
        if (file3 == null) {
            throw new Exception("Project '" + file.getName() + "' -> Project logo not found");
        }
        Log.info("Project logo found -> " + file3.getName());
        try {
            BufferedImage bufferedImage = ImageIO.read(file3);
            ICOEncoder.write((BufferedImage)bufferedImage, (File)new File(file2.getAbsolutePath() + "/" + "ea_icon.ico"));
        }
        catch (Exception exception) {
            throw new Exception("Creating project logo icon failed", exception);
        }
    }
}

