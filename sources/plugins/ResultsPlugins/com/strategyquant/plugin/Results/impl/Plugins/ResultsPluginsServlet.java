package com.strategyquant.plugin.Results.impl.Plugins;

import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.DcOjahK4ng;
import com.strategyquant.lib.L88OaFjjon.G8SyrBEfO8;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsPluginsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ResultsPluginsServlet.class);
   private static final String LOCK_RESULTS_PLUGINS = "ResultsPlugins";
   private static IResultsGroupProvider rgProvider;
   private static final int CUSTOM_PLUGINS_LIMIT_STARTER = 0;
   private static final int CUSTOM_PLUGINS_LIMIT_PRO = 3;

   public ResultsPluginsServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "list":
            return this.listPlugins(var2);
         case "create":
            return this.createPlugin(var2);
         case "rename":
            return this.renamePlugin(var2);
         case "delete":
            return this.deletePlugin(var2);
         case "stats":
            return this.getStats(var2);
         case "orders":
            return this.getOrders(var2);
         case "settings":
            return this.getLastSettingsXml(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String listPlugins(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         File var3 = new File(SQStructure.USER_EXTEND_RESULTS_PLUGINS);
         ArrayList var4 = new ArrayList();
         List var5 = new ArrayList();
         File[] var6 = var3.listFiles();
         if (var6 != null) {
            for (File var10 : var6) {
               if (var10.isDirectory() && !var10.getName().endsWith(".json")) {
                  File var11 = new File(var10, "index.html");
                  if (var11.exists()) {
                     String var12 = var10.getName();
                     if (!var12.equals("CustomPlugin")) {
                        if (!var12.equals("Prop analytics") && !var12.equals("Prop Monte Carlo")) {
                           var5.add(var12);
                        } else {
                           var4.add(var12);
                        }
                     }
                  }
               }
            }
         }

         Collections.sort(var4);
         Collections.sort(var5);
         int var14 = G8SyrBEfO8.getInstance().getLicenseEdition();
         if (var14 == DcOjahK4ng.EDITION_STARTER) {
            if (!var5.isEmpty()) {
               Log.info("ResultsPlugins: {} custom plugin(s) found but not loaded - Starter plan does not allow custom plugins.", var5.size());
            }

            var5.clear();
         } else if (var14 == DcOjahK4ng.EDITION_PROFESIONAL && var5.size() > 3) {
            Log.info("ResultsPlugins: {} custom plugin(s) found, loading only first {} - Pro plan limit.", var5.size(), 3);
            var5 = var5.subList(0, 3);
         }

         JSONArray var15 = new JSONArray();
         String var16 = BrowserGUI.getInstance().getAppUrl();

         for (String var19 : var4) {
            JSONObject var21 = new JSONObject();
            var21.put("name", var19);
            var21.put("url", var16 + "/plugins/" + var19 + "/index.html");
            var21.put("isDefault", true);
            var15.put(var21);
         }

         for (String var20 : var5) {
            JSONObject var22 = new JSONObject();
            var22.put("name", var20);
            var22.put("url", var16 + "/plugins/" + var20 + "/index.html");
            var22.put("isDefault", false);
            var15.put(var22);
         }

         var2.put("plugins", var15);
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Failed to load results plugins.", new Object[0]), var13);
      }

      var2.put("success", "Plugins listed");
      return var2.toString();
   }

   private String renamePlugin(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.getParam(var1, "pluginName", null);
         String var4 = this.getParam(var1, "newName", null);
         if (var3 == null || var4 == null || var4.trim().isEmpty()) {
            return apiErrorJSON(L.t("Plugin name and new name are required.", new Object[0]), null);
         }

         var3 = var3.trim();
         var4 = var4.trim();
         if (!var4.matches("^[a-zA-Z0-9_\\s-]+$") || var4.contains("..") || var4.contains("/") || var4.contains("\\")) {
            return apiErrorJSON(L.t("Invalid plugin name.", new Object[0]), null);
         }

         File var5 = new File(SQStructure.USER_EXTEND_RESULTS_PLUGINS);
         File var6 = new File(var5, var3);
         File var7 = new File(var5, var4);
         if (!var6.isDirectory()) {
            return apiErrorJSON(L.t("Plugin not found.", new Object[0]), null);
         }

         if (var7.exists()) {
            return apiErrorJSON(L.t("A plugin with this name already exists.", new Object[0]), null);
         }

         Files.move(var6.toPath(), var7.toPath());
         var2.put("success", L.t("Plugin renamed.", new Object[0]));
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Failed to rename plugin.", new Object[0]), var8);
      }

      return var2.toString();
   }

   private String deletePlugin(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.getParam(var1, "pluginName", null);
         if (var3 == null || var3.trim().isEmpty()) {
            return apiErrorJSON(L.t("Plugin name is required.", new Object[0]), null);
         }

         var3 = var3.trim();
         if (var3.contains("..") || var3.contains("/") || var3.contains("\\")) {
            return apiErrorJSON(L.t("Invalid plugin name.", new Object[0]), null);
         }

         if (var3.equals("CustomPlugin")) {
            return apiErrorJSON(L.t("Cannot delete template plugin.", new Object[0]), null);
         }

         File var4 = new File(SQStructure.USER_EXTEND_RESULTS_PLUGINS);
         File var5 = new File(var4, var3);
         if (!var5.isDirectory()) {
            return apiErrorJSON(L.t("Plugin not found.", new Object[0]), null);
         }

         this.deleteRecursively(var5);
         var2.put("success", L.t("Plugin removed.", new Object[0]));
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Failed to remove plugin.", new Object[0]), var6);
      }

      return var2.toString();
   }

   private void deleteRecursively(File var1) throws IOException {
      if (var1.isDirectory()) {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               this.deleteRecursively(var6);
            }
         }
      }

      Files.delete(var1.toPath());
   }

   private int countCustomPlugins(File var1) {
      int var2 = 0;
      File[] var3 = var1.listFiles();
      if (var3 != null) {
         for (File var7 : var3) {
            if (var7.isDirectory() && !var7.getName().endsWith(".json") && new File(var7, "index.html").exists()) {
               String var8 = var7.getName();
               if (!var8.equals("CustomPlugin") && !var8.equals("Prop analytics") && !var8.equals("Prop Monte Carlo")) {
                  var2++;
               }
            }
         }
      }

      return var2;
   }

   private String createPlugin(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.getParam(var1, "pluginName", null);
         if (var3 == null || var3.trim().isEmpty()) {
            return apiErrorJSON(L.t("Plugin name is required.", new Object[0]), null);
         }

         var3 = var3.trim();
         if (!var3.matches("^[a-zA-Z0-9_\\s-]+$")) {
            return apiErrorJSON(L.t("Plugin name may only contain letters, numbers, spaces, hyphens and underscores.", new Object[0]), null);
         }

         if (var3.contains("..") || var3.contains("/") || var3.contains("\\")) {
            return apiErrorJSON(L.t("Invalid plugin name.", new Object[0]), null);
         }

         int var4 = G8SyrBEfO8.getInstance().getLicenseEdition();
         if (var4 == DcOjahK4ng.EDITION_STARTER) {
            return apiErrorJSON(
               L.t("Custom plugins are not available in the Starter plan. Upgrade to Pro or Ultimate to use this feature.", new Object[0]), null
            );
         }

         File var5 = new File(SQStructure.USER_EXTEND_RESULTS_PLUGINS);
         if (var4 == DcOjahK4ng.EDITION_PROFESIONAL) {
            int var6 = this.countCustomPlugins(var5);
            Log.info("ResultsPlugins createPlugin: currentCount={}, limit={}", var6, 3);
            if (var6 >= 3) {
               Log.warn("ResultsPlugins: Pro plan limit reached ({} plugins), blocking creation of '{}'.", var6, var3);
               return apiErrorJSON(
                  L.t("Custom plugin limit reached. Pro plan allows up to %d custom plugins. Upgrade to Ultimate for unlimited plugins.", new Object[]{3}),
                  null
               );
            }
         }

         File var12 = new File(var5, "CustomPlugin");
         File var7 = new File(var12, "index.html");
         if (!var7.exists()) {
            return apiErrorJSON(L.t("Custom Plugin template not found.", new Object[0]), null);
         }

         File var8 = new File(var5, var3);
         if (var8.exists()) {
            return apiErrorJSON(L.t("A plugin with this name already exists.", new Object[0]), null);
         }

         if (!var8.mkdirs()) {
            return apiErrorJSON(L.t("Failed to create plugin folder.", new Object[0]), null);
         }

         File var9 = new File(var8, "index.html");
         Files.copy(var7.toPath(), var9.toPath(), StandardCopyOption.REPLACE_EXISTING);
         var2.put("success", L.t("Plugin created.", new Object[0]));
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Failed to create plugin.", new Object[0]), var10);
      }

      return var2.toString();
   }

   private String getStats(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         var3 = rgProvider.get(var1, "ResultsPlugins");
         String var4 = this.getParam(var1, "resultKey", "Portfolio");
         byte var5 = Byte.parseByte(this.getParam(var1, "direction", "0"));
         byte var6 = Byte.parseByte(this.getParam(var1, "sampleType", "127"));
         byte var7 = Byte.parseByte(this.getParam(var1, "plType", "10"));
         Result var8 = var3.subResult(var4);
         if (var8 == null) {
            throw new Exception("Result not found");
         }

         SQStats var9 = var8.statsOrNull(var5, var7, var6);
         if (var9 == null) {
            throw new Exception("Stats not found");
         }

         JSONObject var10 = new JSONObject();
         var10.put("NetProfit", var9.getDouble("NetProfit"));
         var10.put("NumberOfTrades", var9.getInt("NumberOfTrades"));
         String var11 = var7 == 20 ? "DrawdownPct" : (var7 == 30 ? "DrawdownPips" : "Drawdown");
         var10.put("Drawdown", var9.getDouble(var11));
         var10.put("PctDrawdown", var9.getDouble("DrawdownPct"));
         var10.put("PipsDrawdown", var9.getDouble("DrawdownPips"));
         var10.put("AvgProfitByDay", var9.getDouble("AvgProfitPerDay"));
         var10.put("AvgProfitByMonth", var9.getDouble("AvgProfitPerMonth"));
         var10.put("AvgProfitByYear", var9.getDouble("AvgProfitPerYear"));
         var10.put("AvgPctProfitByYear", var9.getDouble("AvgPctProfitPerYear"));
         var10.put("AvgTrade", var9.getDouble("AvgTrade"));
         var10.put("AvgAbsTrade", var9.getDouble("AvgAbsTrade"));
         var10.put("AvgTradesPerDay", var9.getDouble("AvgTradesPerDay"));
         var10.put("AvgTradesPerMonth", var9.getDouble("AvgTradesPerMonth"));
         var10.put("AvgTradesPerYear", var9.getDouble("AvgTradesPerYear"));
         var10.put("ProfitFactor", var9.getDouble("ProfitFactor"));
         var10.put("SharpeRatio", var9.getDouble("SharpeRatio"));
         var10.put("Stability", var9.getDouble("Stability"));
         var10.put("Symmetry", var9.getDouble("Symmetry"));
         var10.put("ReturnDDRatio", var9.getDouble("ReturnDDRatio"));
         var10.put("ReturnOpenDDRatio", var9.getDouble("ReturnOpenDDRatio"));
         var10.put("AAR_DD_Ratio", var9.getDouble("AnnualPctReturnDDRatio"));
         var10.put("CalmarRatio", var9.getDouble("CalmarRatio"));
         var10.put("RExpectancy", var9.getDouble("RExpectancy"));
         var10.put("RExpectancyScore", var9.getDouble("RExpectancyScore"));
         var10.put("SQN", var9.getDouble("SQN"));
         var10.put("SQNScore", var9.getDouble("SQNScore"));
         var10.put("WinLossRatio", var9.getDouble("WinLossRatio"));
         var10.put("WinningPct", var9.getDouble("WinningPct"));
         var10.put("PayoutRatio", var9.getDouble("PayoutRatio"));
         var10.put("AvgBarsTrade", var9.getDouble("AvgBarsInTrade"));
         var10.put("AHPR", var9.getDouble("AHPR"));
         var10.put("ZScore", var9.getDouble("ZScore"));
         var10.put("ZProbability", var9.getDouble("ZProbability"));
         var10.put("Expectancy", var9.getDouble("Expectancy"));
         var10.put("StandardDev", var9.getDouble("StandardDev"));
         var10.put("Exposure", var9.getDouble("Exposure", 0.0));
         var10.put("ExposurePosition", var9.getDouble("ExposurePosition", 0.0));
         var10.put("StagnationPeriod", var9.getInt("Stagnation"));
         var10.put("StagnationPeriodPct", var9.getDouble("StagnationPct"));
         var10.put("StagnationFrom", var9.getInt("StagnationFrom"));
         var10.put("StagnationTo", var9.getInt("StagnationTo"));
         var10.put("DegreesOfFreedom", var9.getInt("DegreesOfFreedom"));
         var10.put("NumberOfProfits", var9.getInt("NumberOfProfits"));
         var10.put("NumberOfLosses", var9.getInt("NumberOfLosses"));
         var10.put("NumberOfCanceled", var9.getInt("NumberOfCanceled"));
         var10.put("GrossProfit", var9.getDouble("GrossProfit"));
         var10.put("GrossLoss", var9.getDouble("GrossLoss"));
         var10.put("AvgWin", var9.getDouble("AvgWin"));
         var10.put("AvgLoss", var9.getDouble("AvgLoss"));
         var10.put("MaxProfit", var9.getDouble("MaxProfit"));
         var10.put("MaxLoss", var9.getDouble("MaxLoss"));
         var10.put("MaxConsecWins", var9.getInt("MaxConsecWins"));
         var10.put("MaxConsecLoss", var9.getInt("MaxConsecLosses"));
         var10.put("AvgConsecWin", var9.getDouble("AvgConsecWins"));
         var10.put("AvgConsecLoss", var9.getDouble("AvgConsecLosses"));
         var10.put("AvgBarsWin", var9.getDouble("AvgBarsWin"));
         var10.put("AvgBarsLoss", var9.getDouble("AvgBarsLoss"));
         var10.put("TotalTradingDays", var9.getInt("TotalTradingDays"));
         var10.put("TotalTradingMonths", var9.getInt("TotalTradingMonths"));
         var10.put("TotalTradingYears", var9.getInt("TotalTradingYears"));
         var10.put("ProfitableMonths", var9.getInt("ProfitableMonths"));
         var10.put("Commission", var9.getDouble("Commission"));
         var10.put("SlippageInMoney", var9.getDouble("SlippageInMoney"));
         var10.put("CommSwapInMoney", var9.getDouble("CommSwapInMoney"));
         var10.put("NetProfitInPips", var9.getDouble("NetProfitInPips"));
         var10.put("MaxNewHighDurationFrom", var9.getInt("MaxNewHighDurationFrom"));
         var10.put("MaxNewHighDurationTo", var9.getInt("MaxNewHighDurationTo"));
         var10.put("MaxNewHighDurationPeriod", var9.getInt("MaxNewHighDuration"));
         var10.put("MaxNewHighDurationPct", var9.getDouble("MaxNewHighDurationPct"));
         var2.put("stats", var10);
      } catch (RecordNotFoundException var16) {
         Log.info("onPrint: " + var16.getMessage());
         return apiErrorJSONNoLog(null, var16);
      } catch (Exception var17) {
         return apiErrorJSON(L.t("Getting strategy stats failed", new Object[0]), var17);
      } finally {
         if (var3 != null) {
            var3.releaseLock("ResultsPlugins");
            Object var19 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private String getOrders(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         var3 = rgProvider.get(var1, "ResultsPlugins");
         String var4 = this.getParam(var1, "resultKey", "Portfolio");
         byte var5 = Byte.parseByte(this.getParam(var1, "direction", "0"));
         byte var6 = Byte.parseByte(this.getParam(var1, "sampleType", "127"));
         OrdersList var7 = var3.orders().filterExcludingControlOrders(var4, var5, var6, true);
         JSONArray var8 = new JSONArray();

         for (int var9 = 0; var9 < var7.size(); var9++) {
            Order var10 = var7.get(var9);
            JSONObject var11 = new JSONObject();
            var11.put("Ticket", var10.Ticket);
            var11.put("Symbol", var10.Symbol);
            var11.put("ResultName", var10.SetupName);
            var11.put("NumberOfTrade", var10.Order);
            var11.put("OpenTime", var10.OpenTime);
            var11.put("CloseTime", var10.CloseTime);
            var11.put("OriginalOpenTime", var10.OriginalOpenTime);
            var11.put("Type", var10.Type);
            var11.put("CloseType", var10.CloseType);
            var11.put("SampleType", var10.SampleType);
            var11.put("OpenPrice", var10.OpenPrice);
            var11.put("ClosePrice", var10.ClosePrice);
            var11.put("OriginalOpenPrice", var10.OriginalPrice);
            var11.put("SLLevel", var10.StopLoss);
            var11.put("PTLevel", var10.TakeProfit);
            var11.put("Size", var10.Size);
            var11.put("BarsInTrade", var10.BarsInTrade);
            var11.put("TimeInTrade", var10.Duration);
            var11.put("ProfitLoss", var10.PL);
            var11.put("ProfitLossPct", var10.PctPL);
            var11.put("ProfitLossPips", var10.PipsPL);
            var11.put("Drawdown", var10.DD);
            var11.put("PctDrawdown", var10.PctDD);
            var11.put("MAE", var10.MAE);
            var11.put("MAEPips", var10.PipsMAE);
            var11.put("MFE", var10.MFE);
            var11.put("MFEPips", var10.PipsMFE);
            var11.put("Comment", var10.Comment);
            var11.put("CommSwap", var10.CommSwap);
            var11.put("SlippageInMoney", var10.SlippageInMoney);
            var11.put("Balance", var10.AccountBalance);
            var8.put(var11);
         }

         var2.put("orders", var8);
      } catch (RecordNotFoundException var16) {
         Log.info("onPrint: " + var16.getMessage());
         return apiErrorJSONNoLog(null, var16);
      } catch (Exception var17) {
         return apiErrorJSON(L.t("Getting strategy orders failed", new Object[0]), var17);
      } finally {
         if (var3 != null) {
            var3.releaseLock("ResultsPlugins");
            Object var19 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private String getLastSettingsXml(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         var3 = rgProvider.get(var1, "ResultsPlugins");
         var2.put("lastSettingsXml", var3.getLastSettings());
      } catch (RecordNotFoundException var10) {
         Log.info("onPrint: " + var10.getMessage());
         return apiErrorJSONNoLog(null, var10);
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Getting strategy orders failed", new Object[0]), var11);
      } finally {
         if (var3 != null) {
            var3.releaseLock("ResultsPlugins");
            Object var13 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }
}
