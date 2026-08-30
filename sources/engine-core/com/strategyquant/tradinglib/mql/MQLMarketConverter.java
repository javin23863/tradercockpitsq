package com.strategyquant.tradinglib.mql;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQLMarketConverter {
   public static final Logger Log = LoggerFactory.getLogger(MQLMarketConverter.class);
   private static final String iCustomStr = "iCustom";
   private static final String volumeCheckFn = " bool CheckVolumeValue(double volume)\r\n  {\r\n//--- minimal allowed volume for trade operations\r\n   double min_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN);\r\n   if(volume<min_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is less than the minimal allowed SYMBOL_VOLUME_MIN=%.2f\",min_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- maximal allowed volume of trade operations\r\n   double max_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MAX);\r\n   if(volume>max_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is greater than the maximal allowed SYMBOL_VOLUME_MAX=%.2f\",max_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- get minimal step of volume changing\r\n   double volume_step=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_STEP);\r\n\r\n   int ratio=(int)MathRound(volume/volume_step);\r\n   if(MathAbs(ratio*volume_step-volume)>0.0000001)\r\n     {\r\n      Verbose(StringFormat(\"Volume is not a multiple of the minimal step SYMBOL_VOLUME_STEP=%.2f, the closest correct volume is %.2f\",\r\n                               volume_step,ratio*volume_step));\r\n      return(false);\r\n     }\r\n   return(true);\r\n  }";

   public static String generateMT4SourceCode(String var0, String var1, String var2, String var3, String var4) throws Exception {
      Log.info("Generating MT4 source code for MQL market...");
      return generateMTSourceCode(var0, false, var1, var2, var3, var4);
   }

   public static String generateMT5SourceCode(String var0, String var1, String var2, String var3, String var4) throws Exception {
      Log.info("Generating MT5 source code for MQL market...");
      return generateMTSourceCode(var0, true, var1, var2, var3, var4);
   }

   private static String generateMTSourceCode(String var0, boolean var1, String var2, String var3, String var4, String var5) throws Exception {
      int var6 = var0.indexOf("#property");
      if (var6 < 0) {
         var6 = 0;
      }

      var0 = var0.replaceAll("(?m)^#property.*", "");
      String[] var7 = new String[]{"copyright", "link", "version", "description"};
      String[] var8 = new String[]{var2, var3, var4, var5};
      String var9 = "";

      for (int var10 = 0; var10 < var7.length; var10++) {
         var9 = var9 + "#property " + var7[var10] + " \"" + var8[var10] + "\"\n";
      }

      var9 = var9 + "#property strict\n";
      var0 = var0.substring(0, var6) + var9 + var0.substring(var6);
      String var22 = var1 ? "" : "extern ";
      var0 = var0.replace(var22 + "bool sqDisplayInfoPanel = true;", "bool sqDisplayInfoPanel = false;");
      ArrayList var11 = getUsedIndicators(var0);
      String var12 = "\n";
      int var13 = var0.indexOf("#property strict") + "#property strict".length();
      if (var13 < 0) {
         throw new Exception("Cannot add resources into the EA code - #property strict line not found");
      }

      for (int var14 = 0; var14 < var11.size(); var14++) {
         String var15 = (String)var11.get(var14) + "." + (var1 ? "ex5" : "ex4");
         var12 = var12 + "\n#resource \"" + var15 + "\"";
         var0 = var0.replace("\"" + (String)var11.get(var14) + "\"", "\"::" + var15 + "\"");
      }

      var0 = var0.substring(0, var13) + var12 + var0.substring(var13);
      int var23 = var0.indexOf(var1 ? "ulong openPosition(" : "int sqOpenOrder(");
      if (var23 < 0) {
         throw new Exception("Cannot find " + (var1 ? "openPosition" : "sqOpenOrder") + " function");
      }

      String var24 = var0.substring(0, var23);
      String var16 = var0.substring(var23);
      var24 = var24
         + " bool CheckVolumeValue(double volume)\r\n  {\r\n//--- minimal allowed volume for trade operations\r\n   double min_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MIN);\r\n   if(volume<min_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is less than the minimal allowed SYMBOL_VOLUME_MIN=%.2f\",min_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- maximal allowed volume of trade operations\r\n   double max_volume=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_MAX);\r\n   if(volume>max_volume)\r\n     {\r\n      Verbose(StringFormat(\"Volume is greater than the maximal allowed SYMBOL_VOLUME_MAX=%.2f\",max_volume));\r\n      return(false);\r\n     }\r\n\r\n//--- get minimal step of volume changing\r\n   double volume_step=SymbolInfoDouble(Symbol(),SYMBOL_VOLUME_STEP);\r\n\r\n   int ratio=(int)MathRound(volume/volume_step);\r\n   if(MathAbs(ratio*volume_step-volume)>0.0000001)\r\n     {\r\n      Verbose(StringFormat(\"Volume is not a multiple of the minimal step SYMBOL_VOLUME_STEP=%.2f, the closest correct volume is %.2f\",\r\n                               volume_step,ratio*volume_step));\r\n      return(false);\r\n     }\r\n   return(true);\r\n  }\n";
      if (var1) {
         var16 = var16.replace("if(volume <= 0) return (0);", "if(volume <= 0 || !CheckVolumeValue(volume)) return (0);");
      } else {
         var16 = var16.replace("if(size <= 0) return (0);", "if(size <= 0 || !CheckVolumeValue(size)) return (0);");
      }

      return var24 + var16;
   }

   public static String lockMT4(String var0, double var1, boolean var3, String var4, int var5) throws Exception {
      if (var1 > 0.0) {
         int var6 = var0.indexOf("extern double mmLots");
         if (var6 < 0) {
            throw new Exception("mmLots setting not found");
         }

         int var7 = var0.indexOf(";", var6);
         if (var7 < 0) {
            throw new Exception("Cannot find the end of mmLots line");
         }

         var0 = var0.substring(0, var6) + "double mmLots = " + var1 + ";" + var0.substring(var7 + 1);
      }

      int var11 = var0.indexOf("VerboseLog(\"Starting the EA\");");
      if (var11 < 0) {
         throw new Exception("Lock begin position not found");
      }

      var11 += "VerboseLog(\"Starting the EA\");".length();
      String var13 = var0.substring(0, var11 + 1);
      String var8 = var0.substring(var11);
      if (var3) {
         var13 = var13 + "\nif(!IsDemo()){ VerboseLog(\"This EA can trade only on demo accounts!\"); return(INIT_FAILED); }";
      }

      if (var4 != null) {
         int var9 = var13.indexOf("void OnTick() {");
         if (var9 < 0) {
            throw new Exception("OnTick function not found");
         }

         var9 += "void OnTick() {".length();
         String var10 = "if(iTime(NULL, 0, 0) >= StringToTime(\""
            + var4
            + "\")){ Alert(\"This EA is allowed to trade only until "
            + var4
            + "!\"); ExpertRemove(); return; }";
         var13 = var13.substring(0, var9) + "\n" + var10 + var13.substring(var9);
      }

      if (var5 > 0) {
         String var15 = "if(AccountNumber() != " + var5 + "){ VerboseLog(\"This EA is locked to account " + var5 + "!\"); return(INIT_FAILED); };";
         var13 = var13 + "\n" + var15;
      }

      return var13 + var8;
   }

   public static String lockMT5(String var0, double var1, boolean var3, String var4, int var5) throws Exception {
      if (var1 > 0.0) {
         int var6 = var0.indexOf("input double mmLots");
         if (var6 < 0) {
            throw new Exception("mmLots setting not found");
         }

         int var7 = var0.indexOf(";", var6);
         if (var7 < 0) {
            throw new Exception("Cannot find the end of mmLots line");
         }

         var0 = var0.substring(0, var6) + "double mmLots = " + var1 + ";" + var0.substring(var7 + 1);
      }

      int var11 = var0.indexOf("VerboseLog(\"Starting the EA\");");
      if (var11 < 0) {
         throw new Exception("Lock begin position not found");
      }

      var11 += "VerboseLog(\"Starting the EA\");".length();
      String var13 = var0.substring(0, var11 + 1);
      String var8 = var0.substring(var11);
      if (var3) {
         var13 = var13
            + "\nif(AccountInfoInteger(ACCOUNT_TRADE_MODE) != ACCOUNT_TRADE_MODE_DEMO){ VerboseLog(\"This EA can trade only on demo accounts!\"); return(INIT_FAILED); }";
      }

      if (var4 != null) {
         int var9 = var13.indexOf("void OnTick() {");
         if (var9 < 0) {
            throw new Exception("OnTick function not found");
         }

         var9 += "void OnTick() {".length();
         String var10 = "if(getTime(0) >= StringToTime(\""
            + var4
            + "\")){ Alert(\"This EA is allowed to trade only until "
            + var4
            + "!\"); ExpertRemove(); return; }";
         var13 = var13.substring(0, var9) + "\n" + var10 + var13.substring(var9);
      }

      if (var5 > 0) {
         String var15 = "if(AccountInfoInteger(ACCOUNT_LOGIN) != "
            + var5
            + "){ VerboseLog(\"This EA is locked to account "
            + var5
            + "!\"); return(INIT_FAILED); };";
         var13 = var13 + "\n" + var15;
      }

      return var13 + var8;
   }

   public static void compileMT4EA(String var0, String var1, String var2, String var3) throws Exception {
      compileEA(var0, false, var1, var2, var3);
   }

   public static void compileMT5EA(String var0, String var1, String var2, String var3) throws Exception {
      compileEA(var0, true, var1, var2, var3);
   }

   private static void compileEA(String var0, boolean var1, String var2, String var3, String var4) throws Exception {
      File var5 = null;
      Log.info("Compiling MQL Market MT4 EA..");
      Log.info(" - mtExpertsPath: " + var2);
      Log.info(" - mtInstallPath: " + var3);
      Log.info(" - eaName: " + var4);
      String var6 = var1 ? "mq5" : "mq4";
      String var7 = var1 ? "ex5" : "ex4";

      try {
         if (var4.endsWith("." + var7)) {
            var4 = var4.replace("." + var7, "");
         }

         if (!new File(var2).exists()) {
            throw new Exception("Selected experts path (" + var2 + ") does not exist");
         }

         var5 = new File(var2 + "/" + var4 + "_temp");
         if (!var5.exists() && !var5.mkdirs()) {
            throw new Exception("Cannot create temp folder in experts directory");
         }

         File var8 = new File(MainApp.getDataPath() + "custom_indicators/" + (var1 ? "MetaTrader5" : "MetaTrader4") + "/Indicators/");
         if (!var8.exists()) {
            throw new Exception("SQ Indicators folder '" + var8.getAbsolutePath() + "' not found");
         }

         File[] var9 = var8.listFiles();

         for (int var10 = 0; var10 < var9.length; var10++) {
            File var11 = var9[var10];
            String var12 = var11.getName();
            Log.info("Copying indicator " + var11.getName() + "...");
            Files.copy(var11.toPath(), new File(var5.getAbsolutePath() + "/" + var12).toPath());
         }

         String var18 = var5.getAbsolutePath() + "/" + var4 + "." + var6;
         SQUtils.stringToFile(var18, var0);
         String var19 = var3 + "/metaeditor.exe";
         if (!new File(var19).exists()) {
            var19 = var3 + "/metaeditor64.exe";
            if (!new File(var19).exists()) {
               throw new Exception("Metaeditor not found. Please check if MetaTrader installation folder path is correct");
            }
         }

         String var20 = var19 + " /compile:\"" + var18 + "\" /log";
         Log.info("---- Running command: " + var20);
         Process var13 = Runtime.getRuntime().exec(var20);
         if (!var13.waitFor(60L, TimeUnit.SECONDS)) {
            Log.error("Command '" + var20 + "' failed!");
            throw new Exception("MQL Generation TIMED OUT (compile command)");
         }

         File var14 = new File(var5.getAbsolutePath() + "/" + var4 + "." + var7);
         if (!var14.exists()) {
            throw new Exception("EA compilation failed - target file doesn't exist");
         }

         Files.copy(var14.toPath(), new File(var2 + "/" + var4 + "." + var7).toPath());
         Log.info("MQL Market EA created successfully");
      } finally {
         if (var5 != null && !SQUtils.deleteDirectory(var5.getAbsolutePath())) {
            Log.warn("Temp directory was not fully removed");
         }
      }
   }

   private static ArrayList<String> getUsedIndicators(String var0) {
      if (var0 == null) {
         return null;
      }

      ArrayList var1 = new ArrayList();

      for (int var2 = var0.indexOf("iCustom", 0); var2 > 0; var2 = var0.indexOf("iCustom", var2 + 1)) {
         int var3 = var0.indexOf("(", var2);
         if (var3 < 0) {
            break;
         }

         int var4 = Math.min(var2 + 100, var0.length() - 1);
         String var5 = var0.substring(var3, var4);
         String[] var6 = var5.split(",");
         String var7 = var6[2].trim().substring(1);
         var4 = var7.indexOf("\"");
         if (var4 < 0) {
            break;
         }

         var7 = var7.substring(0, var4).replace("'", "");
         if (!var1.contains(var7)) {
            var1.add(var7);
         }
      }

      return var1;
   }
}
