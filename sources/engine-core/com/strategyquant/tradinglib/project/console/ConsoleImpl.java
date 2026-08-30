package com.strategyquant.tradinglib.project.console;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.imports.AvailableDataFormats;
import com.strategyquant.datalib.data.imports.CustomDataFormat;
import com.strategyquant.datalib.data.imports.DataImportEngine;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.L88OaFjjon.Tu5UdlpFO9;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.impl.IConsoleImpl;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleImpl implements IConsoleImpl {
   public static final Logger Log = LoggerFactory.getLogger(ConsoleImpl.class);
   protected HashMap<String, String> parsedParams;
   protected String appName;
   protected String exeName;
   private int exitStatus = 0;
   String sqLogo = "  ____   _                _                        ___                        _    __  __\n / ___| | |_  _ __  __ _ | |_  ___   __ _  _   _  / _ \\  _   _   __ _  _ __  | |_  \\ \\/ /\n \\___ \\ | __|| '__|/ _` || __|/ _ \\ / _` || | | || | | || | | | / _` || '_ \\ | __|  \\  / \n  ___) || |_ | |  | (_| || |_|  __/| (_| || |_| || |_| || |_| || (_| || | | || |_   /  \\ \n |____/  \\__||_|   \\__,_| \\__|\\___| \\__, | \\__, | \\__\\_\\ \\__,_| \\__,_||_| |_| \\__| /_/\\_\\\n                                    |___/  |___/                                         ";
   String qdmLogo = "   ___   ____   __  __    \n  / _ \\ |  _ \\ |  \\/  |\n | | | || | | || |\\/| |  \n | |_| || |_| || |  | |   \n  \\__\\_\\|____/ |_|  |_|";

   public ConsoleImpl(String var1, String var2) {
      this.appName = var1;
      this.exeName = var2;
      MainApp.cli = this;
   }

   public void run(String[] var1) throws Exception {
      MainApp.AppLoaded = true;
      if (var1.length != 0 && (var1.length != 1 || !var1[0].startsWith("liccheck") && !var1[0].startsWith("license"))) {
         if (var1.length == 1 && var1[0].equals("-gui")) {
            this.runCommand(new String[]{"-gui"});
            this.cli();
         } else {
            try {
               this.parsedParams = this.parseParams(var1);
               CLILogger.log("--------------------------------------------------");
               CLILogger.log(L.t("Starting %s in command line mode.", new Object[]{this.appName}));
               CLILogger.log(L.t("Params: %s", new Object[]{this.appArgsToString(var1)}));
               this.runCommand(var1);
            } catch (Exception var6) {
               this.exitStatus = 1;
               Log.error("Error: " + var6.getMessage(), var6);
               if (this.parsedParams != null && this.parsedParams.containsKey("-verbose")) {
                  CLILogger.log("Error: " + SQUtils.stackTraceToString(var6));
               } else {
                  CLILogger.log("Error: " + var6.getMessage());
               }
            } finally {
               this.waitAndFinish();
            }
         }
      } else {
         this.cli();
      }
   }

   private void cli() {
      (new Thread() {
            @Override
            public void run() {
               ConsoleImpl.Log
                  .debug(MainApp.getProductName() + " CLI loaded in " + SQTime.formatDateTime((System.currentTimeMillis() - MainApp.TimeAppStarted) / 1000L));
               if (MainApp.checkProduct("SQUANT")) {
                  CLILogger.log(ConsoleImpl.this.sqLogo);
               }

               CLILogger.log(L.t("Welcome to %s Command Line Interface", new Object[]{ConsoleImpl.this.appName}));
               CLILogger.log(L.t("SQX CLI is now ready. Execute a command or type -h for assistance", new Object[0]));
               Scanner var1 = new Scanner(System.in);

               while (var1.hasNextLine()) {
                  try {
                     String var2 = var1.nextLine();
                     if (!var2.trim().isEmpty()) {
                        String[] var3 = ConsoleImpl.translateCommandline(var2);
                        if (var3[0].equalsIgnoreCase("-exit") || !MainApp.CLIWebServeActive) {
                           ConsoleImpl.this.runCommand(var3);
                        }
                     }
                  } catch (Exception var4) {
                     ConsoleImpl.Log.error("Error: " + var4.getMessage(), var4);
                     if (ConsoleImpl.this.parsedParams != null && ConsoleImpl.this.parsedParams.containsKey("-verbose")) {
                        CLILogger.log("Error: " + SQUtils.stackTraceToString(var4));
                     } else {
                        CLILogger.log("Error: " + var4.getMessage());
                     }
                  }
               }
            }
         })
         .start();
      MainApp.CLIInteractiveMode = true;
   }

   public void runCommand(String[] var1) throws Exception {
      this.parsedParams = this.parseParams(var1);
      switch (var1[0]) {
         case "-run":
            this.run();
            break;
         case "-symbol":
            this.symbol();
            break;
         case "-instrument":
            this.instrument();
            break;
         case "-data":
            this.data();
            break;
         case "-brokerprofile":
            this.brokerProfile();
            break;
         case "-license":
            this.license();
            break;
         case "-a":
            this.addSymbol();
            break;
         case "-e":
            this.editSymbol();
            break;
         case "-d":
            this.deleteSymbol();
            break;
         case "-c":
            this.clearSymbolData();
            break;
         case "-l":
            this.listSymbols();
            break;
         case "-ia":
            this.addInstrument();
            break;
         case "-ie":
            this.editInstrument();
            break;
         case "-id":
            this.deleteInstrument();
            break;
         case "-il":
            this.listInstruments();
            break;
         case "-u":
            this.update();
            break;
         case "-di":
            this.dataImport();
            break;
         case "-de":
            this.dataExport();
            break;
         case "-dc":
            this.cloneData();
            break;
         case "-tz":
            this.listTimezones();
            break;
         case "help":
            this.help();
            break;
         case "-help":
            this.help();
            break;
         case "-h":
            this.help();
            break;
         case "-gui":
            MainApp.CLIWebServeActive = MainApp.initWebServer();
            if (MainApp.CLIWebServeActive) {
               Log.info("All other CLI commands are disabled.");
            }
            break;
         case "-deletefile":
            this.deleteFile();
            break;
         case "-waitfor":
            this.waitFor();
            break;
         case "-execute":
            this.execute();
            break;
         case "quit":
         case "exit":
         case "-exit":
            this.exit();
            break;
         default:
            this.exitStatus = 1;
            CLILogger.log(L.t("Unrecognized command %s. Specify -h for list of available options and commands.", new Object[]{var1[0]}));
      }
   }

   private Map<String, String[]> createParameterMap() {
      HashMap var1 = new HashMap();

      for (String var3 : this.parsedParams.keySet()) {
         if (!var3.equals("action")) {
            String var4 = this.parsedParams.get(var3);
            var1.put(var3, new String[]{var4});
         }
      }

      return var1;
   }

   private void brokerProfile() throws Exception {
      String var1 = this.getParam("action");
      String var2 = var1;
      Map var3 = this.createParameterMap();
      switch (var1) {
         case "list":
            CLILogger.log(L.t("List of available profiles", new Object[0]), true);
            break;
         case "importstocks":
            var3.put("brokerId", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "import";
            break;
         case "exportstocks":
            var3.put("brokerId", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "export";
            break;
         case "import":
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "loadXml";
            break;
         case "export":
            var3.put("ids[]", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "saveXml";
            break;
         case "updateAll":
            var2 = "updateData";
            List var6 = BrokerManager.getInstance().getAllBrokers();
            List var7 = var6.stream().map(var0 -> var0.getId().toString()).collect(Collectors.toList());
            var3.put("brokerId", new String[]{String.join(",", String.join(",", var7))});
            break;
         case "update":
            var2 = "updateData";
            var3.put("brokerId", new String[]{this.getParam("id")});
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }

      String var8 = (String)Program.get("Brokers").call(var2, new Object[]{var3});
      CLILogger.log(var1.equals("list") ? this.parseListBrokers(var8) : this.parseCallResponse(var8));
   }

   private String parseListBrokers(String var1) {
      try {
         JSONObject var2 = new JSONObject(var1);
         if (var2.has("success")) {
            JSONArray var3 = (JSONArray)var2.get("rows");
            StringBuilder var4 = new StringBuilder();
            var4.append("ID\tName\n");

            for (int var5 = 0; var5 < var3.length(); var5++) {
               JSONObject var6 = (JSONObject)var3.get(var5);
               JSONArray var7 = (JSONArray)var6.get("data");
               JSONObject var8 = (JSONObject)var6.get("userdata");
               var4.append(var8.getInt("id") + "\t" + var7.get(0) + "\n");
            }

            return var4.toString();
         }

         if (var2.has("error")) {
            return var2.getString("error") + "\n" + "--------------------------------------------------";
         }
      } catch (Exception var9) {
      }

      return "";
   }

   public String runRemoteCommand(String[] var1) throws Exception {
      CLILogger.lastCommandOutput = "";
      this.runCommand(var1);
      return CLILogger.lastCommandOutput;
   }

   private void execute() throws Exception {
      String var1 = this.getParam("file");
      File var2 = new File(var1);
      if (!var2.exists()) {
         throw new Exception(L.t("Script '%s' doesn't exist.", new Object[]{var2.getAbsolutePath()}));
      }

      CLILogger.log(String.format("Executing script '%s'", var2.getName()));
      Process var3 = Runtime.getRuntime().exec(var1);
      var3.waitFor();
      CLILogger.log(L.t("Script done.", new Object[0]));
   }

   private void waitFor() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "user":
            CLILogger.log(L.t("Waiting for user action.", new Object[0]));
            CLILogger.log(L.t("Press \"ENTER\" to continue...", new Object[0]));
            Scanner var4 = new Scanner(System.in);
            var4.nextLine();
            CLILogger.log(L.t("Continuing to next command.", new Object[0]));
            break;
         case "file":
            String var5 = this.getParam("file");
            File var6 = new File(var5);
            if (!var6.exists()) {
               CLILogger.log(L.t("Waiting for file '%s'.", new Object[]{var6.getAbsolutePath()}));

               while (true) {
                  try {
                     Thread.sleep(1000L);
                  } catch (Exception var9) {
                  }

                  try {
                     if (var6.exists()) {
                        CLILogger.log(L.t("File '%s' exists.", new Object[]{var6.getName()}));
                        CLILogger.log(L.t("Continuing to next command.", new Object[0]));
                        return;
                     }
                  } catch (Exception var8) {
                     Log.error("Exc.", var8);
                  }
               }
            }

            L.t("File '%s' exists.", new Object[]{var6.getAbsolutePath()});
            CLILogger.log(L.t("Continuing to next command.", new Object[0]));
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   private void deleteFile() throws Exception {
      String var1 = this.getParam("file");
      File var2 = new File(var1);
      if (!var2.exists()) {
         throw new Exception(L.t("File '%s' doesn't exist.", new Object[]{var2.getAbsolutePath()}));
      }

      if (!var2.delete()) {
         throw new Exception(L.t("File '%s' couldn't be deleted.", new Object[]{var2.getAbsolutePath()}));
      }

      CLILogger.log(L.t("File '%s' successfully deleted.", new Object[]{var2.getAbsolutePath()}));
   }

   protected void symbol() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "add":
            this.addSymbol();
            break;
         case "edit":
            this.editSymbol();
            break;
         case "delete":
            this.deleteSymbol();
            break;
         case "clear":
            this.clearSymbolData();
            break;
         case "list":
            this.listSymbols();
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   protected void instrument() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "add":
            this.addInstrument();
            break;
         case "edit":
            this.editInstrument();
            break;
         case "delete":
            this.deleteInstrument();
            break;
         case "list":
            this.listInstruments();
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   protected void data() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "update":
            this.update();
            break;
         case "import":
            this.dataImport();
            break;
         case "export":
            this.dataExport();
            break;
         case "clone":
            this.cloneData();
            break;
         case "timezones":
            this.listTimezones();
            break;
         case "exportToMT4":
            this.exportToMT4();
            break;
         case "exportToMT5":
            this.exportToMT5();
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   protected void license() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "info":
            this.printLicenseInfo();
            break;
         case "update":
            try {
               String var4 = this.getParam("code");
               CLILogger.log("Verifying license ...");
               MainApp.rsq3UErJhC(var4);
               CLILogger.log("License was successfully verified.");
               this.printLicenseInfo();
               V571hfnsHw var5 = MainApp.v571hfnsHw();
               if (!var5.yIifbIrWD3() && !var5.w49uJpREZO()) {
                  Tu5UdlpFO9.pzi7BBRxNg("");
               } else {
                  Tu5UdlpFO9.pzi7BBRxNg(var5.eHvc2JguAd());
               }
            } catch (Exception | Error var6) {
               CLILogger.log("Failed to check license - " + var6.getMessage());
               Log.error("Failed to check license. Exc. ", var6);
            }
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   private void printLicenseInfo() {
      String var1 = this.appName;
      if (var1.endsWith("X")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      V571hfnsHw var2 = MainApp.v571hfnsHw();
      var1 = var1 + MainApp.printAppVersion(var2.printEdition(), false);
      if (var2.aJg1L21A0t()) {
         var1 = var1 + String.format(" for %s", var2.inygRmSMx7() != null ? var2.inygRmSMx7().getName() : "Unknown");
      }

      if (var2.aVwpAOwrF1()) {
         var1 = var1 + " - Licensed to: " + var2.oOkoIeUsMG() + " - " + var2.fSECzwVwpK();
      }

      var1 = var1 + " " + var2.printType(var2.eHvc2JguAd());
      var1 = var1 + ", license " + var2.eHvc2JguAd();
      CLILogger.log(var1);
   }

   protected void exit() {
      CLILogger.log(L.t("Bye", new Object[0]));
      CLILogger.closeFile();
      MainApp.exitJVM("cmd -exit", this.exitStatus);
   }

   protected void run() throws Exception {
      String var1 = SQUtils.fileToString(this.getParam("file"));
      String[] var2 = var1.split("\n");

      for (int var3 = 0; var3 < var2.length; var3++) {
         String var4 = var2[var3];
         if (!var4.startsWith("#")) {
            CLILogger.log(L.t("Running command: %s", new Object[]{var4.trim()}), true);
            this.runCommand(translateCommandline(var4));
         }
      }
   }

   private void listTimezones() throws Exception {
      String var1 = (String)Program.get("DataManagerData").call("listTimezones", null);
      CLILogger.log(this.parseCallResponse(var1));
   }

   protected void help() throws Exception {
      String var1 = null;
      if (var1 == null) {
         String var2 = SQStructure.INTERNAL_DIR_PATH + "web/" + MainApp.getProduct() + "/help.txt";
         var1 = SQUtils.fileToString(var2);
         var1 = var1.replaceAll("#ExportAvailableCsvFormats#", this.getExportCsvFormats());
         var1 = var1.replaceAll("#CryptoExchanges#", this.getExchanges());
         var1 = var1.replaceAll("#AvailableTimeframes#", this.getTimeframes());
      }

      CLILogger.log(var1);
   }

   protected String getExportCsvFormats() {
      String var1 = "";

      try {
         String var2 = (String)Program.get("DataManagerData").call("exportToCsvLoadSettings", null);
         JSONObject var3 = new JSONObject(var2);
         JSONArray var4 = var3.getJSONArray("formats");

         for (int var5 = 0; var5 < var4.length(); var5++) {
            var1 = var1 + var4.getJSONObject(var5).getString("name") + ",";
         }
      } catch (Exception var6) {
      }

      if (var1.length() > 0) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      return var1;
   }

   protected String getTimeframes() {
      String var1 = "";

      try {
         ArrayList var2 = TimeframeManager.getPredefinedTimeframes();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            var1 = var1 + (String)var2.get(var3) + ",";
         }
      } catch (Exception var4) {
      }

      if (var1.length() > 0) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      return var1;
   }

   protected String getExchanges() {
      String var1 = "";

      try {
         String var2 = (String)Program.get("DataSourceCrypto").call("getExchanges", null);
         JSONObject var3 = new JSONObject(var2);
         JSONArray var4 = var3.getJSONArray("exchanges");

         for (int var5 = 0; var5 < var4.length(); var5++) {
            var1 = var1 + var4.getJSONObject(var5).getString("value") + ",";
         }
      } catch (Exception var6) {
      }

      if (var1.length() > 0) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      return var1;
   }

   private void cloneData() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("symbols", new String[]{this.getParam("symbols")});
      var1.put("postfix", new String[]{this.getParam("postfix", "_{timeframe}_{cloneTime}")});
      var1.put("removeWeekends", new String[]{this.getParam("removeweekends", "false")});
      String var2 = this.getParam("timezone", null);
      String var3 = this.getParam("hours", null);
      if (var2 == null && var3 == null) {
         this.getParam("timezone");
      }

      var1.put("cloneTimezone", new String[]{var2});
      var1.put("cloneTimezoneType", new String[]{var2 != null ? "1" : "0"});
      var1.put("cloneTimezoneShift", new String[]{var3});
      String var4 = (String)Program.get("DataManagerData").call("cloneToTimezone", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var4));
   }

   private void dataExport() throws Exception {
      CLILogger.log("Exporting data.", true);
      String var1 = this.getParam("symbols");
      String var2 = this.getParam("timeframe");
      String var3 = this.getParam("outputdir", SQPaths.exportDirPath);
      String var4 = this.getParam("prefix", "");
      String var5 = this.getParam("format", "MetaTrader4 bar format");
      HashMap var6 = new HashMap();
      var6.put("symbols", new String[]{var1});
      var6.put("timeframe", new String[]{var2});
      var6.put("directory", new String[]{var3});
      var6.put("filePrefix", new String[]{var4});
      var6.put("session", new String[]{"No Session"});
      var6.put("fileFormat", new String[]{var5});
      if (var5.equalsIgnoreCase("Custom")) {
         var6.put("header", new String[]{this.getParam("cHeader", "NA")});
         var6.put("format", new String[]{this.getParam("cFormat")});
         var6.put("includeHeader", new String[]{this.getParam("cIncludeHeader", "false")});
      }

      if (this.paramsExists("datefrom")) {
         var6.put("dateFrom", new String[]{this.getParam("datefrom")});
      }

      if (this.paramsExists("dateto")) {
         var6.put("dateTo", new String[]{this.getParam("dateto")});
      }

      String var7 = (String)Program.get("DataManagerData").call("exportToCsv", new Object[]{var6});
      CLILogger.log(this.parseCallResponse(var7));
   }

   private void exportToMT4() throws Exception {
      CLILogger.log("Exporting data to MT4.", true);
      String var1 = this.getParam("symbol");
      HashMap var2 = new HashMap();
      var2.put("sqSymbol", new String[]{var1});
      var2.put("timeframe", new String[]{this.getParam("timeframe")});
      var2.put("appPath", new String[]{this.getParam("appPath")});
      var2.put("dataPath", new String[]{this.getParam("dataPath")});
      var2.put("serverName", new String[]{this.getParam("serverName")});
      var2.put("mt4Symbol", new String[]{var1});
      var2.put("mt4SymbolName", new String[]{var1});
      var2.put("properties", new String[]{"default"});
      if (this.paramsExists("datefrom")) {
         var2.put("dateFrom", new String[]{this.getParam("datefrom")});
      }

      if (this.paramsExists("dateto")) {
         var2.put("dateTo", new String[]{this.getParam("dateto")});
      }

      String var3 = (String)Program.get("DataManagerData").call("exportToMT4", new Object[]{var2});
      CLILogger.log(this.parseCallResponse(var3));
   }

   private void exportToMT5() throws Exception {
      CLILogger.log("Exporting data to MT5.", true);
      String var1 = this.getParam("timeframe", "Timeframe", new String[]{"TICK", "M1"});
      String var2 = "points";
      String var3 = "1";
      if (this.paramsExists("spreadType")) {
         var2 = this.getParam("spreadType", "Spread", new String[]{"points", "pips", "real"});
      } else {
         CLILogger.log("Using default spreadType: " + var2);
      }

      if (this.paramsExists("spreadValue")) {
         if (var2.equals("points") || var2.equals("pips")) {
            var3 = this.getParam("spreadValue");
         }
      } else if (!var2.equals("real")) {
         CLILogger.log("Using default spreadValue: " + var3);
      }

      String var4 = this.getParam("symbol");
      HashMap var5 = new HashMap();
      var5.put("symbol", new String[]{var4});
      var5.put("timeframe", new String[]{var1});
      var5.put("spread", new String[]{var2});
      var5.put("spreadValue", new String[]{var3});
      var5.put("spreadPoints", new String[]{var3});
      var5.put("directory", new String[]{this.getParam("outputdir", SQPaths.exportDirPath)});
      var5.put("fileName", new String[]{this.getParam("filename", var4)});
      if (this.paramsExists("datefrom")) {
         var5.put("dateFrom", new String[]{this.getParam("datefrom")});
      }

      if (this.paramsExists("dateto")) {
         var5.put("dateTo", new String[]{this.getParam("dateto")});
      }

      String var6 = (String)Program.get("DataManagerData").call("exportToMT5", new Object[]{var5});
      CLILogger.log(this.parseCallResponse(var6));
   }

   private String getParam(String var1, String var2, String[] var3) throws Exception {
      String var4 = this.getParam(var1);

      for (int var5 = 0; var5 < var3.length; var5++) {
         if (var3[var5].equals(var4)) {
            return var4;
         }
      }

      throw new Exception(L.t("'%s' must be one of ['%s'].", new Object[]{var2, String.join(",", var3)}));
   }

   private void dataImport() throws Exception {
      CLILogger.log(L.t("Importing data.", new Object[0]), true);
      String var1 = this.getParam("filepath");
      String var2 = this.getParam("symbol");
      String var3 = this.getParam("instrument", null);
      String var4 = this.getBarType();
      String var5 = this.getParam("timeframe", "auto");
      String var6 = this.getParam("timezone", "Etc/UCT");
      String var7 = this.getParam("format");
      String var8 = this.getParam("errorhandling", "stop");
      if (var8.equalsIgnoreCase("stop")) {
         var8 = "0";
      } else if (var8.equalsIgnoreCase("ignore")) {
         var8 = "1";
      } else {
         var8 = "0";
      }

      CLILogger.log(L.t("Checking symbol '%s'.", new Object[]{var2}));
      if (!DataManager.checkDataExists("History", var2)) {
         CLILogger.log(L.t("Symbol doesn't exist. Creating symbol...", new Object[0]));
         HashMap var9 = new HashMap();
         var9.put("connection", new String[]{"History"});
         var9.put("symbol", new String[]{var2});
         var9.put("instrument", new String[]{var3});
         var9.put("barType", new String[]{var4});
         var9.put("source", new String[]{"1"});
         String var10 = (String)Program.get("DataSourceFiles").call("add", new Object[]{var9});
         CLILogger.log(this.parseCallResponse(var10));
      } else {
         CLILogger.log(L.t("Symbol exists.", new Object[0]));
      }

      DataInfo var17 = DataManager.getDataInfo("History", var2);
      CLILogger.log(L.t("Recognizing file format...", new Object[0]));
      ArrayList var18 = AvailableDataFormats.getInstance().getAvailableFileFormats();
      CustomDataFormat var11 = null;
      int var12 = -1;
      if (var7 != null && !var7.trim().isEmpty()) {
         for (int var13 = 0; var13 < var18.size(); var13++) {
            try {
               if (((CustomDataFormat)var18.get(var13)).getName().equals(var7)) {
                  var12 = var13;
                  break;
               }
            } catch (Exception var15) {
               Log.debug("Import history data - cannot apply file format '" + var7 + "'. Reason: " + var15.getMessage(), var15);
            }
         }
      }

      if (var12 != -1) {
         var11 = DataImportEngine.getFileFormat(var1, var18, (CustomDataFormat)var18.get(var12));
      } else {
         var11 = DataImportEngine.getFileFormat(var1, var18, (CustomDataFormat)var18.get(0));
      }

      CLILogger.log(L.t("Importing data...", new Object[0]));
      HashMap var20 = new HashMap();
      var20.put("connectionName", new String[]{var17.connection});
      var20.put("symbol", new String[]{var17.symbol});
      var20.put("timezone", new String[]{var6});
      var20.put("fileFormat", new String[]{var11.getName()});
      var20.put("filePath", new String[]{var1});
      var20.put("dateFormat", new String[]{var11.getDateFormat()});
      var20.put("separator", new String[]{var11.getSeparator()});
      var20.put("skipRows", new String[]{var11.getSkipRows() + ""});
      var20.put("skipColumns", new String[]{var11.getSkipColumns() + ""});
      var20.put("errorHandling", new String[]{var8});
      var20.put("timeframe", new String[]{var5});
      var20.put("columnTypes", new String[]{var11.columTypesToString()});
      String var14 = (String)Program.get("DataSourceFiles").call("import", new Object[]{var20});
      CLILogger.log(this.parseCallResponse(var14));
   }

   private void update() throws Exception {
      HashMap var1 = new HashMap();
      String var2 = "updateAll";
      if (this.paramsExists("symbols")) {
         var2 = "updateSelected";
         var1.put("symbols", new String[]{this.getParam("symbols")});
         CLILogger.log(L.t("Updating selected data.", new Object[0]), true);
      } else {
         CLILogger.log(L.t("Updating all data.", new Object[0]), true);
      }

      String var3 = (String)Program.get("DataManagerData").call(var2, new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var3));
   }

   private void listInstruments() throws Exception {
      HashMap var1 = new HashMap();
      if (this.paramsExists("csv")) {
         var1.put("csv", new String[]{this.getParam("csv")});
      }

      String var2 = (String)Program.get("DataManagerInstruments").call("list", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var2));
   }

   private void deleteInstrument() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("instruments", new String[]{this.getParam("instruments")});
      String var2 = (String)Program.get("DataManagerInstruments").call("removeInstrument", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var2));
   }

   private void editInstrument() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("instrument", new String[]{this.getParam("instrument")});
      if (this.paramsExists("description")) {
         var1.put("description", new String[]{this.getParam("description")});
      }

      if (this.paramsExists("pointvalue")) {
         var1.put("pointValue", new String[]{this.getParam("pointvalue")});
      }

      if (this.paramsExists("ticksize")) {
         var1.put("tickSize", new String[]{this.getParam("ticksize")});
      }

      if (this.paramsExists("tickstep")) {
         var1.put("tickStep", new String[]{this.getParam("tickstep")});
      }

      if (this.paramsExists("defaultspread")) {
         var1.put("defaultSpread", new String[]{this.getParam("defaultspread")});
      }

      if (this.paramsExists("defaultslippage")) {
         var1.put("defaultSlippage", new String[]{this.getParam("defaultslippage")});
      }

      if (this.paramsExists("datatype")) {
         var1.put("dataType", new String[]{this.getDataType()});
      }

      if (this.paramsExists("minDistance")) {
         var1.put("minDistance", new String[]{this.getParam("minDistance")});
      }

      if (this.paramsExists("orderSizeMultiplier")) {
         var1.put("orderSizeMultiplier", new String[]{this.getParam("orderSizeMultiplier")});
      }

      if (this.paramsExists("orderSizeStep")) {
         var1.put("orderSizeStep", new String[]{this.getParam("orderSizeStep")});
      }

      String var2 = (String)Program.get("DataManagerInstruments").call("editInstrument", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var2));
   }

   private void addInstrument() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("instrument", new String[]{this.getParam("instrument")});
      var1.put("description", new String[]{this.getParam("description", "")});
      var1.put("pointValue", new String[]{this.getParam("pointvalue", "100000")});
      var1.put("tickSize", new String[]{this.getParam("ticksize", "0.0001")});
      var1.put("tickStep", new String[]{this.getParam("tickstep", "0.00001")});
      var1.put("defaultSpread", new String[]{this.getParam("defaultspread", "2")});
      var1.put("defaultSlippage", new String[]{this.getParam("defaultslippage", "1")});
      var1.put("commissions", new String[]{""});
      var1.put("dataType", new String[]{this.getDataType()});
      var1.put("orderSizeMultiplier", new String[]{this.getParam("orderSizeMultiplier", "1")});
      var1.put("orderSizeStep", new String[]{this.getParam("orderSizeStep", "0")});
      var1.put("minDistance", new String[]{this.getParam("minDistance", "0")});
      if (this.paramsExists("swap")) {
         var1.put("swap", new String[]{this.getParam("swap")});
      }

      String var2 = this.getParam("broker", "");
      BrokerManager var3 = BrokerManager.getInstance();
      BrokerDto var4 = var2.equals("") ? var3.getDefaultBroker() : var3.getBroker(var2);
      if (var4 == null) {
         CLILogger.log("Broker was not found: " + var2);
      } else {
         var1.put("broker", new String[]{var4.getId().toString()});
         String var5 = (String)Program.get("DataManagerInstruments").call("addInstrument", new Object[]{var1});
         CLILogger.log(this.parseCallResponse(var5));
      }
   }

   private void listSymbols() throws Exception {
      HashMap var1 = new HashMap();
      if (this.paramsExists("csv")) {
         var1.put("csv", new String[]{this.getParam("csv")});
      }

      String var2 = (String)Program.get("DataManagerData").call("listData", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var2));
   }

   private void clearSymbolData() throws Exception {
      String var1 = this.getParam("symbols");
      String[] var2 = var1.split(",");
      String var3 = "";

      for (int var4 = 0; var4 < var2.length; var4++) {
         if (var4 == var2.length - 1) {
            var3 = var3 + "History";
         } else {
            var3 = var3 + "History,";
         }
      }

      HashMap var6 = new HashMap();
      var6.put("symbols", new String[]{var1});
      var6.put("connections", new String[]{var3});
      String var5 = (String)Program.get("DataManagerData").call("clearData", new Object[]{var6});
      CLILogger.log(this.parseCallResponse(var5));
   }

   private void deleteSymbol() throws Exception {
      String var1 = this.getParam("symbols");
      String[] var2 = var1.split(",");
      String var3 = "";

      for (int var4 = 0; var4 < var2.length; var4++) {
         if (var4 == var2.length - 1) {
            var3 = var3 + "History";
         } else {
            var3 = var3 + "History,";
         }
      }

      HashMap var6 = new HashMap();
      var6.put("symbols", new String[]{var1});
      var6.put("connections", new String[]{var3});
      String var5 = (String)Program.get("DataManagerData").call("removeData", new Object[]{var6});
      CLILogger.log(this.parseCallResponse(var5));
   }

   private void editSymbol() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("connection", new String[]{"History"});
      String var2 = this.getParam("symbol");
      var1.put("symbol", new String[]{var2});
      var1.put("symbolNew", new String[]{this.getParam("name", var2)});
      if (this.paramsExists("instrument")) {
         var1.put("instrument", new String[]{this.getParam("instrument")});
      }

      if (this.paramsExists("barType")) {
         var1.put("barType", new String[]{this.getBarType()});
      }

      String var3 = (String)Program.get("DataManagerData").call("editData", new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var3));
   }

   protected void addSymbol() throws Exception {
      String var1 = this.getParam("datasource", "dukascopy");
      if (var1.equalsIgnoreCase("dukascopy")) {
         HashMap var2 = new HashMap();
         var2.put("symbols[]", this.getParam("symbols").split(","));
         var2.put("postfix", new String[]{this.getParam("postfix", "")});
         var2.put("dataType", new String[]{this.getParam("datatype", "M1")});
         String var3 = this.getParam("broker", "");
         BrokerManager var4 = BrokerManager.getInstance();
         BrokerDto var5 = var3.equals("") ? var4.getDefaultBroker() : var4.getBroker(var3);
         if (var5 == null) {
            CLILogger.log("Broker was not found: " + var3);
            return;
         }

         var2.put("broker", new String[]{var5.getId().toString()});
         CLILogger.log(L.t("Creating symbols...", new Object[0]));
         String var6 = (String)Program.get("DataSourceDukascopy").call("addData", new Object[]{var2});
         CLILogger.log(this.parseCallResponse(var6));
      } else if (var1.equalsIgnoreCase("file")) {
         String[] var7 = this.getParam("symbols").split(",");

         for (int var13 = 0; var13 < var7.length; var13++) {
            HashMap var19 = new HashMap();
            var19.put("connection", new String[]{"History"});
            var19.put("symbol", new String[]{var7[var13]});
            var19.put("instrument", new String[]{this.getParam("instrument")});
            var19.put("barType", new String[]{this.getBarType()});
            CLILogger.log(L.t("Creating symbol '%s'...", new Object[]{var7[var13]}));
            String var22 = (String)Program.get("DataSourceFiles").call("add", new Object[]{var19});
            CLILogger.log(this.parseCallResponse(var22));
         }
      } else if (var1.equalsIgnoreCase("darwinex")) {
         HashMap var8 = new HashMap();
         var8.put("symbols[]", this.getParam("symbols").split(","));
         var8.put("broker", new String[]{"-1"});
         var8.put("postfix", new String[]{this.getParam("postfix", "")});
         String var14 = this.getParam("broker", "");
         BrokerManager var20 = BrokerManager.getInstance();
         BrokerDto var23 = var14.equals("") ? var20.getDefaultBroker() : var20.getBroker(var14);
         if (var23 == null) {
            CLILogger.log("Broker was not found: " + var14);
            return;
         }

         var8.put("broker", new String[]{var23.getId().toString()});
         CLILogger.log(L.t("Creating symbols...", new Object[0]));
         String var24 = (String)Program.get("DataSourceDarwinex").call("downloadAddData", new Object[]{var8});
         CLILogger.log(this.parseCallResponse(var24));
      } else if (var1.equalsIgnoreCase("crypto")) {
         HashMap var9 = new HashMap();
         var9.put("symbols", new String[]{this.getParam("symbols")});
         var9.put("postfix", new String[]{this.getParam("postfix", "")});
         var9.put("timeframe", new String[]{this.getParam("timeframe", "M1")});
         var9.put("exchange", new String[]{this.getParam("exchange", "Binance")});
         CLILogger.log("Creating symbols...");
         String var15 = (String)Program.get("DataSourceCrypto").call("add", new Object[]{var9});
         CLILogger.log(this.parseCallResponse(var15));
      } else if (var1.equalsIgnoreCase("yahoo")) {
         HashMap var10 = new HashMap();
         var10.put("symbols", new String[]{this.getParam("symbols")});
         var10.put("postfix", new String[]{this.getParam("postfix", "")});
         CLILogger.log(L.t("Creating symbols...", new Object[0]));
         String var16 = (String)Program.get("DataSourceYahoo").call("add", new Object[]{var10});
         CLILogger.log(this.parseCallResponse(var16));
      } else if (var1.equalsIgnoreCase("SQEquityData")) {
         HashMap var11 = new HashMap();
         var11.put("symbols", new String[]{this.getParam("symbols")});
         var11.put("barType", new String[]{this.getBarType()});
         CLILogger.log(L.t("Creating symbols...", new Object[0]));
         String var17 = (String)Program.get("DataSourceSQEquityData").call("add", new Object[]{var11});
         CLILogger.log(this.parseCallResponse(var17));
      } else {
         if (!var1.equalsIgnoreCase("mt5api")) {
            throw new Exception(L.t("Unrecognized data source '%s'.", new Object[]{var1}));
         }

         HashMap var12 = new HashMap();
         var12.put("symbols", new String[]{this.getParam("symbols")});
         var12.put("postfix", new String[]{this.getParam("postfix", "")});
         var12.put("broker", new String[]{this.getParam("broker", "-1")});
         String var18 = this.getParam("path", "");
         var12.put("path", new String[]{var18});
         var12.put("dateFrom", new String[]{this.getParam("datefrom")});
         var12.put("dateTo", new String[]{this.getParam("dateto")});
         CLILogger.log(L.t("Creating symbols...", new Object[0]));
         String var21 = (String)Program.get("DataSourceMt5Api").call("importData", new Object[]{var12});
         CLILogger.log(this.parseCallResponse(var21));
      }
   }

   protected String getDataType() throws Exception {
      String var1 = this.getParam("datatype", "Forex");
      if (var1.equalsIgnoreCase("Stock")) {
         return "1";
      } else if (var1.equalsIgnoreCase("Futures")) {
         return "2";
      } else if (var1.equalsIgnoreCase("Forex")) {
         return "3";
      } else if (var1.equalsIgnoreCase("CFDs")) {
         return "4";
      } else if (var1.equalsIgnoreCase("ETF")) {
         return "5";
      } else if (var1.equalsIgnoreCase("Index")) {
         return "6";
      } else {
         return var1.equalsIgnoreCase("Crypto") ? "7" : "3";
      }
   }

   protected String getBarType() throws Exception {
      String var1 = this.getParam("bartype", "startofbar");
      if (var1.equalsIgnoreCase("startofbar")) {
         return "1";
      } else {
         return var1.equalsIgnoreCase("endofbar") ? "2" : "1";
      }
   }

   protected String appArgsToString(String[] var1) {
      String var2 = "";

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2 = var2 + var1[var3] + " ";
      }

      return var2;
   }

   protected String getParam(String var1) throws Exception {
      if (!this.paramsExists(var1)) {
         throw new Exception(L.t("Missing parameter '%s'.", new Object[]{var1}));
      } else {
         return this.parsedParams.get(var1);
      }
   }

   protected String getParam(String var1, String var2) throws Exception {
      return !this.paramsExists(var1) ? var2 : this.parsedParams.get(var1);
   }

   protected boolean paramsExists(String var1) {
      return this.parsedParams.containsKey(var1);
   }

   public HashMap<String, String> parseParams(String[] var1) throws Exception {
      try {
         HashMap var2 = new HashMap();
         CLILogger.redirectOutputToFile(null);

         for (int var3 = 0; var3 < var1.length; var3++) {
            String var4 = var1[var3];
            if (var4.contains("=")) {
               String[] var5 = var4.split("=");
               if (var5.length == 2) {
                  var2.put(var5[0], var5[1]);
               }
            } else if (var4.contains(">") && var3 + 1 < var1.length) {
               CLILogger.redirectOutputToFile(var1[var3 + 1]);
            } else {
               var2.put(var4, null);
            }
         }

         return var2;
      } catch (Exception var6) {
         CLILogger.log(L.t("Incorrect command line parameters.", new Object[0]) + " " + var6.getMessage());
         throw new Exception("Incorrect command line parameters. " + var6.getMessage());
      }
   }

   private void redirectOutputToFile(String var1) {
   }

   protected String parseCallResponse(String var1) {
      String var2 = var1;

      try {
         JSONObject var3 = new JSONObject(var1);
         if (var3.has("success")) {
            var2 = var3.getString("success");
         } else if (var3.has("error")) {
            var2 = var3.getString("error");
         }
      } catch (Exception var4) {
      }

      return var2.equals("{}") ? "" : var2 + "\n" + "--------------------------------------------------";
   }

   protected void waitAndFinish() {
      while (true) {
         try {
            Thread.sleep(500L);
         } catch (Exception var2) {
         }

         if (SQGrid.getGridClient().countAllJobs(null) == 0L) {
            SQWebSocketManager.getInstance().sendData();
            CLILogger.closeFile();
            MainApp.exitApp();
         }
      }
   }

   public static String[] translateCommandline(String var0) throws Exception {
      if (var0 != null && !var0.isEmpty()) {
         byte var1 = 0;
         StringTokenizer var2 = new StringTokenizer(var0, "\"' ", true);
         ArrayList var3 = new ArrayList();
         StringBuilder var4 = new StringBuilder();
         boolean var5 = false;

         while (var2.hasMoreTokens()) {
            String var6 = var2.nextToken();
            switch (var1) {
               case 1:
                  if ("'".equals(var6)) {
                     var5 = true;
                     var1 = 0;
                  } else {
                     var4.append(var6);
                  }
                  continue;
               case 2:
                  if ("\"".equals(var6)) {
                     var5 = true;
                     var1 = 0;
                  } else {
                     var4.append(var6);
                  }
                  continue;
            }

            if ("'".equals(var6)) {
               var1 = 1;
            } else if ("\"".equals(var6)) {
               var1 = 2;
            } else if (" ".equals(var6)) {
               if (var5 || var4.length() > 0) {
                  var3.add(var4.toString().trim());
                  var4.setLength(0);
               }
            } else {
               var4.append(var6);
            }

            var5 = false;
         }

         if (var5 || var4.length() > 0) {
            var3.add(var4.toString().trim());
         }

         if (var1 != 1 && var1 != 2) {
            return var3.toArray(new String[var3.size()]);
         } else {
            throw new Exception("Unbalanced quotes in " + var0);
         }
      } else {
         return new String[0];
      }
   }
}
