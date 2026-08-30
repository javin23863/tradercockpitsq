package com.strategyquant.plugin.Portfolio.impl.Composer;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.memory.SQMemoryProtectionError;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.StrategyLoader;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioComposerServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioComposerServlet.class);
   private PortfolioComposerResultsSender sender = new PortfolioComposerResultsSender();
   private StopPauseEngine stopPauseEngine;
   private PortfolioComposerSimulator simulator;
   private ProgressEngine progressEngine;

   public PortfolioComposerServlet() {
      try {
         this.progressEngine = new ProgressEngine("PortfolioComposer");
         this.simulator = new PortfolioComposerSimulator();
         this.stopPauseEngine = new StopPauseEngine();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "recompute":
            return this.onRecompute(var2);
         case "stop":
            return this.onStop(var2);
         case "loadFiles":
            return this.onLoadFiles(var2);
         case "loadGridData":
            return this.onLoadGridData(var2);
         case "removeReports":
            return this.onRemoveReports(var2);
         case "removeAllReports":
            return this.onRemoveAllReports(var2);
         case "addBuyHoldStrategy":
            return this.onAddBuyHoldStrategy(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String onAddBuyHoldStrategy(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "symbol")[0];

      try {
         SQProject var4 = ProjectEngine.get("PortfolioComposer");
         Databank var5 = var4.getResultsDatabank();
         DataInfo var6 = DataManager.getDataInfo("History", var3);
         if (var6 == null || var6.symbolInfo == null) {
            throw new TradingException(String.format("Instrument info for symbol '%s doesn't exist", var3));
         }

         String var7 = "StrBuyHold-" + var3;
         String var8 = SQStructure.PLUGINS_DIR + "PortfolioComposer/StrSingleAsset.sqx";
         ResultsGroup var9 = StrategyLoader.loadStrategy(var4, var7, var8, 100.0);
         var9.specialValues().set(SpecialValues.PortfolioComposerHodlSymbol, var3);
         var9.setName(var7);
         var5.add(var9, true);
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Adding strategy failed.", new Object[0]), var10);
      }

      var2.put("success", L.t("Strategy added.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveReports(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "results")[0];
         String[] var4 = var3.split(",");
         SQProject var5 = ProjectEngine.get("PortfolioComposer");
         Databank var6 = var5.getResultsDatabank();
         ProjectConfigHelper.delete(var5, var6, var4, "onRemoveReports");
         var6.addExtraDatabankChange("remove", "DatabankEmpty");
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Removing reports failed.", new Object[0]), var7);
      }

      var2.put("success", L.t("Reports removed.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveAllReports(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         SQProject var3 = ProjectEngine.get("PortfolioComposer");
         Databank var4 = var3.getResultsDatabank();
         var4.removeAll(true, String.format("API request portfoliocomposer/removeAllReports from databank %s/%s", var3.getName(), var4.getName()));
         var4.addExtraDatabankChange("remove", "DatabankEmpty");
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Removing reports failed.", new Object[0]), var5);
      }

      var2.put("success", L.t("Reports removed.", new Object[0]));
      return var2.toString();
   }

   private String onLoadGridData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      SQProject var4 = ProjectEngine.get("PortfolioComposer");
      Databank var5 = var4.getResultsDatabank();
      JSONArray var6 = new JSONArray();
      HashSet var7 = new HashSet();
      boolean var8 = false;
      ArrayList var9 = var5.getRecords();

      for (int var10 = 0; var10 < var9.size(); var10++) {
         ResultsGroup var11 = (ResultsGroup)var9.get(var10);
         String var12 = var11.getName();

         try {
            MoneyManagementMethod var13 = ProjectConfigHelper.getMoneyManagement(
               XMLUtil.stringToElement(var11.getLastSettings()).getChild("RiskMoneyManagement").getChild("MoneyManagement")
            );
            String var14 = var13.printFormatedName();
            if (!var7.isEmpty() && !var7.contains(var14)) {
               var8 = true;
            }

            var7.add(var14);
            double var15 = var13.getInitialCapital();
            var6.put(var12 + " - " + var14 + " / " + PlTypes.printPL(var15, (byte)10));
         } catch (Exception var17) {
            Log.error(String.format("Error while parsing the MoneyManagement method used for strategy %s", var12), var17);
            var6.put(var12 + " - NA");
         }

         DatabankColumn var18 = (DatabankColumn)DatabankColumns.get().findClassByName("MiniEquityChart");
         String var19 = var18.getValue(var11, "Portfolio", (byte)0, (byte)10, (byte)127);
         JSONArray var20 = new JSONArray();
         var20.put(var11.getName());
         var20.put(var19);
         var3.put(var20);
      }

      var2.put("mmMethods", var6);
      var2.put("mmMethodsDifferent", var8);
      var2.put("rows", var3);
      var2.put("success", L.t("Data loaded.", new Object[0]));
      return var2.toString();
   }

   private String onRecompute(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "results")[0];
      String var3 = this.tryGetParam(var1, "weights")[0];
      String[] var4 = var2.split(",");
      String[] var5 = var3.split(",");
      final PortfolioComposerSettings var6 = new PortfolioComposerSettings();
      var6.dateRangeLimited = this.tryGetParam(var1, "dateRange")[0].equals("limited");
      var6.startDay = SQTime.parseDateToMilis(this.tryGetParam(var1, "startDay")[0]);
      var6.endDay = SQTime.parseDateToMilis(this.tryGetParam(var1, "endDay")[0]);
      var6.initialCapital = Double.valueOf(this.tryGetParam(var1, "initialCapital")[0]);
      var6.leverage = Integer.valueOf(this.tryGetParam(var1, "leverage")[0]);
      var6.decimals = 5;
      final boolean var7 = this.tryGetParam(var1, "autocomputation")[0].equals("true");
      if (var7) {
         String var8 = this.getParam(var1, "selectionType", "SharpRatio");
         var6.setSelectionTypeByString(var8);
         var6.maxSimulations = Integer.parseInt(((String[])var1.get("maxSimulations"))[0]);
         int var9 = Integer.parseInt(((String[])var1.get("confLevel"))[0]);
         var6.confidenceLevel = Double.valueOf((double)var9) / 100.0;
         var6.RiskFreeRate = Double.parseDouble(((String[])var1.get("riskFreeRate"))[0]);
      }

      if (var4.length != var5.length) {
         throw new Exception("The lengths of the results / weights arrays do not match.");
      }

      if (var4.length > 50) {
         throw new Exception("Select max 50 strategies to create a portfolio.");
      }

      if (this.stopPauseEngine.getRunningStatus() == 1) {
         throw new Exception(L.t("Computing Portfolio is already running. Cannot start another process.", new Object[0]));
      }

      if (!MainApp.v571hfnsHw().aDm88fRJB2() && var4.length > 4) {
         throw new Exception(L.t("Portfolio Composer in Pro version is limited to maximum 4 source strategies.", new Object[0]));
      }

      if (MainApp.v571hfnsHw().bDm88fRJA3()) {
         throw new Exception(L.t("PortfolioComposer is available only in full version, not in this special trial.", new Object[0]));
      }

      final SQProject var20;
      try {
         var20 = ProjectEngine.get("PortfolioComposer");
         Databank var21 = (Databank)var20.getDatabanks().get("Results");

         for (int var10 = 0; var10 < var4.length; var10++) {
            String var11 = var4[var10];
            double var12 = Double.valueOf(var5[var10]);
            ResultsGroup var14 = var21.getLocked(var11, "PortfolioComposer");
            var6.strategies.add(var14);
            var6.weights.add(var12);
         }
      } finally {
         for (ResultsGroup var17 : var6.strategies) {
            var17.releaseLock("PortfolioComposer");
         }
      }

      (new Thread() {
            @Override
            public void run() {
               PortfolioComposerServlet.this.simulator
                  .start(var20, var6, PortfolioComposerServlet.this.sender, PortfolioComposerServlet.this.stopPauseEngine, var7);
            }
         })
         .start();
      JSONObject var22 = new JSONObject();
      var22.put("success", "ok");
      return var22.toString();
   }

   private String onLoadFiles(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      final String[] var3 = this.tryGetParam(var1, "filePaths[]");
      (new Thread() {
         @Override
         public void run() {
            PortfolioComposerServlet.this.loadFiles(var3);
            PortfolioComposerServlet.Log.info("Strategies loaded");
         }
      }).start();
      var2.put("success", L.t("Loading strategies.", new Object[0]));
      return var2.toString();
   }

   private void loadFiles(String[] var1) {
      Throwable var2 = null;
      SQProject var3 = null;
      Databank var4 = null;

      try {
         var3 = ProjectEngine.get("PortfolioComposer");
         var4 = var3.getResultsDatabank();
         var3.bestResults.disable();
         var4.disableSynchronization();
         var3.publisher.resetLastData("progress-channel");
         String var5 = null;
         boolean var6 = false;
         double var7 = 0.0;
         double var9 = 100.0 / var1.length;
         StrategyLoader.init();

         for (int var11 = 0; var11 < var1.length; var11++) {
            try {
               File var12 = new File(var1[var11]);
               if (!var12.isDirectory()) {
                  if (!SQProject.isMemoryProtectionUsed() && !var6) {
                     try {
                        MemoryUsageChecker.checkAvailableMemory();
                     } catch (SQMemoryProtectionError var20) {
                        var3.userConfirm
                           .awaitConfirmation(
                              L.t("Memory usage warning", new Object[0]),
                              L.t("SQ is running out of memory - current consumption reached 85%.", new Object[0])
                                 + "<br>"
                                 + L.t("If you'll continue, SQ might crash or freeze when memory runs out.", new Object[0])
                                 + "<br>"
                                 + L.t(
                                    "We recommend you to stop further strategies loading and eventually to increase available memory in performance settings.",
                                    new Object[0]
                                 )
                                 + "<br><br>"
                                 + L.t("Do you want to stop loading remaining strategies?", new Object[0])
                           );
                        if (var3.userConfirm.isConfirmed()) {
                           break;
                        }

                        var6 = true;
                     }
                  }

                  var5 = SQUtils.stripExtension(var12.getName());
                  ResultsGroup var13 = StrategyLoader.loadStrategy(var3, var5, var1[var11], var7);
                  var7 += var9;
                  if (var13 == null) {
                     break;
                  }

                  var4.add(var13, true);
               }
            } catch (Exception var21) {
               Log.error("Error while loading strategies. Exc.", var21);
               var2 = var21;
            } catch (Error var22) {
               Log.error("Error while loading strategies. Exc.", var22);
               var2 = var22;
               break;
            }
         }
      } catch (Exception var23) {
         Log.error("Error while loading strategies. Exc.", var23);
         var2 = var23;
      } finally {
         var4.enableSynchronization();
      }

      var3.bestResults.enable();
      if (!StrategyLoader.isCanceled()) {
         var3.getProgress()
            .update("load", 100.0, var2 == null ? null : "Some strategies were not loaded because of errors.<br>Last error: " + var2.getMessage());
      }
   }

   private String onStop(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      this.stopPauseEngine.stop();
      System.gc();
      var2.put("success", "ok");
      return var2.toString();
   }
}
