package com.strategyquant.plugin.Servlet.impl.AlgoWizard;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.volumeProfile.VolumeProfileBlocks;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.atm.ATMConfigVerifier;
import com.strategyquant.tradinglib.backtest.IBacktester;
import com.strategyquant.tradinglib.backtest.SQBacktester;
import com.strategyquant.tradinglib.backtestrunner.StrategyChecker;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.commissions.CommissionsMethodsList;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.generator.Negaters;
import com.strategyquant.tradinglib.generator.StrategyWithVariables;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectCustomResources;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.RandomGroups;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategySaver;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.project.bestresults.BestResultOverview;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.overview.OverviewTemplates;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.strategy.xml.SQ3StrategyConverter;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgoWizardServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(AlgoWizardServlet.class);
   public static final String VAR_ID_LONG_ENTRY = "33333333-1111-1111-3333-333333333333";
   public static final String VAR_ID_LONG_EXIT = "33333333-1111-2222-3333-333333333333";
   public static final String VAR_ID_SHORT_ENTRY = "33333333-2222-1111-3333-333333333333";
   public static final String VAR_ID_SHORT_EXIT = "33333333-2222-2222-3333-333333333333";
   private NegatersList negatersList = null;
   private IBacktester backtester;
   private IResultsGroupProvider rgProvider;
   private static final String RequestFailedFlag = "request-failed";
   private static boolean isBacktestNode;

   public AlgoWizardServlet(IBacktester var1, IResultsGroupProvider var2) {
      this.backtester = var1;
      this.rgProvider = var2;
      isBacktestNode = MainApp.isBacktestNode();
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      Log.debug("AW Request: " + var1);
      if (var1.startsWith("RESULTS")) {
         return this.onResults(var1, var2);
      }

      switch (var1) {
         case "getConfig":
            return this.onGetConfig(var2);
         case "loadFile":
            return this.onLoadFile(var2);
         case "saveFile":
            return this.onSaveFile(var2);
         case "saveToRetester":
            return this.onSaveToRetester(var2);
         case "negate":
            return this.onNegate(var2);
         case "negateCustomBlock":
            return this.onNegateCustomBlock(var2);
         case "backtest":
            return this.onBacktest(var2);
         case "stopBacktest":
            return this.onStopBacktest(var2);
         case "addNewTimeframe":
            return this.onAddNewTimeframe(var2);
         case "saveBlockToTagCloud":
            return this.onSaveBlock(var2);
         case "loadExample":
            return this.onLoadExample(var2);
         case "loadBlockGroups":
            return this.onLoadBlockGroups(var2);
         case "saveBlockGroups":
            return this.onSaveBlockGroups(var2);
         case "loadCustomBlocks":
            return this.onLoadCustomBlocks(var2);
         case "saveCustomBlocks":
            return this.onSaveCustomBlocks(var2);
         case "customDataIndys":
            return this.onCustomDataIndys(var2);
         case "backupsCustomBlocks":
            return this.onListBackupsCustomBlocks(var2);
         case "backupsBlockGroups":
            return this.onListBackupsBlockGroups(var2);
         case "getBacktestResults":
            return this.onGetBacktestResults(var2);
         case "checkResources":
            return this.onCheckResources(var2);
         case "resolveResources":
            return this.onResolveResources(var2);
         default:
            return apiErrorJSON("request-failedEndpoint 'algowizard' - command '" + var1 + "' not found", null);
      }
   }

   private String onCheckResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"strategyXML"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "strategyXML");
      String var4 = this.getParam(var1, "settingsXML", null);

      try {
         Element var5 = XMLUtil.stringToElement(var3);
         Element var6 = var5.getChild("Strategy");
         Element var7 = ProjectCustomResources.checkStrategyResources(var6);
         if (var4 != null) {
            try {
               Element var8 = XMLUtil.stringToElement(var4);
               Element var9 = ProjectResources.checkTaskResources(var8, true);
               if (var9 != null) {
                  List var10 = var9.getChildren();

                  for (int var11 = 0; var11 < var10.size(); var11++) {
                     var7.addContent(((Element)var10.get(var11)).clone());
                  }
               }
            } catch (Exception var12) {
               Log.error("Checking resources from settingsXML failed", var12);
            }
         }

         var2.put("success", L.t("Custom resources checked.", new Object[0]));
         var2.put("resourcesXML", XMLUtil.elementToString(var7));
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot resolve resources.", new Object[0]), var13);
      }

      return var2.toString();
   }

   private String onResolveResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"strategyXML", "resourcesXML"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "strategyXML");
      String var4 = this.tryGetParamValue(var1, "resourcesXML");
      String var5 = this.getParam(var1, "settingsXML", null);

      try {
         Element var6 = XMLUtil.stringToElement(var3);
         Element var7 = XMLUtil.stringToElement(var4);
         var6 = ProjectCustomResources.resolveStrategyResources(var7, var6);
         Element var8 = null;
         if (var5 != null) {
            var8 = XMLUtil.stringToElement(var5);
            JSONArray var9 = new JSONArray();
            boolean var10 = ProjectResources.resolveResources(var7, var8, null, var9, var6);
            var2.put("continueLoading", var10);
            var2.put("newSymbols", var9);
            var2.put("settingsXML", XMLUtil.elementToString(var8));
         }

         var2.put("strategyXML", XMLUtil.elementToString(var6));
         var2.put("success", L.t("Resources resolved.", new Object[0]));
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot resolve resources.", new Object[0]), var11);
      }

      return var2.toString();
   }

   private String onCustomDataIndys(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      String var3;
      try {
         File var4 = new File(SQPaths.algoWizardCustomDataIndysPath);
         var3 = SQUtils.fileToString(var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot get CustomDataIndys.", new Object[0]), var5);
      }

      var2.put("xml", var3);
      var2.put("success", L.t("CustomDataIndys loaded.", new Object[0]));
      return var2.toString();
   }

   protected byte[] executeBinary(String var1, Map<String, String[]> var2, String var3) throws Exception {
      String var4 = null;

      try {
         if (var1.endsWith("/")) {
            var1 = var1.replaceFirst("/", "");
         }

         if (var1.startsWith("RESULTS/webstatic")) {
            var1 = var1.replaceAll("RESULTS/webstatic", "");
            if (var1.contains("plugins/")) {
               var4 = MainApp.getDataPath() + var1;
            } else {
               var4 = MainApp.getDataPath() + "internal/web/" + var1;
            }

            return Files.readAllBytes(new File(var4).toPath());
         } else {
            if (var1.equals("RESULTS")) {
               var1 = var1 + "/index.html";
            }

            var4 = MainApp.getDataPath() + "internal/web/" + var1;
            return Files.readAllBytes(new File(MainApp.getDataPath() + "internal/web/" + var1).toPath());
         }
      } catch (Exception var6) {
         throw new Exception("request-failed -> Endpoint 'algowizard'. File not found: " + var4);
      }
   }

   private String onResults(String var1, Map<String, String[]> var2) throws Exception {
      String var3 = null;

      try {
         if (var1.endsWith("/")) {
            var1 = var1.replaceFirst("/", "");
         }

         if (var1.startsWith("RESULTS/webstatic")) {
            var1 = var1.replaceAll("RESULTS/webstatic", "");
            if (var1.contains("plugins/")) {
               var3 = MainApp.getDataPath() + var1;
            } else {
               var3 = MainApp.getDataPath() + "internal/web/" + var1;
            }

            return SQUtils.fileToString(var3);
         } else {
            if (var1.equals("RESULTS")) {
               var1 = var1 + "/index.html";
            }

            var3 = MainApp.getDataPath() + "internal/web/" + var1;
            return SQUtils.fileToString(MainApp.getDataPath() + "internal/web/" + var1);
         }
      } catch (Exception var5) {
         Log.error("request-failed -> Endpoint 'algowizard'. File not found: " + var3);
         return apiErrorJSON("request-failed -> File not found.", null);
      }
   }

   private String onGetConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         var2.put("wizardConfig", SQUtils.fileToString(new File(SQPaths.wizardConfigPath)));
         String var3 = SQStructure.INTERNAL_DIR_PATH + "web/" + "AlgoWizard" + "/assets/InitialStrategy.xml";
         var2.put("initialStrategyConfig", SQUtils.fileToString(new File(var3)));
         var2.put("recentFiles", AlgoWizardRecentFiles.listJSON());
         var2.put("blocksTagCloud", AlgoWizardBlocksTagCloud.listJSON());
         var2.put("timeframes", WSDataObjects.getTimeframes(false).getDataArray());
         var2.put("algoWizardType", System.getProperty("algoWizardType"));
         var2.put("build", MainApp.getAppVersion());
         var2.put("skins", this.getSkinList());

         try {
            var2.put("zoom", Double.parseDouble(MainApp.settings().get("zoom")));
         } catch (Exception var5) {
            var2.put("zoom", 1.0);
         }

         JSONObject var4 = this.getAiChatBackendUrls();
         if (var4.length() > 0) {
            var2.put("aiChatBackendUrls", var4);
         }

         var2.put("volumeProfile", VolumeProfileSubscription.toJSON());
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot get wizard config.", new Object[0]), var6);
      }

      var2.put("success", L.t("Config loaded.", new Object[0]));
      return var2.toString();
   }

   private JSONObject getAiChatBackendUrls() {
      JSONObject var1 = new JSONObject();
      this.addAiChatBackendUrlIfPresent(var1, "chat", "aichat-chat");
      this.addAiChatBackendUrlIfPresent(var1, "chatDev", "aichat-chatDev");
      this.addAiChatBackendUrlIfPresent(var1, "pseudocodeToXml", "aichat-pseudocodeToXml");
      this.addAiChatBackendUrlIfPresent(var1, "usage", "aichat-usage");
      this.addAiChatBackendUrlIfPresent(var1, "bugReport", "aichat-bugReport");
      this.addAiChatBackendUrlIfPresent(var1, "credits", "aichat-credits");
      this.addAiChatBackendUrlIfPresent(var1, "tokenizerStockpicker", "aichat-tokenizerStockpicker");
      this.addAiChatBackendUrlIfPresent(var1, "tokenizerStandard", "aichat-tokenizerStandard");
      return var1;
   }

   private void addAiChatBackendUrlIfPresent(JSONObject var1, String var2, String var3) {
      String var4 = MainApp.settings().get(var3, "").trim();
      if (!var4.isEmpty()) {
         var1.put(var2, var4);
      }
   }

   private JSONArray getSkinList() {
      JSONArray var1 = new JSONArray();
      File var2 = new File(SQStructure.PLUGINS_DIR);
      File[] var3 = var2.listFiles();
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var3.length; var5++) {
         File var6 = var3[var5];
         if (var6.isDirectory() && var6.getName().toLowerCase().startsWith("skin")) {
            String var7 = var6.getAbsolutePath() + "/module.js";
            var4.add(var7);
         }
      }

      for (int var13 = 0; var13 < var4.size(); var13++) {
         File var14 = new File((String)var4.get(var13));
         if (var14.exists()) {
            try {
               String var15 = SQUtils.fileToString(var14);
               int var8 = var15.indexOf("sqPluginProvider.plugin");
               if (var8 >= 0) {
                  int var9 = var15.indexOf("{", var8);
                  if (var9 >= 0) {
                     int var10 = var15.indexOf("}", var9);
                     if (var10 >= 0) {
                        String var11 = var15.substring(var9, var10 + 1);
                        var1.put(new JSONObject(var11));
                     }
                  }
               }
            } catch (Exception var12) {
               Log.error("Loading skin plugin failed", var12);
            }
         }
      }

      return var1;
   }

   private String onLoadFile(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "filePath")[0];
         String var4 = SQUtils.getExtension(var3);
         if (var4.equals("sqw")) {
            Element var5 = XMLUtil.fileToXmlElement(new File(var3));
            if (var5 != null) {
               String var6 = var5.getAttributeValue("version");

               try {
                  if (var6 != null && Double.parseDouble(var6) < 3.8) {
                     var5 = SQ3StrategyConverter.getInstance().convert(var5);
                  }

                  var5 = StrategyFixer.fixStrategy(XMLUtil.elementToString(var5));
               } catch (Exception var9) {
                  Log.error("Modifying XML config failed", var9);
               }
            }

            var2.put("strategyConfig", XMLUtil.elementToString(var5));
         } else {
            IProgram var11 = Program.get("Loader");
            ResultsGroup var12 = (ResultsGroup)var11.call("loadFile", new Object[]{var3});
            Element var7 = var12.getStrategyXml();
            StrategyXMLModifier.addOptions(var7, var12.getName(), var3);
            String var8 = XMLUtil.elementToString(var7);
            var2.put("strategyConfig", var8);
            var2.put("lastSettings", var12.getLastSettings());
         }

         new File(var3);
         var2.put("strategyName", SQUtils.stripExtension(new File(var3).getName()));
         AlgoWizardRecentFiles.save(var3);
         var2.put("recentFiles", AlgoWizardRecentFiles.listJSON());
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Loading failed.", new Object[0]), var10);
      }

      var2.put("success", L.t("File loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSaveFile(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         boolean var3 = Boolean.parseBoolean(this.tryGetParam(var1, "chooseFolder")[0]);
         String var4 = this.tryGetParam(var1, "strategyConfig")[0];
         String var5 = this.getParam(var1, "strategyName", null);
         var4 = SQUtils.xmlPrettyFormat(var4);
         String var6 = this.getParam(var1, "lastSettings", null);
         boolean var7 = false;

         try {
            var7 = Boolean.parseBoolean(this.tryGetParam(var1, "backtested")[0]);
         } catch (Exception var29) {
         }

         String var8 = null;
         if (var3) {
            String var9 = null;

            try {
               var9 = this.tryGetParam(var1, "defaultFolder")[0];
            } catch (Exception var30) {
               if (MainApp.settings().containsKey("awSaveFile")) {
                  var9 = MainApp.settings().get("awSaveFile");
               } else {
                  var9 = SQPaths.projectsDirPath;
               }
            }

            var8 = this.chooseFilePath(var9, var5);
         } else {
            var8 = this.tryGetParam(var1, "filePath")[0];
         }

         if (var8 == null) {
            throw new Exception("Saving canceled");
         }

         File var40 = new File(var8);
         boolean var10 = var40.exists();
         if (!var10) {
            var40.getParentFile().mkdirs();
         }

         JarOutputStream var11 = null;
         ZipInputStream var12 = null;
         File var13 = null;
         byte[] var14 = new byte[1024];
         int var15 = 0;

         try {
            try {
               if (var7 && this.backtester instanceof SQBacktester) {
                  String var16 = this.tryGetParam(var1, "backtestID")[0];
                  SQBacktester.getResultsGroup(var16);
               }
            } catch (Exception var28) {
               var7 = false;
            }

            if (var7 && this.backtester instanceof SQBacktester) {
               String var44 = this.tryGetParam(var1, "backtestID")[0];
               ResultsGroup var46 = SQBacktester.getResultsGroup(var44);
               var6 = var46.getLastSettings();
               var6 = this.addResources(var6);
               var46.setLastSettings(var6);
               StrategySaver var48 = new StrategySaver();
               var48.save(var46, "sqx", var8, false, 0, var1);
            } else {
               var6 = this.addResources(var6);
               if (!var10) {
                  var11 = new JarOutputStream(new FileOutputStream(var40));
                  JarEntry var43 = new JarEntry("strategy_Portfolio.xml");
                  var11.putNextEntry(var43);
                  var11.write(var4.getBytes("UTF8"));
                  if (var6 != null) {
                     JarEntry var45 = new JarEntry("lastSettings.xml");
                     var11.putNextEntry(var45);
                     var11.write(var6.getBytes("UTF8"));
                  }
               } else {
                  var13 = new File(var8 + "_temp");
                  Files.copy(var40.toPath(), var13.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  var12 = new ZipInputStream(new FileInputStream(var13));
                  var11 = new JarOutputStream(new FileOutputStream(var40));

                  while (true) {
                     ZipEntry var42 = var12.getNextEntry();
                     if (var42 == null) {
                        break;
                     }

                     String var17 = var42.getName();
                     if (var17.startsWith("strategy_Portfolio.xml")) {
                        JarEntry var47 = new JarEntry("strategy_Portfolio.xml");
                        var11.putNextEntry(var47);
                        var11.write(var4.getBytes("UTF8"));
                     } else if (var17.startsWith("lastSettings.xml") && var6 != null) {
                        JarEntry var18 = new JarEntry("lastSettings.xml");
                        var11.putNextEntry(var18);
                        var11.write(var6.getBytes("UTF8"));
                     } else {
                        var11.putNextEntry(var42);

                        while ((var15 = var12.read(var14)) > 0) {
                           var11.write(var14, 0, var15);
                        }
                     }
                  }
               }
            }
         } catch (Exception var31) {
            Log.error("Saving exception. ", var31);
         } finally {
            if (var11 != null) {
               var11.close();
            }

            if (var12 != null) {
               var12.close();
            }

            if (var13 != null) {
               var13.delete();
            }
         }

         var2.put("filePath", var8);

         try {
            var2.put("newStrategyName", SQUtils.stripExtension(new File(var8).getName()));
         } catch (Exception var27) {
            Log.error("Error while saving file. ", var27);
         }

         AlgoWizardRecentFiles.save(var8);
         var2.put("recentFiles", AlgoWizardRecentFiles.listJSON());
      } catch (Exception var33) {
         return apiErrorJSON(L.t("Saving failed.", new Object[0]), var33);
      }

      var2.put("success", L.t("File saved.", new Object[0]));
      return var2.toString();
   }

   private String addResources(String var1) {
      if (var1 == null) {
         return null;
      }

      try {
         Element var2 = XMLUtil.stringToElement(var1);
         ProjectResources.addDataResources(var2);
         return XMLUtil.elementToString(var2);
      } catch (Exception var3) {
         Log.error("Adding resources failed", var3);
         return var1;
      }
   }

   private String onSaveToRetester(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "strategyConfig")[0];
         String var4 = null;
         String var5 = null;

         try {
            var4 = this.tryGetParam(var1, "strategyName")[0];
         } catch (Exception var12) {
            var4 = "AlgoWizardStrategy";
         }

         try {
            var5 = this.tryGetParam(var1, "lastSettings")[0];
         } catch (Exception var11) {
         }

         boolean var6 = false;

         try {
            var6 = Boolean.parseBoolean(this.tryGetParam(var1, "backtested")[0]);
         } catch (Exception var10) {
         }

         try {
            if (var6 && this.backtester instanceof SQBacktester) {
               String var7 = this.tryGetParam(var1, "backtestID")[0];
               SQBacktester.getResultsGroup(var7);
            }
         } catch (Exception var9) {
            var6 = false;
         }

         ResultsGroup var15 = null;
         if (var6 && this.backtester instanceof SQBacktester) {
            String var8 = this.tryGetParam(var1, "backtestID")[0];
            var15 = SQBacktester.getResultsGroup(var8).clone();
            var15.setName(var4);
         } else {
            var15 = new ResultsGroup(var4);
            var15.addSubresult(var4, new SettingsMap());
            var15.setLastSettings(var5);
            var15.computeAllStats();
            var15.portfolio().addStrategyXml(XMLUtil.stringToElement(var3));
         }

         SQProject var17 = ProjectEngine.get("Retester");
         var17.getResultsDatabank().add(var15, true);
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Saving failed.", new Object[0]), var13);
      }

      var2.put("success", L.t("Strategy saved to Retester.", new Object[0]));
      return var2.toString();
   }

   private String chooseFilePath(String var1, String var2) throws Exception {
      throw new Exception("Not implemented for Electron.");
   }

   private String onGetBacktestResults(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         if (this.backtester instanceof SQBacktester) {
            String var4 = this.tryGetParam(var1, "backtestID")[0];
            var3 = SQBacktester.getResultsGroup(var4);
         } else {
            var3 = this.rgProvider.get(var1, "AlgoWizardServlet");
         }

         Element var24 = XMLUtil.stringToElement(var3.getLastSettings());
         Element var5 = var24.getChild("Data");
         ChartSetups var6 = ProjectConfigHelper.getChartSetups(var5);
         ChartSetup var7 = var6.getMainSetup();
         ChartDef var8 = var7.getMainChart();
         String var9 = var7.getSymbol();
         String var10 = var7.getTimeframe();
         String var11 = SQTime.toUIDateString(var8.getHistoryFrom()) + " - " + SQTime.toUIDateString(var8.getHistoryTo());
         String var12 = Engines.getEngineName(var7.getBacktestEngine());
         String var13 = Precisions.toString(var7.getTestPrecision());
         JSONObject var14 = new JSONObject();
         var14.put("data", var9 + " / " + var10 + ", " + var11);
         var14.put("engine", var12);
         var14.put("precision", var13);
         var2.put("info", var14);
         BestResultOverview var15 = new BestResultOverview();
         String var16 = var15.print(var3, (byte)127);
         var16 = var16.substring(var16.indexOf("<body"));
         var16 = var16.substring(0, var16.indexOf("</body>") + 7);
         var2.put("stats", var16);
         OverviewTemplate var17 = (OverviewTemplate)OverviewTemplates.getInstance().findClassByName("SQDefaultPct");
         var16 = var17.drawValues(var3, var3.getMainResultKey(), new StatsTypeCombination((byte)0, (byte)10, (byte)127));
         String var18 = var16.substring(var16.indexOf("<div class=\"performance\">"));
         var18 = var18.substring(0, var18.indexOf("</div>") + 6);
         var2.put("monthlyPerfPct", var18);
      } catch (Exception var22) {
         return apiErrorJSON(L.t("Saving failed.", new Object[0]), var22);
      } finally {
         if (var3 != null) {
            var3.releaseLock("AlgoWizardServlet");
         }
      }

      var2.put("success", L.t("Backtest results loaded.", new Object[0]));
      return var2.toString();
   }

   private String onBacktest(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      Log.info("Incoming backtest request");

      try {
         String var3 = this.tryGetParam(var1, "strategyName")[0];
         String var4 = this.tryGetParam(var1, "strategyXML")[0];
         String var5 = this.tryGetParam(var1, "settings")[0];
         var4 = var4.replace("&", "&#38;");
         var5 = var5.replace("&", "&#38;");
         var4 = var4.replace("___", " ");
         var5 = var5.replace("___", " ");
         boolean var6 = false;
         boolean var7 = false;
         boolean var8 = false;
         String var9 = "NONE";

         try {
            var6 = Boolean.parseBoolean(this.tryGetParam(var1, "doBacktest")[0]);
         } catch (Exception var54) {
         }

         try {
            var7 = Boolean.parseBoolean(this.tryGetParam(var1, "doNegation")[0]);
         } catch (Exception var53) {
         }

         try {
            var8 = Boolean.parseBoolean(this.tryGetParam(var1, "doSimpleBacktest")[0]);
         } catch (Exception var52) {
         }

         try {
            var9 = this.tryGetParam(var1, "webSocketID")[0];
         } catch (Exception var51) {
         }

         String var10 = this.getParam(var1, "brokerSymbols", null);
         if (var10 != null && var10.trim().isEmpty()) {
            var10 = null;
         }

         Element var11 = XMLUtil.stringToElement(var5);
         String var12 = "No Session";
         if (isBacktestNode) {
            try {
               String var13 = this.getParam(var1, "session", null);
               String var14 = this.getParam(var1, "userID", null);
               Log.debug("Checking session...");
               if (var13 != null && var14 != null) {
                  Element var15 = XMLUtil.stringToElement(var13);
                  var12 = var15.getAttributeValue("name");
                  Log.debug("Session: " + var12);
                  if (!var12.equals("No Session")) {
                     var12 = var14 + "_" + var12;
                     var15.setAttribute("name", var12);
                     Session var16 = new Session();
                     var16.setFromXML(var15);
                     Session var17 = SessionManager.getSession(var12);
                     if (var17 == null) {
                        SessionManager.addSession(var16);
                        Log.debug("Session added to SessionManager: " + var16.getSessionName());
                     } else if (!var16.toText().equals(var17.toText())) {
                        SessionManager.updateSession(var16);
                        Log.debug("Session updated in SessionManager: " + var16.getSessionName());
                     } else {
                        Log.debug("Session definition already exists in SessionManager: " + var16.getSessionName());
                     }

                     Element var18 = var11.getChild("Options").getChild("BuildTradingOptions");
                     Element var19 = XMLUtil.tryAddElement(var18, "Params");
                     List var20 = var19.getChildren("Param");

                     for (int var21 = 0; var21 < var20.size(); var21++) {
                        Element var22 = (Element)var20.get(var21);

                        try {
                           String var23 = var22.getAttributeValue("className");
                           if (var23.equals("SessionOption")) {
                              var22.setText(var12);
                              Log.debug("Session option changed in settingsXML successfully");
                              break;
                           }
                        } catch (Exception var55) {
                           Log.error("Exc.", var55);
                        }
                     }
                  }
               }
            } catch (Exception var56) {
               throw new Exception("Failed to load Session data. " + var56.getMessage(), var56);
            }
         }

         if (isBacktestNode && var4 != null && var4.contains("key=\"CBlock_")) {
            throw new Exception("Custom blocks are not supported yet in online version");
         }

         String var63;
         if (var1.containsKey("backtestID")) {
            var63 = this.tryGetParam(var1, "backtestID")[0];
         } else {
            var63 = UUID.randomUUID().toString();
         }

         String var64 = null;
         if (var1.containsKey("userID")) {
            var64 = this.tryGetParam(var1, "userID")[0];
         }

         String var65 = null;
         if (var1.containsKey("accountID")) {
            var65 = this.tryGetParam(var1, "accountID")[0];
         }

         String var66 = null;
         if (var1.containsKey("userSecretCode")) {
            var66 = this.tryGetParam(var1, "userSecretCode")[0];
         }

         double var67 = 0.0;
         if (var7) {
            try {
               var4 = this.negateSignals(var4);
               var2.put("negatedXML", var4);
            } catch (Exception var50) {
               Log.error("Negation error - ", var50);
               var2.put("negationError", var50.getMessage());
               var2.put("negatedXML", var4);
            }
         }

         int var69 = this.getEngine(var11);
         if (var69 == 1316847364) {
            String var71 = this.getMainSymbol(var11);
            String var74 = this.getParam(var1, "groupSymbols", null);
            this.checkStockpickerData(var71, var69, var64, var74, var11);
         }

         if (var6) {
            ChartSetups var72 = null;
            Object var75 = null;
            MoneyManagementMethod var77 = null;
            Object var79 = null;
            CommissionsMethod var24 = CommissionsMethodsList.create("None", new Object[0]);
            new SwapMethod();
            ATM var26 = null;
            if (!isBacktestNode && (var69 == 56756755 || var69 == 938213070)) {
               var12 = ProjectResources.getSession(var11);
            }

            double var27 = 0.0;
            Element var31 = var11.getChild("Data");

            try {
               if (!var12.equals("No Session")) {
                  Element var32 = var31.getChild("Setups");
                  List var33 = var32.getChildren("Setup");

                  for (int var34 = 0; var34 < var33.size(); var34++) {
                     Element var35 = (Element)var33.get(var34);
                     var35.setAttribute("session", var12);
                  }
               }

               var72 = ProjectConfigHelper.getChartSetups(var31);
            } catch (Exception var57) {
               throw new Exception("Unable to load backtest charts - " + var57.getMessage(), var57);
            }

            var69 = var72.getMainSetup().getBacktestEngine();

            for (int var83 = 0; var83 < var72.size(); var83++) {
               ChartSetup var85 = (ChartSetup)var72.get(var83);
               ArrayList var87 = var85.getCharts();

               for (int var91 = 0; var91 < var87.size(); var91++) {
                  ChartDef var36 = (ChartDef)var87.get(var91);
                  String var37 = var36.getSymbol();
                  DataInfo var38 = DataManager.getDataInfo("History", var37);
                  if (var38 == null) {
                     throw new Exception(L.t("Symbol with name '%s' doesn't exist.", new Object[]{var37}));
                  }

                  if (var38.rows == 0 || var38.dateFrom == 0L || var38.dateTo == 0L) {
                     throw new Exception(L.t("No data available for symbol '%s'.", new Object[]{var37}));
                  }
               }
            }

            Element var84;
            if (var4.contains("Position Score")) {
               var84 = StrategyFixer.fixStrategy(var4);
            } else {
               var84 = XMLUtil.stringToElement(var4);
            }

            VolumeProfileBlocks.checkStrategyAllowed(var84, var3);
            if (!StrategyChecker.strategyValid(var84) && var69 != 1316847364 && var69 != -1816889229) {
               var2.put("error", "Strategy doesn't contain any Rule or order open block");
               return var2.toString();
            }

            Element var86 = var11.getChild("RiskMoneyManagement");
            var79 = ProjectConfigHelper.getRiskManagement(var86.getChild("RiskManagement"));
            var77 = ProjectConfigHelper.getMoneyManagement(var86.getChild("MoneyManagement"));
            double var29 = var77.getInitialCapital();
            var75 = ProjectConfigHelper.getOptions(var11.getChild("Options").getChild("BuildTradingOptions"));

            try {
               Element var88 = var31.getChild("Setups").getChild("Setup");
               var24 = ProjectConfigHelper.getCommissionsMethod(var88);
            } catch (Exception var49) {
               throw new Exception("Cannot get commissions method", var49);
            }

            SwapMethod var25;
            try {
               Element var89 = var31.getChild("Setups").getChild("Setup");
               var25 = ProjectConfigHelper.getSwapMethod(var89);
            } catch (Exception var48) {
               throw new Exception("Cannot get swap method", var48);
            }

            try {
               var27 = Double.parseDouble((String)ProjectConfigHelper.getChartSetupValues(var31, "slippage").get(0));
            } catch (Exception var47) {
               throw new Exception("Cannot get slippage value", var47);
            }

            try {
               var67 = Double.parseDouble((String)ProjectConfigHelper.getChartSetupValues(var31, "minDist").get(0));
            } catch (Exception var46) {
               throw new Exception("Cannot get min distance value", var46);
            }

            Element var90 = var11.getChild("ATMs");
            if (var90 != null) {
               var26 = new ATM(var90);
               ATMConfigVerifier.verifyConfig(var11);
            }

            if (var8) {
               for (int var92 = 0; var92 < var72.size(); var92++) {
                  ChartSetup var94 = (ChartSetup)var72.get(var92);
                  var94.setTestPrecision(1);
                  var94.setMinDistance(0.0);
                  ArrayList var96 = var94.getCharts();

                  for (int var97 = 0; var97 < var96.size(); var97++) {
                     ChartDef var39 = (ChartDef)var96.get(var97);
                     String var40 = var39.getSymbol();
                     DataInfo var41 = DataManager.getDataInfo("History", var40);
                     if (var41 != null && var41.rows > 0) {
                        long var42 = var41.dateTo - 31536000000L;
                        if (var69 == 1316847364) {
                           var42 = SQTime.addYears(var41.dateTo, -5);
                        }

                        long var44 = var41.dateTo;
                        var39.setHistoryFrom(var42 < var41.dateFrom ? var41.dateFrom : var42);
                        var39.setHistoryTo(var44);
                     }
                  }
               }
            }

            ChartSetup var93 = (ChartSetup)var72.get(0);
            long var95 = var93.getMainChart().getHistoryFrom();
            long var98 = var93.getMainChart().getHistoryTo();
            if (var95 < 0L || var98 < 0L) {
               throw new Exception("Invalid history settings. Date from or Date to is smaller than 0.");
            }

            if (var98 <= var95) {
               throw new Exception(L.t("Date to %s must be greater than Date from %s.", new Object[]{SQTime.toDateString(var95), SQTime.toDateString(var98)}));
            }

            Log.info(
               String.format(
                  "Running %s backtest for %s/%s, from %s to %s",
                  var8 ? "simple " : "full",
                  var93.getSymbol(),
                  var93.getTimeframe(),
                  SQTime.toDateString(var95),
                  SQTime.toDateString(var98)
               )
            );
            if (var69 == 1316847364 || var69 == -1816889229) {
               String var99 = var77.getEngine();
               if (!var99.equals("SP,SA")) {
                  throw new Exception(
                     L.t(
                        "The specified MM method %s is not supported by Stockpicker engine.",
                        new Object[]{var77.printFormatedName(), SQTime.toDateString(var98)}
                     )
                  );
               }
            }

            HashMap var100 = new HashMap();
            var100.put("StrategyName", var3);
            var100.put("StrategyXml", var84);
            var100.put("StrategyObject", StrategyBase.createXmlStrategy(var84, var3));
            var100.put("ChartSetups", var72);
            var100.put("MinDistance", var67);
            var100.put("Slippage", var27);
            var100.put("Commission", var24);
            var100.put("Swap", var25);
            var100.put("MoneyManagement.InitialCapital", var29);
            var100.put("MoneyManagement.Method", var77);
            var100.put("RiskManagement", var79);
            var100.put("FitnessFunction", this.getFitnessFunction());
            var100.put("TradingOptions", var75);
            var100.put("OutOfSample", new OutOfSample());
            var100.put("StrategyLastSettings", var11);
            var100.put("UserID", var64);
            var100.put("AccountID", var65);
            var100.put("SimpleBacktest", var8);
            var100.put("UserSecretCode", var66);
            var100.put("ATM", var26);
            var100.put("IsAlgoWizard", true);
            var100.put("BrokerSymbols", var10);
            var100.put("webSocketID", var9);
            this.backtester.run(var63, var72.getMainSetup(), var100);
            var2.put("backtestID", var63);
            var2.put("backtestDateFrom", SQTime.toUIDateString(var95));
            var2.put("backtestDateTo", SQTime.toUIDateString(var98));
         }

         var2.put("strategyXML", var4);
         var2.put("success", L.t("Backtest started.", new Object[0]));
      } catch (Exception var58) {
         Log.error("Backtest failed", var58);
         var2.put("error", "Backtest failed - " + var58.getMessage());
      }

      return var2.toString();
   }

   private void checkStockpickerData(String var1, int var2, String var3, String var4, Element var5) throws Exception {
      if (var2 == 1316847364) {
         String var6 = var1;
         String var7 = null;
         boolean var10 = false;
         DataInfo var9 = DataManager.getDataInfo("History", var1);
         if (var9 != null) {
            Log.info(
               String.format(
                  "Symbol found, symbol=%s, basketId=%d, available data from %s to %s",
                  var9.symbol,
                  var9.basketId,
                  SQTime.toDateString(var9.dateFrom),
                  SQTime.toDateString(var9.dateTo)
               )
            );
         }

         if (var9 == null || var9.basketId < 0) {
            boolean var11 = var4 != null && !var4.trim().isEmpty();
            if (var11) {
               Log.info(String.format("Stockpicker custom group definition, symbol=%s, userID=%s, groupSymbols=%s", var1, var3, var4));
               if (var3 == null || var3.trim().isEmpty()) {
                  throw new Exception("Custom group definition - userID cannot be null.");
               }

               var1 = var1.replace("[", "").replace("]", "");
               var7 = "[" + var1 + "_" + var3 + "]";
               int var8 = Math.abs((var7 + var4).hashCode()) * -1;
               BasketDto var12 = BasketOfStocksManager.getInstance().getBasket(var8);
               if (var12 == null) {
                  BasketOfStocksManager.getInstance().createCustomGroup(var7, var8, var4, false, var3);
               }

               var9 = DataManager.getDataInfo("History", var7);
               if (var9 == null) {
                  var9 = DataManager.addCustomData("History", var7, var8, var6);
               }

               var9.basketId = var8;
               Log.info(String.format("Stockpicker - custom group created %d", var8));
               var10 = true;
            } else if (var9 == null) {
               throw new DataException(2, L.t("Group with name %s doesn't exist", new Object[]{var1}));
            }

            if (var10) {
               Log.info(String.format("Updating settings XML with newly created symbol for custom group: %s", var7));
               List var18 = var5.getChild("Data").getChild("Setups").getChild("Setup").getChildren("Chart");

               for (int var13 = 0; var13 < var18.size(); var13++) {
                  Element var14 = (Element)var18.get(var13);
                  String var15 = var14.getAttributeValue("symbol");
                  if (var15.equals(var6) || var15.equals("Same as main chart")) {
                     var14.setAttribute("symbol", var7);
                     Log.info(String.format("Updated chart %d symbol to %s", var13, var7));
                  }
               }
            }
         }
      }
   }

   private int getEngine(Element var1) throws Exception {
      try {
         Element var2 = var1.getChild("Data");
         Element var3 = var2.getChild("Setups").getChild("Setup");
         return Engines.getEngine(var3.getAttributeValue("engine"));
      } catch (Exception var5) {
         throw new Exception("Failed to parse engine.", var5);
      }
   }

   private String getMainSymbol(Element var1) throws Exception {
      try {
         Element var2 = var1.getChild("Data");
         Element var3 = var2.getChild("Setups").getChild("Setup").getChild("Chart");
         String var4 = var3.getAttributeValue("symbol");
         return var4 == null ? null : XMLUtil.removeXMLCharacters(var4);
      } catch (Exception var5) {
         throw new Exception("Failed to parse main symbol.", var5);
      }
   }

   private String onNegate(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "strategyXML")[0];

         try {
            if (this.isStandardStrategy(var3)) {
               var2.put("strategyXML", this.negateSignals(var3));
            } else {
               var2.put("strategyXML", this.negateStockpicker(var3));
            }
         } catch (Exception var5) {
            Log.error("Cannot perform negation", var5);
            var2.put("strategyXML", var3);
            var2.put("negationError", var5.getMessage());
         }
      } catch (Exception var6) {
         Log.error("Cannot perform negation", var6);
         return apiErrorJSON("Can't perform negation", var6);
      }

      var2.put("success", L.t("Negation done.", new Object[0]));
      return var2.toString();
   }

   private boolean isStandardStrategy(String var1) {
      return !var1.contains("pickerEditor=\"true\"");
   }

   private String onNegateCustomBlock(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = "<StrategyFile><Strategy><Variables/></Strategy></StrategyFile>";
         String var4 = "<signal>" + this.tryGetParam(var1, "blockXML")[0] + "</signal>";
         StrategyWithVariables var5 = new StrategyWithVariables(XMLUtil.stringToElement(var3));
         Element var6 = XMLUtil.stringToElement(var4);
         Element var7 = this.negateBlockFromSignal(var6, var5);
         this.fixCustomParamVariables(var7);
         String var8 = XMLUtil.elementToString(var7);
         var2.put("negatedBlock", var8);
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot perform signal negation.", new Object[0]), var9);
      }

      var2.put("success", L.t("Signal negated", new Object[0]));
      return var2.toString();
   }

   private void fixCustomParamVariables(Element var1) {
      String var2 = var1.getAttributeValue("variableType");
      if (var2 != null && var2.equals("CustomParam")) {
         var1.setAttribute("customParam", "true");
         String var3 = var1.getText();
         if (var3.startsWith("#")) {
            var1.setAttribute("value", var3);
         }

         var1.removeAttribute("variable");
         var1.removeAttribute("variableType");
      }

      List var5 = var1.getChildren();

      for (int var4 = 0; var4 < var5.size(); var4++) {
         this.fixCustomParamVariables((Element)var5.get(var4));
      }
   }

   private String negateSignals(String var1) throws Exception {
      Element var2 = XMLUtil.stringToElement(var1);
      Element var3 = this.getSignalRule(var2);
      StrategyWithVariables var4 = new StrategyWithVariables(var2);
      boolean var5 = false;
      Element var6 = this.getSignal(var3, "33333333-1111-1111-3333-333333333333");
      if (var6 != null) {
         Element var7 = this.getSignal(var3, "33333333-2222-1111-3333-333333333333");
         if (var7 == null) {
            throw new Exception("No short entry signal found!");
         }

         Element var8 = this.negateBlockFromSignal(var6, var4);
         if (var8 != null) {
            var7.removeContent();
            var7.addContent(var8);
            var5 = true;
         }
      }

      Element var11 = this.getSignal(var3, "33333333-1111-2222-3333-333333333333");
      if (var11 != null) {
         Element var12 = this.getSignal(var3, "33333333-2222-2222-3333-333333333333");
         if (var12 == null) {
            throw new Exception("No short exit signal found!");
         }

         Element var9 = this.negateBlockFromSignal(var11, var4);
         if (var9 != null) {
            var12.removeContent();
            var12.addContent(var9);
            var5 = true;
         }
      }

      if (var5) {
         var1 = XMLUtil.xmlToString(var2);
      }

      Element var13 = this.getEntryRuleThen(var2, 1);
      Element var14 = this.getEntryRuleThen(var2, 2);
      if (var13 != null && var14 != null) {
         Element var10 = this.negateActionBlock(var13, var4);
         var14.removeChildren("Item");
         var14.addContent(var10);
         var5 = true;
      }

      if (var5) {
         var1 = XMLUtil.xmlToString(var2);
      }

      return var1;
   }

   private String negateStockpicker(String var1) throws Exception {
      Element var2 = XMLUtil.stringToElement(var1);
      StrategyWithVariables var3 = new StrategyWithVariables(var2);
      boolean var4 = false;
      Element var5 = this.getSPRule(var2, "Long");
      if (var5 == null) {
         throw new Exception("Cannot find SP long rule!");
      }

      Element var6 = this.getSPRule(var2, "Short");
      if (var6 == null) {
         throw new Exception("Cannot find SP short rule!");
      }

      Element var7 = var5.getChild("Entry");
      if (var7 == null) {
         throw new Exception("No long entry signal found!");
      }

      Element var8 = var6.getChild("Entry");
      if (var8 == null) {
         throw new Exception("No short entry signal found!");
      }

      Element var9 = this.negateBlockFromSignal(var7, var3);
      if (var9 != null) {
         var8.removeContent();
         var8.addContent(var9);
         var4 = true;
      }

      Element var10 = this.getSPExit(var5);
      if (var10 != null) {
         Element var11 = this.getSPExit(var6);
         if (var11 != null) {
            var9 = this.negateBlockFromSignal(var10, var3);
            if (var9 != null) {
               var11.removeContent();
               var11.addContent(var9);
               var4 = true;
            }
         }
      }

      Element var15 = var5.getChild("Order");
      if (var15 == null) {
         throw new Exception("No long order found!");
      }

      Element var12 = var6.getChild("Order");
      if (var12 == null) {
         throw new Exception("No short order found!");
      }

      var9 = this.negateActionBlock(var15, var3);
      if (var9 != null) {
         var12.removeContent();
         var12.addContent(var9);
         var4 = true;
      }

      if (var4) {
         var1 = XMLUtil.xmlToString(var2);
      }

      return var1;
   }

   private Element getSPExit(Element var1) {
      List var2 = var1.getChildren("Exit");
      return var2.size() > 1 ? (Element)var2.get(1) : null;
   }

   private Element getSPRule(Element var1, String var2) throws Exception {
      List var3 = var1.getChild("Strategy").getChild("Rules").getChild("Events").getChildren("Event");
      Element var4 = null;

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         if (var6.getAttributeValue("key").equals("OnBarUpdate")) {
            var4 = var6;
            break;
         }
      }

      if (var4 == null) {
         throw new Exception("No OnBarUpdate event found in strategy.");
      }

      List var8 = var4.getChildren("Rule");
      if (var8.size() == 0) {
         throw new Exception("No rules found in OnBarUpdate event.");
      }

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Element var7 = (Element)var8.get(var9);
         if (var7.getAttributeValue("type").equals("StockpickerEntryExit") && var7.getAttributeValue("name").equals(var2)) {
            return var7;
         }
      }

      return null;
   }

   private Element negateActionBlock(Element var1, StrategyWithVariables var2) throws Exception {
      Element var3 = var1.getChild("Item").clone();
      if (this.negatersList == null) {
         this.negatersList = Negaters.list();
      }

      IBlock var4 = Blocks.getBlockObject(var3.getAttributeValue("key"), var2, var3);
      IBlock var5 = this.negatersList.negate(var4, var2);
      return Blocks.generateBlockTreeXml(var5);
   }

   private Element getSignalRule(Element var1) throws Exception {
      Element var2 = var1.getChild("Strategy").getChild("Rules").getChild("Events").getChild("Event").getChild("Rule");
      if (!var2.getAttributeValue("type").equals("Signal") && !var2.getAttributeValue("type").equals("SignalFuzzy")) {
         throw new Exception("Rule is not a signal rule!");
      } else {
         return var2;
      }
   }

   private Element getEntryRuleThen(Element var1, int var2) throws Exception {
      List var3 = var1.getChild("Strategy").getChild("Rules").getChild("Events").getChild("Event").getChildren("Rule");
      if (var3.size() <= 1) {
         throw new Exception("There are not enough rules!");
      } else {
         Element var4 = (Element)var3.get(var2);
         if (!var4.getAttributeValue("type").equals("IfThen")) {
            throw new Exception("Rule is not an If-Then rule!");
         } else {
            return var4.getChild("Then");
         }
      }
   }

   private Element negateBlockFromSignal(Element var1, StrategyWithVariables var2) throws Exception {
      List var3 = var1.getChildren();
      if (var3.size() == 0) {
         Log.debug("No conditions defined");
         return null;
      }

      if (var3.size() > 1) {
         throw new Exception("Signal has more than one child!");
      }

      Element var4 = (Element)var3.get(0);
      Element var5 = var4.clone();
      if (this.negatersList == null) {
         this.negatersList = Negaters.list();
      }

      IBlock var6 = Blocks.getBlockObject(var5.getAttributeValue("key"), var2, var5);
      IBlock var7 = this.negatersList.negate(var6, var2);
      return Blocks.generateBlockTreeXml(var7);
   }

   private Element getSignal(Element var1, String var2) throws Exception {
      List var3 = var1.getChild("signals").getChildren("signal");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getAttributeValue("variable").equals(var2)) {
            return var5;
         }
      }

      return null;
   }

   private String onStopBacktest(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "backtestID")[0];
         this.backtester.stop(var3);
      } catch (Exception var4) {
         var2.put("error", var4.getMessage());
      }

      var2.put("success", L.t("Backtest stopped.", new Object[0]));
      return var2.toString();
   }

   private long getDateFrom(long var1, long var3) {
      Calendar var5 = Calendar.getInstance();
      var5.setTimeInMillis(var3);
      var5.add(1, -2);
      long var6 = var5.getTimeInMillis();
      return var1 < var6 ? var6 : var1;
   }

   private IFitnessFunction getFitnessFunction() throws Exception {
      String var1 = "<Rankings><FitnessCriteria method=\"ComputeFromStrategyResult\"><Settings><Ranking type=\"NetProfit\" /></Settings></FitnessCriteria></Rankings>";
      return ProjectConfigHelper.getFitnessFunction(XMLUtil.stringToElement(var1));
   }

   private String onAddNewTimeframe(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "timeframe")[0];
         TimeframeManager.addTimeframe(var3);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getTimeframes(), new String[]{"SQUANT", "QDM"});
      } catch (Exception var4) {
         var2.put("error", var4.getMessage());
      }

      var2.put("success", L.t("Timeframe added.", new Object[0]));
      return var2.toString();
   }

   private String onSaveBlock(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "blockKey")[0];
         String var4 = this.tryGetParam(var1, "blockType")[0];
         String var5 = this.tryGetParam(var1, "blockParams")[0];
         AlgoWizardBlocksTagCloud.save(var3, var4, var5);
         var2.put("blocksTagCloud", AlgoWizardBlocksTagCloud.listJSON());
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Saving failed.", new Object[0]), var6);
      }

      var2.put("success", L.t("Block saved.", new Object[0]));
      return var2.toString();
   }

   private String onLoadExample(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "fileName")[0];
         String var4 = MainApp.getDataPath() + "internal/web/AlgoWizard/examples/" + var3;
         IProgram var5 = Program.get("Loader");
         ResultsGroup var6 = (ResultsGroup)var5.call("loadFile", new Object[]{var4});
         var2.put("lastSettingsXML", var6.getLastSettings());
         var2.put("sourceCode", XMLUtil.elementToString(var6.getStrategyXml()));
         var2.put("fileName", SQUtils.stripExtension(var3));
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot load example.", new Object[0]), var7);
      }

      var2.put("success", L.t("Example loaded.", new Object[0]));
      return var2.toString();
   }

   private String onLoadBlockGroups(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = XMLUtil.elementToString(RandomGroups.getXml());
         var2.put("blockGroupsXML", var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Loading block groups failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Block groups loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSaveBlockGroups(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"blockGroupsXML"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "blockGroupsXML");
         RandomGroups.save(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Saving block groups failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Block groups saved.", new Object[0]));
      return var2.toString();
   }

   private String onLoadCustomBlocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         var2.put("customBlocksXML", XMLUtil.elementToString(CustomBlocks.get()));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Loading custom blocks failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Custom blocks loaded.", new Object[0]));
      return var2.toString();
   }

   private String onListBackupsBlockGroups(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         var2.put("backups", RandomGroups.listBackupFiles());
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Loading backups failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Backups loaded.", new Object[0]));
      return var2.toString();
   }

   private String onListBackupsCustomBlocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         var2.put("backups", CustomBlocks.listBackupFiles());
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Loading backups failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Backups loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSaveCustomBlocks(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"customBlocksXML"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "customBlocksXML");
         CustomBlocks.save(XMLUtil.stringToElement(var3));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Saving custom blocks failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Custom blocks saved.", new Object[0]));
      return var2.toString();
   }
}
