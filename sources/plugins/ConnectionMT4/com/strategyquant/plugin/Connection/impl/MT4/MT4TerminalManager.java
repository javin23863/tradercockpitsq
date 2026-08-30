package com.strategyquant.plugin.Connection.impl.MT4;

import com.jfx.net.JFXServer;
import com.jfx.strategy.Strategy;
import com.jfx.ts.io.PSUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4TerminalManager {
   private static final Logger Log = LoggerFactory.getLogger(MT4TerminalManager.class);
   private static String curTerminalPath;
   private static String terminalsFolder;
   private static String zipPath;
   private static final String host = "127.0.0.1";
   private static final int port = 7777;
   private static HashMap<String, Integer> terminalPIDs = new HashMap<>();

   public static void init() throws Exception {
      System.setProperty("jfx_server_host", "127.0.0.1");
      System.setProperty("jfx_server_port", String.valueOf(7777));
      System.setProperty("nj4x_server_port", String.valueOf(7777));
      String var0 = System.getProperty("user.dir");
      terminalsFolder = var0 + "/internal/SQTRADER/terminals";
      zipPath = terminalsFolder + "/terminal.zip";
      JFXServer.getInstance();

      try {
         JFXServer.getInstance();
         Log.info("JFX server initialized at 127.0.0.1:7777");
      } catch (Exception var3) {
         String var2 = "JFX server initialization at 127.0.0.1:7777 failed.";
         Log.info(var2);
         throw new Exception(var2);
      }
   }

   public static void deinit() throws Exception {
      PSUtils.getInstance().deinit();
      JFXServer.stop();
   }

   public static void createTerminal(String var0, String var1, String var2, String var3, Strategy var4, boolean var5, boolean var6, boolean var7) throws Exception {
      if (!terminalExists(var3)) {
         createTerminalFolderFromZip(zipPath, terminalsFolder, var3);
         createMTInitFile(var0, var1, var2);
         createEASetFile("127.0.0.1", 7777, var3);
         startMetaTrader(var3, var5, var6);
      } else {
         startMetaTrader(var3, var5, var6);
      }
   }

   private static void createTerminalFolderFromZip(String var0, String var1, String var2) throws Exception {
      curTerminalPath = var1 + "/" + var2;
      Log.info("Creating terminal folder \"" + curTerminalPath + "\"");
      File var3 = new File(curTerminalPath);
      deleteDirectory(var3);
      if (!var3.exists() && !var3.mkdirs()) {
         throw new Exception("Cannot create folder \"" + var2 + "\" in \"" + var1 + "\"");
      }

      try {
         ZipFile var4 = new ZipFile(var0);
         var4.extractAll(curTerminalPath);
      } catch (ZipException var5) {
         throw new Exception("Cannot unzip file \"" + var0 + "\" into \"" + curTerminalPath + "\"");
      }
   }

   private static void startMetaTrader(String var0, boolean var1, boolean var2) {
      Log.info("Starting MT4...");
      String var3 = "\"" + curTerminalPath + "/terminal.exe\"";
      String var4 = "\"" + curTerminalPath + "/init.ini\"";
      String var5 = " /portable /skipupdate " + var4;
      boolean var6 = var2;
      if (var1) {
         var6 = false;
      }

      int var7 = PSUtils.getInstance().startProcess2(var3 + var5, curTerminalPath, var1, true, var6);
      terminalPIDs.put(var0, var7);
   }

   private static void createMTInitFile(String var0, String var1, String var2) throws Exception {
      String var3 = curTerminalPath + "/init.ini";

      try {
         PrintWriter var4 = new PrintWriter(var3);

         try {
            var4.println("; common settings");
            var4.println("Profile=default");
            var4.println("Login=" + var0);
            var4.println("Password=" + var1);
            var4.println("Server=" + var2);
            var4.println("AutoConfiguration=false");
            var4.println(";  DataServer=192.168.0.1:443");
            var4.println("EnableDDE=false");
            var4.println("EnableNews=false");
            var4.println("ProxyEnable=false");
            var4.println("ProxyServer=$ProxyServer$");
            var4.println("ProxyType=$ProxyType$");
            var4.println("ProxyLogin=$ProxyLogin$");
            var4.println("ProxyPassword=$ProxyPassword$");
            var4.println("; experts settings");
            var4.println("ExpertsEnable=true");
            var4.println("ExpertsDllImport=true");
            var4.println("ExpertsDllConfirm=false");
            var4.println("ExpertsExpImport=true");
            var4.println("ExpertsTrades=true");
            var4.println("ExpertsTradesConfirm=false");
            var4.println("Expert=jfx");
            var4.println("ExpertParameters=jfx_ea.set");
            var4.println("Symbol=EURUSD");
            var4.println("Period=H4");
            var4.flush();
            var4.close();
         } catch (Throwable var8) {
            try {
               var4.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var4.close();
      } catch (Exception var9) {
         throw new Exception("Error creating Strategy set file: \"" + var3 + "\"", var9);
      }
   }

   private static void createEASetFile(String var0, int var1, String var2) throws Exception {
      String var3 = curTerminalPath + "/MQL4/Presets/jfx_ea.set";

      try {
         PrintWriter var4 = new PrintWriter(var3);

         try {
            var4.println("jfxHost=" + var0);
            var4.println("jfxPort=" + var1);
            var4.println("strategy=" + var2);
            var4.println("param1=0.0");
            var4.println("param2=0.0");
            var4.println("param3=0.0");
            var4.println("param4=0.0");
            var4.println("param5=0.0");
            var4.println("param6=0.0");
            var4.println("param7=0.0");
            var4.println("param8=0.0");
            var4.println("param9=0.0");
            var4.println("param10=0.0");
            var4.println("DEBUG_DLL=false");
            var4.flush();
            var4.close();
         } catch (Throwable var8) {
            try {
               var4.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var4.close();
      } catch (Exception var9) {
         throw new Exception("Error creating Strategy set file: \"" + var3 + "\"", var9);
      }
   }

   public static void removeAllTerminals() throws Exception {
      for (String var1 : terminalPIDs.keySet()) {
         removeTerminal(var1);
      }
   }

   public static void removeTerminal(String var0) throws Exception {
      Log.info("Removing teminal \"" + var0 + "\"");
      if (terminalExists(var0)) {
         if (!killTerminalProcess(var0)) {
            throw new Exception("Cannot terminate terminal process");
         }

         terminalPIDs.remove(var0);

         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var2) {
            var2.printStackTrace();
         }

         String var1 = terminalsFolder + "/" + var0;
         if (!deleteDirectory(new File(var1))) {
            Log.error("Cannot delete folder \"" + var1 + "\"");
         }
      } else {
         Log.warn("Terminal \"" + var0 + "\" does not exist.");
      }
   }

   private static boolean terminalExists(String var0) {
      for (String var2 : terminalPIDs.keySet()) {
         if (var2.compareTo(var0) == 0) {
            return true;
         }
      }

      return false;
   }

   private static boolean deleteDirectory(File var0) {
      if (var0.exists()) {
         File[] var1 = var0.listFiles();
         if (null != var1) {
            for (int var2 = 0; var2 < var1.length; var2++) {
               if (var1[var2].isDirectory()) {
                  deleteDirectory(var1[var2]);
               } else {
                  var1[var2].delete();
               }
            }
         }
      }

      return var0.delete();
   }

   public static int getTerminalPID(String var0) {
      return terminalPIDs.get(var0);
   }

   public static boolean killTerminalProcess(String var0) {
      int var1 = terminalPIDs.get(var0);
      boolean var2 = PSUtils.getInstance().getHwndByPID(var1) == 0;
      return var2 || PSUtils.getInstance().terminateApp(var1, 1000);
   }
}
