package com.strategyquant.strategyquant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.strategyquant.allTimeStats.AllTimeStats;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataFolderSweeper;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.DataCsvLoader;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.columns.AskCol;
import com.strategyquant.datalib.data.io.columns.BidCol;
import com.strategyquant.datalib.data.io.columns.CloseCol;
import com.strategyquant.datalib.data.io.columns.DateCol;
import com.strategyquant.datalib.data.io.columns.DateTimeCol;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.data.io.columns.HighCol;
import com.strategyquant.datalib.data.io.columns.LowCol;
import com.strategyquant.datalib.data.io.columns.OpenCol;
import com.strategyquant.datalib.data.io.columns.TimeCol;
import com.strategyquant.datalib.data.io.columns.VolumeCol;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.dataseries.DoubleListCache;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.jobslib.JobEngine;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.L88OaFjjon.G8SyrBEfO8;
import com.strategyquant.lib.L88OaFjjon.Lewgre21g;
import com.strategyquant.lib.L88OaFjjon.Tu5UdlpFO9;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.LauncherConfig;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.impl.MainAppStandardImpl;
import com.strategyquant.lib.app.webserver.MainAppWebServer;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.customization.Customization;
import com.strategyquant.lib.hw.HardwareInfo;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.language.LFileGen;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.tempfiles.TempFilesManager;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.lib.whitelabel.Brokers;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.MemoryCleaner;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.backtest.TestfilesUtils;
import com.strategyquant.tradinglib.blocks.BlocksConfigGenerator;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.exchange.IExchange;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.formulas.Formulas;
import com.strategyquant.tradinglib.generator.Checkers;
import com.strategyquant.tradinglib.generator.Negaters;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.historyData.SpecialSymbolsManager;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.performance.Performance;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import com.strategyquant.tradinglib.project.IProject;
import com.strategyquant.tradinglib.project.IProjectCondition;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategiesSaver;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.console.SQConsole;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.project.websocket.channels.ProjectChannels;
import com.strategyquant.tradinglib.results.IResultPlugin;
import com.strategyquant.tradinglib.results.file.ILoaderPlugin;
import com.strategyquant.tradinglib.results.file.ISaverPlugin;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.results.stats.computer.OrderPLComputer;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.talib.TALibBlocks;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.WebServer;
import com.strategyquant.webguilib.config.RemoteAccessConfig;
import com.strategyquant.webguilib.servlet.MainServlet;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQApp extends MainAppStandardImpl {
   public static final Logger Log = LoggerFactory.getLogger(SQApp.class);
   private WebServer webServer;
   private SQConsole consoleAppInstance;
   private static String p = "StrategyQuant X";
   private static String lpc = lpc();
   private static String ps = "SQ";

   public static void main(String[] var0) {
      main(var0, false);
   }

   public static void main(String[] var0, boolean var1) {
      System.setProperty("algoWizardType", "Embedded");
      System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize", "200000000");
      appArgs = var0;
      runInConsole = var1;
      if (!runInConsole()) {
         hideConsoleForUIApp();
         MainApp.setMultiInstance(true);
         MainApp.addInstance(new SQApp(new MainWindow()));
         MainApp.settings().load();
         activateDebugLoggingLevel();
         MainApp.start();
      } else {
         MainApp.setMultiInstance(true);
         MainApp.addInstance(new SQApp(null));
         MainApp.settings().load();
         MainApp.start();
      }
   }

   private static void hideConsoleForUIApp() {
      if (!new File(MainApp.getDataPath() + "/console.txt").exists()) {
         OperatingSystem var0 = new OperatingSystem();
         if (var0.isWindows()) {
         }
      }
   }

   private static void activateDebugLoggingLevel() {
      LoggerContext var0 = (LoggerContext)LoggerFactory.getILoggerFactory();
      if (Boolean.parseBoolean(MainApp.settings().get("debugLevelActive", "false"))) {
         ch.qos.logback.classic.Logger var1 = var0.getLogger("root");
         var1.setLevel(Level.DEBUG);
         Log.info("Activated debug logging level.");
      } else {
         ch.qos.logback.classic.Logger var2 = var0.getLogger(ContextHandler.class);
         var2.setLevel(Level.ERROR);
      }
   }

   private static void redirectLogMessagesToFile(java.util.logging.Logger var0, String var1) throws IOException {
      FileHandler var2 = new FileHandler(var1);
      var2.setFormatter(new SimpleFormatter());

      for (Handler var6 : var0.getHandlers()) {
         var0.removeHandler(var6);
      }

      var0.addHandler(var2);
   }

   private static String lpc() {
      return MainApp.isBrazilianEdition() ? "SQBR" : "GENBUILDER";
   }

   public SQApp(MainWindow var1) {
      super("SQUANT", p, lpc, var1, ps);
      if (var1 != null) {
         this.webServer = var1.getWebServer();
      }
   }

   protected boolean initApp() throws Exception {
      Log.info("SQX version: " + this.getAppVersion());
      if (MainApp.runInConsole()) {
         Log.debug("HW: " + HardwareInfo.getInfo());
         Log.debug("Properties: " + System.getProperties());
         RuntimeMXBean var1 = ManagementFactory.getRuntimeMXBean();

         for (String var4 : var1.getInputArguments()) {
            Log.debug("Runtime args: " + var4);
         }

         for (GarbageCollectorMXBean var5 : ManagementFactory.getGarbageCollectorMXBeans()) {
            Log.debug("GC bean: " + var5.getName());
         }
      } else {
         Log.info("HW: " + HardwareInfo.getInfo());
         Log.info("Properties: " + System.getProperties());
         RuntimeMXBean var8 = ManagementFactory.getRuntimeMXBean();

         for (String var15 : var8.getInputArguments()) {
            Log.info("Runtime args: " + var15);
         }

         for (GarbageCollectorMXBean var17 : ManagementFactory.getGarbageCollectorMXBeans()) {
            Log.info("GC bean: " + var17.getName());
         }
      }

      this.deleteDeprecatedFiles();
      Log.debug(
         "Computer name: "
            + System.getenv("COMPUTERNAME")
            + ", User name: "
            + System.getProperty("user.name")
            + ", SQ version: "
            + MainApp.printAppVersion(true)
      );
      Locale var9 = new Locale("en", "US");
      Locale.setDefault(var9);
      System.setProperty("java.net.preferIPv4Stack", "true");
      this.checkWritable();
      this.setTimeZone();
      this.iUKTXv8M();
      if (!this.isRunInConsole()) {
         BrowserGUI.getInstance().showLicenseLoadingScreen();
         this.checkMaxLoadingTime();
      }

      try {
         this.createUserFolders();
         this.updateLoadingInfo(L.t("Initializing database & settings...", new Object[0]));
         Log.debug("Main app constructor");
         this.updateLoadingInfo(L.t("Loading customizations...", new Object[0]));
         Customization.load(true);
         HistoryDataManager.get().initAsync();
         this.updateLoadingInfo(L.t("Compiling snippets...", new Object[0]));
         this.checkSnippets();
         this.updateLoadingInfo(L.t("Loading performance settings...", new Object[0]));
         Performance.init();
         MemoryCleaner.init();
         MemoryUsageChecker.init();
         this.updateLoadingInfo(L.t("Initializing building blocks...", new Object[0]));
         this.initBlocks();
         this.updateLoadingInfo(L.t("Loading plugins...", new Object[0]));
         this.initPluginSystem();
         this.initPrograms();
         this.renameSQ4DatabankFiles();
         this.copyUpdaterFiles();
         this.updateLoadingInfo(L.t("Initializing stats computer...", new Object[0]));
         this.initStatsComputer(false);
         this.initEngines();
         MainApp.enableRequests();
         this.updateLoadingInfo(L.t("Initializing communication channels...", new Object[0]));
         ProjectChannels.init();
         TestfilesUtils.deleteTestingFiles();
         TempFilesManager.get().init();
         this.updateLoadingInfo(L.t("Checking data...", new Object[0]));
         this.checkData();
         this.updateLoadingInfo(L.t("Loading GUI...", new Object[0]));
         new DoubleListCache();
      } catch (Exception var6) {
         Log.error("Error during initialization.", var6);
         if (!runInConsole) {
            MainApp.showErrorDialog(L.t("Error", new Object[0]), L.t("Error during initialization.", new Object[0]) + "\n" + var6.getMessage());
         }

         MainApp.exitJVM("Error during initialization - " + var6.getMessage(), 1);
      } catch (Error var7) {
         Log.error("Error during initialization.", var7);
         if (!runInConsole) {
            MainApp.showErrorDialog(L.t("Error", new Object[0]), L.t("Error during initialization.", new Object[0]) + "\n" + var7.getMessage());
         }

         MainApp.exitJVM("Error during initialization - " + var7.getMessage(), 1);
      }

      Log.debug("Initialization of all components performed successfully.");
      return true;
   }

   private void createUserFolders() {
      Log.debug("Creating user folders...");
      File var1 = new File(SQStructure.USER_LIBS);
      if (!var1.exists()) {
         var1.mkdirs();
      }

      File var2 = new File(SQStructure.USER_EXTEND);
      if (!var2.exists()) {
         var2.mkdirs();
      }

      File var3 = new File(SQStructure.USER_EXTEND_SNIPPETS);
      if (!var3.exists()) {
         var3.mkdirs();
      }

      File var4 = new File(SQStructure.USER_EXTEND_PLUGINS);
      if (!var4.exists()) {
         var4.mkdirs();
      }

      File var5 = new File(SQStructure.USER_EXTEND_RESULTS_PLUGINS);
      if (!var5.exists()) {
         var5.mkdirs();
      }

      File var6 = new File(SQStructure.USER_EXTEND_SNIPPETS + "/SQ/Blocks");
      if (!var6.exists()) {
         var6.mkdirs();
      }
   }

   private void checkMaxLoadingTime() {
      ScheduledExecutorService var1 = Executors.newSingleThreadScheduledExecutor();
      var1.schedule(
         new Runnable() {
            @Override
            public void run() {
               if (!MainApp.AppLoaded) {
                  MainApp.showErrorDialog(
                     L.t("Please restart the program.", new Object[0]),
                     L.t("StrategyQuant X couldn't load correctly.", new Object[0]) + "\n" + L.t("We did some cleanup, it should work now.", new Object[0])
                  );
               }
            }
         },
         5L,
         TimeUnit.MINUTES
      );
   }

   private void deleteDeprecatedFiles() {
      Scanner var1 = null;

      try {
         File var2 = new File(SQStructure.INTERNAL_DIR_PATH + "deprecated.dat");
         if (var2.exists()) {
            String var3 = SQUtils.fileToString(var2);
            var1 = new Scanner(var3);

            while (var1.hasNextLine()) {
               String var4 = var1.nextLine();
               File var5 = new File(MainApp.getDataPath() + var4);
               if (var5.exists()) {
                  var5.delete();
               }
            }
         }
      } catch (Exception var7) {
         if (var1 != null) {
            var1.close();
         }
      }

      try {
         new File(SQStructure.INTERNAL_DIR_PATH + "extend\\Snippets\\SQ\\TradingOptions\\UseInitialSL.java").delete();
      } catch (Exception var6) {
      }
   }

   private void copyUpdaterFiles() {
      String var1 = "StrategyQuantX.exe";
      File var2 = new File(MainApp.getDataPath() + "internal/" + var1);
      if (var2.exists()) {
         Path var3 = Paths.get(MainApp.getDataPath() + "internal/" + var1);
         Path var4 = Paths.get(MainApp.getDataPath() + var1);

         try {
            Files.move(var3, var4, StandardCopyOption.REPLACE_EXISTING);
            Log.info("Updated updater - copied from /internal");
         } catch (IOException var6) {
            Log.error("Move of updater failed. " + var6.getMessage(), var6);
         }
      }
   }

   private void renameSQ4DatabankFiles() {
      String var1 = MainApp.getDataPath() + "user/projects";

      try {
         Stream var2 = Files.find(Paths.get(var1), 5, (var0, var1x) -> String.valueOf(var0).endsWith(".sq4"));

         try {
            var2.map(String::valueOf).forEach(var0 -> {
               File var1x = new File(var0);
               String var2x = var0.replace(".sq4", ".sqx");
               Log.info("Renaming file {} to {}", var0, var2x);
               var1x.renameTo(new File(var2x));
            });
         } catch (Throwable var6) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (var2 != null) {
            var2.close();
         }
      } catch (IOException var7) {
         Log.error("Cannot rename .sq4 files in databanks", var7);
      }
   }

   public void renameFiles(String var1) {
   }

   protected void afterInit() throws Exception {
      AllTimeStats.getInstance().start();
      if (this.isRunInConsole()) {
         Log.info(L.t("HTTP API started, you can access it on http://localhost:%d/call?cmd=-h", new Object[]{MainAppWebServer.port}));
         Log.debug("Starting app in command line mode.");
         this.consoleAppInstance = new SQConsole(this);
         this.consoleAppInstance.run(appArgs);
         BasketOfStocksManager.getInstance().synhronizeAsync(new Runnable() {
            @Override
            public void run() {
               try {
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
               } catch (Exception var2) {
                  SQApp.Log.error("Error while sending WS messages about stockgroup synchronization.", var2);
               }
            }
         });
         BrokerManager.getInstance().synhronizeAsync(new Runnable() {
            @Override
            public void run() {
               try {
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
               } catch (Exception var2) {
                  SQApp.Log.error("Error while sending WS messages about stockgroup synchronization.", var2);
               }
            }
         });
      } else {
         SQInstaller var1 = new SQInstaller();
         var1.checkForUpdatesAsync();
      }
   }

   private void setTimeZone() {
      this.timeZone = TimeZone.getDefault();
      SQTimeOld.setDefaultTimeZone();
      SQTime.init();
   }

   private void initBlocks() throws Exception {
      Blocks.init();
      Formulas.init();
      Negaters.init();
      Checkers.init();
      StrategyBase.init();
      new ParametersSettings();
      this.regenerateWizardConfig(false);
      TALibBlocks.init();
      XMLIndicatorsAnalyzer.init();
      CustomBlocks.load();
   }

   private void regenerateWizardConfig(boolean var1) {
      try {
         String var2 = MainApp.getDataPath() + "internal/web/SQWIZARD/branding/global/config.xml";
         File var3 = new File(var2);
         boolean var4 = true;
         long var5 = new File(SQStructure.SNIPPETS_JAR_PATH).lastModified();

         try {
            long var7 = Long.valueOf(SQUtils.fileToString(new File(SQStructure.WIZARD_HASH_PATH)));
         } catch (Exception var9) {
         }

         if (var1 || var4 || !var3.exists()) {
            BlocksConfigGenerator var11 = new BlocksConfigGenerator();
            var11.run(var2);
            SQUtils.stringToFile(new File(SQStructure.WIZARD_HASH_PATH), var5 + "");
         }
      } catch (Exception var10) {
         Log.error("Cannot regenerate Wizard config.xml. Exc.", var10);
      }
   }

   private void initStatsComputer(boolean var1) throws Exception {
      Log.debug("Initializing stats computer...");
      if (var1) {
         StatsComputer.clearGlobalStatsValues();
      } else {
         StatsComputer.registerOrderComputer(new OrderPLComputer());
      }

      for (DatabankColumn var3 : DatabankColumns.get().getAvailableClasses()) {
         StatsComputer.registerDatabankColumn(var3);
      }

      StatsComputer.createDependencyGraph();
   }

   private void updateLoadingInfo(String var1) {
      if (MainApp.electronWS != null) {
         MainApp.sendMessage("loadingInfo", var1);
      }
   }

   private void iUKTXv8M() {
      boolean var1 = false;

      try {
         Log.debug("Verifying license.");
         Brokers.init();
         Log.debug("Verifying license. Step 2");
         G8SyrBEfO8 var2 = G8SyrBEfO8.getInstance();
         var2.rR6Ivukeqw(var2.eyGcvZ1s());
         Log.info("Hardware ID: " + var2.getHardwareID());
         Log.debug("Verifying license. Step 3");

         try {
            if (var2.licenseFileExists()) {
               Log.debug("Verifying license. Step 4");
               var2.verifyLicenseFromFile();
               ScheduledExecutorService var3 = Executors.newSingleThreadScheduledExecutor();
               var3.schedule(new Runnable() {
                  @Override
                  public void run() {
                     G8SyrBEfO8.checkLicense();
                  }
               }, 5L, TimeUnit.MINUTES);
            }
         } catch (Exception var6) {
            MainApp.license.setLicense(var2.getLastCheckedLicenseCode());
            MainApp.license.setError(var6.getMessage());
            Log.error("License verification failed. Exc. ", var6);
         }

         Tu5UdlpFO9.init(SQStructure.INTERNAL_DIR_PATH);
         if (!this.isRunInConsole()) {
            Log.debug("Verifying license. Step 5");
            Log.info("License dialog initing");
            String var10 = Tu5UdlpFO9.odz0Gyiqhr();
            if (var10 != null && !var10.isEmpty()) {
               MainApp.license.setLicense(var10);
            }

            Log.debug("Verifying license. Step 6");
            if (!G8SyrBEfO8.getInstance().verified()) {
               Log.debug("Verifying license. Step 7");
               BrowserGUI.getInstance().loadLicenseDialogForm();
               Log.debug("Verifying license. Step 8");
               if (!var2.isItTrial() && !var2.isItPartnerTrial() && !var2.isItFullMbg()) {
                  Tu5UdlpFO9.pzi7BBRxNg("");
               } else {
                  Tu5UdlpFO9.pzi7BBRxNg(var2.getLicenseCode());
               }
            }
         } else {
            Log.debug("Verifying license. Step 5 CLI");
            String var9 = null;
            if (appArgs.length > 0 && (appArgs[0].startsWith("liccheck") || appArgs[0].startsWith("license"))) {
               try {
                  String var4 = appArgs[0];
                  var1 = appArgs[0].startsWith("liccheck");
                  if (var4.contains("=")) {
                     String[] var5 = var4.split("=");
                     if (var5.length == 2 && (var5[0].equals("license") || var5[0].equals("liccheck"))) {
                        var9 = var5[1].trim();
                     }
                  }
               } catch (Exception var7) {
                  Log.error("Error while parsing license code from app args.", var7);
               }
            } else if (!G8SyrBEfO8.getInstance().verified()) {
               Log.debug("Verifying license. Step 6 CLI");
               var9 = Tu5UdlpFO9.odz0Gyiqhr();
            }

            if (var9 != null && !var9.isEmpty()) {
               Log.info("Verifying license ...");
               var2.verifyLicenseOnline(var9);
               if (!var2.isItTrial() && !var2.isItPartnerTrial() && !var2.isItFullMbg()) {
                  Tu5UdlpFO9.pzi7BBRxNg("");
               } else {
                  Tu5UdlpFO9.pzi7BBRxNg(var2.getLicenseCode());
               }
            }
         }

         if (!G8SyrBEfO8.getInstance().verified()) {
            Log.info("Missing license.");
            MainApp.exitJVM("Missing license.", 1);
         }

         Lewgre21g.g5Tq3ZdPHp("L-" + MainApp.v571hfnsHw().eHvc2JguAd());
         ScheduledExecutorService var11 = Executors.newSingleThreadScheduledExecutor();
         var11.scheduleAtFixedRate(() -> G8SyrBEfO8.checkLicense(), 3L, 3L, TimeUnit.DAYS);
         Log.debug("License was successfully verified.");
      } catch (Exception | Error var8) {
         Log.info("Failed to check license - " + var8.getMessage());
         Log.error("Failed to check license. Exc. ", var8);
         MainApp.exitJVM("Failed to check license", 1);
      }

      if (var1) {
         Log.info("Exit after license check");
         MainApp.exitJVM("Exit after license check");
      }
   }

   private void initEngines() throws Exception {
      this.updateLoadingInfo(L.t("Initializing engines...", new Object[0]));
      SessionManager.init(SQPaths.dataDirPath);
      InstrumentManager.init(SQPaths.dataDirPath);
      DataManager.init(SQPaths.dataDirPath);
      DataFolderSweeper.get().init();
      BasketOfStocksManager.init(SQPaths.dataDirPath);
      BrokerManager.init(SQPaths.dataDirPath);
      CustomDataManager.init(SQPaths.customDataDirPath);
      JobEngine.init(new JobEngine(0));
      LauncherConfig.getInstance();
      SQProject.init();
      ResultsGroup.init();
      this.addAlias("EURUSD_tick", "EURUSD");
      this.addAlias("EURUSD_fhdb", "EURUSD");
      this.addAlias("GBPUSD_fhdb", "EURUSD");
      this.addAlias("GBPUSD_fhdb - MT4", "EURUSD");
      this.addAlias("EURUSD_fhdb - MT4", "EURUSD");
      this.addAlias("CADCHF_dukasM1", "EURUSD");
      this.addAlias("EURUSD_M1", "EURUSD");
      this.addAlias("AUDCAD_tick", "EURUSD");
      this.addAlias("USDJPY_tick", "USDJPY");
      this.addAlias("EURJPY_M1", "USDJPY");
      HistoryDataManager.get().checkRenamedHistoryTickers();
      SpecialSymbolsManager.updateAll();
      this.updateLoadingInfo(L.t("Loading data...", new Object[0]));
      long var1 = System.currentTimeMillis();
      InstrumentManager.list();
      DataManager.list();
      SessionManager.getSessions();
      CustomDataManager.list();
      BrokerManager.getInstance().getAllBrokers();
      BasketOfStocksManager.getInstance().getGroups();
      HistoryDataManager.get().getStockExchange(null);
      HistoryDataManager.get().getFuturesExchange(null);
      Log.info("Data loaded in {} ms", System.currentTimeMillis() - var1);
      this.updateLoadingInfo(L.t("Loading projects...", new Object[0]));
      var1 = System.currentTimeMillis();
      ProjectEngine.list();
      Log.info("Projects loaded in {} ms", System.currentTimeMillis() - var1);
   }

   private void addAlias(String var1, String var2) throws DataException {
   }

   private void importInstruments() {
   }

   private void initBackgroundTasks() throws Exception {
   }

   protected void initPluginSystem() {
      try {
         Log.debug("Initializing plugins...");
         SQPluginManager.setWorkDirectory(MainApp.getDataPath());
         SQPluginManager.initForProduct(this.getProductCode(), null);
         SQPluginManager.loadPlugins(IAppPlugin.class);
         SQPluginManager.loadPlugins(IConnectionPlugin.class);
         SQPluginManager.loadPlugins(ISettingTabPlugin.class);
         SQPluginManager.loadPlugins(IServletPlugin.class);
         SQPluginManager.loadPlugins(ISQTask.class);
         SQPluginManager.loadPlugins(ILoaderPlugin.class);
         SQPluginManager.loadPlugins(ISaverPlugin.class);
         SQPluginManager.loadPlugins(IProject.class);
         SQPluginManager.loadPlugins(IFitnessFunction.class);
         SQPluginManager.loadPlugins(IEquityChartAddon.class);
         SQPluginManager.loadPlugins(ICrossCheck.class);
         SQPluginManager.loadPlugins(IResultPlugin.class);
         SQPluginManager.loadPlugins(IProjectCondition.class);
         SQPluginManager.loadPlugins(IExchange.class);
      } catch (Exception var2) {
         Log.error("Exception while loading plugins:", var2);
         this.webServer.showErrorDialog("Exception while loading plugins", var2.getMessage());
      }

      System.gc();
   }

   protected void initPrograms() {
      com.strategyquant.tradinglib.results.file.FileHandler var1 = new com.strategyquant.tradinglib.results.file.FileHandler();
      Program.register("Loader", var1);
      Program.register("Saver", var1);
      System.gc();
   }

   private void checkData() {
      DataManager.checkData();
      CustomDataManager.checkData();
   }

   private void importSymbols() throws Exception {
      this.testImportData("History", "TEST_GBPUSD_M1", "M1", 1, MainApp.getDataPath() + "tests/toimport/GBPUSD_M1.csv");
      this.testImportData("History", "TEST_USDJPY_M1", "H1", 1, MainApp.getDataPath() + "tests/toimport/USDJPY_M1_SAMPLE.csv");
   }

   private void testImportData(String var1, String var2, String var3, int var4, String var5) {
      try {
         DataInfo var6 = DataManager.getDataInfo(var1, var2);
         if (var6 != null && var6.rows > 0) {
            String var7 = DataManager.getDataFileName(var1, var2, var3, "No Session");
            File var8 = new File(var7);
            if (var8.exists()) {
               return;
            }

            DataManager.deleteData(var1, var2);
         }

         if (!InstrumentManager.checkInstrumentExists(var2)) {
            InstrumentManager.addInstrument(var2, var2, 100000.0, 1.0E-4, 1.0E-5, 1.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", (byte)3);
         }

         DataManager.addData(var1, var2, var2, var4, 1);
         var6 = DataManager.getDataInfo(var1, var2);
         DataBinWriterNew var11;
         if (var3.equals("TICK")) {
            var11 = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), var6.symbolInfo);
         } else {
            var11 = DataBinWriterNew.getInstance(1, MainApp.getDataPath(), var6.symbolInfo);
         }

         var11.setFileName(DataManager.getDataFileName(var1, var2, var3, "No Session"));
         var11.open();
         this.importData(var1, var3, var11, var6, var5);
         var11.close();
      } catch (Exception var9) {
         Log.error("Exc.", var9);
      }
   }

   private void importData(String var1, String var2, DataBinWriterNew var3, DataInfo var4, String var5) throws Exception {
      ImportDataInfo var6 = new ImportDataInfo();
      var6.filePath = var5;
      var6.rowCount = 10;
      var6.skipCols = 0;
      var6.skipRows = 0;
      var6.separator = ",";
      if (var2.equals("TICK")) {
         var6.dateFormat = "yyyy.MM.dd HH:mm:ss";
         var6.columnTypes = getTickColumnList();
         var6.hasTwoVolumes = true;
      } else {
         var6.dateFormat = "yyyy.MM.dd";
         var6.columnTypes = getOHLCColumnList();
      }

      if (var4.symbol.equals("TS_Test_1")) {
         var6.dateFormat = "MM/dd/yyyy HH:mm";
         var6.skipRows = 1;
         var6.hasTwoVolumes = true;
         var6.columnTypes = getOHLCV2ColumnList();
      }

      DataCsvLoader var7 = new DataCsvLoader();
      var7.setParams(var6, var4, null);
      long var8 = 0L;
      long var10 = 0L;
      int var12 = 0;
      long var13 = 0L;

      try {
         var7.openFile();

         while (var7.readData()) {
            if (var7.isDataCorrect()) {
               var3.writeData(var7.tickData);
               if (var8 == 0L) {
                  var8 = var7.tickData.time;
               }

               var10 = var7.tickData.time;
               var12++;
            }
         }

         var7.close();
         DataCsvLoader.reset();
         DataManager.updateData(var1, var4.symbol, var8, var10, var12, var13, 1, var2, "Etc/UCT");
      } catch (Exception var16) {
         var7.close();
         throw var16;
      }
   }

   public static ArrayList<DefaultCol> getTickColumnList() {
      ArrayList var0 = new ArrayList();
      var0.add(new DateTimeCol());
      var0.add(new BidCol());
      var0.add(new AskCol());
      var0.add(new VolumeCol());
      var0.add(new VolumeCol());
      return var0;
   }

   public static ArrayList<DefaultCol> getOHLCColumnList() {
      ArrayList var0 = new ArrayList();
      var0.add(new DateCol());
      var0.add(new TimeCol());
      var0.add(new OpenCol());
      var0.add(new HighCol());
      var0.add(new LowCol());
      var0.add(new CloseCol());
      var0.add(new VolumeCol());
      return var0;
   }

   public static ArrayList<DefaultCol> getOHLCV2ColumnList() {
      ArrayList var0 = new ArrayList();
      var0.add(new DateCol());
      var0.add(new TimeCol());
      var0.add(new OpenCol());
      var0.add(new HighCol());
      var0.add(new LowCol());
      var0.add(new CloseCol());
      var0.add(new VolumeCol());
      var0.add(new VolumeCol());
      return var0;
   }

   public void beforeExit() {
      super.beforeExit();
      DataToSend var1 = new DataToSend("ExitApp", new JSONObject().put("save", "true"));
      SQWebSocketManager.addToDataQueue(var1, new String[]{"TASKMANAGER"});
      SQWebSocketManager.addToDataQueue(var1, new String[]{"BUILDER"});
      if (MainApp.v571hfnsHw().gWfGtoRYJG()) {
         StrategiesSaver.saveAll(false, "Save all strategies before exiting SQX.");
      }

      AllTimeStats.getInstance().end();
   }

   public void exitApp(boolean var1) {
      super.exitApp();
      this.generateAppVersionFileForUpdater();
      if (DataManager.get() != null) {
         DataManager.flushUpdatedData();
      }

      HistoryDataManager.get().dispose();
      if (!this.isRunInConsole()) {
         if (!MainApp.AppLoaded) {
            String var2 = SQStructure.INTERNAL_DIR_PATH + "/web/SQUANT/filesMD5.txt";
            Log.debug("StrategyQuant X couldn't load correctly. Deleting hash file " + var2);
            new File(var2).delete();
         }

         if (ProjectEngine.isInitialized()) {
            ProjectEngine.stopAll();
         }

         saveWindowSettings();
         MainApp.settings().save();
         TempFilesManager.get().destroy();

         try {
            if (SQGrid.started()) {
               SQGrid.getGridClient().close();
            }
         } catch (Exception var4) {
            Log.error("Error while closing grid engine", var4);
         }

         new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
               MainApp.exitJVM("ok");
            }
         }, var1 ? 3000L : 0L);
      } else {
         MainApp.settings().save();
         TempFilesManager.get().destroy();

         try {
            SQGrid.getGridClient().waitForAllTaskFinished();
            SQGrid.getGridClient().close();
         } catch (Exception var3) {
            Log.error("Error while closing grid engine", var3);
         }

         this.consoleAppInstance.exit();
      }
   }

   public boolean isRunInConsole() {
      return runInConsole();
   }

   private static void saveWindowSettings() {
   }

   public void regenerateLangs() throws Exception {
      LFileGen var1 = new LFileGen();
      var1.start();
   }

   public String getFrameTitle() {
      V571hfnsHw var2 = MainApp.v571hfnsHw();
      File var3 = new File(MainApp.getDataPath() + "/education.txt");
      if (var3.exists()) {
         return "StrategyQuant X";
      }

      String var1 = " StrategyQuant ";
      var1 = var1 + MainApp.printAppVersion(var2.printEdition(), false);
      if (var2.aJg1L21A0t()) {
         var1 = var1 + String.format(" for %s", var2.inygRmSMx7() != null ? var2.inygRmSMx7().getName() : "Unknown");
      }

      if (var2.aVwpAOwrF1()) {
         var1 = var1 + " - Licensed to: " + var2.oOkoIeUsMG() + " - " + var2.fSECzwVwpK();
      }

      var1 = var1 + " " + var2.printType(var2.eHvc2JguAd());
      String var4 = MainApp.settings().get("headerCustomText", null);
      if (var4 != null && !var4.trim().isEmpty()) {
         var1 = var1 + " - " + var4;
      }

      return var1;
   }

   public boolean initWebServer() {
      if (this.webServer != null) {
         CLILogger.log("Web server is already running.");
         return true;
      }

      try {
         this.webServer = new SQWebServer();
         this.webServer.start();
         RemoteAccessConfig.getInstance().forceRemoteAccessEnabled();
         return true;
      } catch (Exception var2) {
         Log.error("Cannot start web server", var2);
         return false;
      }
   }

   public void reloadApp() {
      try {
         Log.debug("Reloading app...");
         this.regenerateWizardConfig(true);
         BlocksConfig.getConfig().reload();
         StrategyGeneratorCache.clear();
         this.initStatsComputer(true);
         MainServlet.getConfigsCache = null;

         try {
            StrategyBase.reload();
         } catch (Exception var2) {
            Log.error("!!! Exception reloading StrategyBase", var2);
         }

         DataToSend var1 = new DataToSend("ReloadUI", new JSONObject().put("reload", "true"));
         SQWebSocketManager.addToDataQueue(var1, new String[]{"SQUANT"});
      } catch (Exception var3) {
         Log.error("Error while reloading app ...", var3);
      }
   }
}
