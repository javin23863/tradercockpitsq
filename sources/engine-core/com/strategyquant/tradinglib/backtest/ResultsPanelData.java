package com.strategyquant.tradinglib.backtest;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.miniequitychart.MiniEquityChartComputer;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.project.bestresults.BestResultOverview;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.overview.OverviewTemplates;
import com.strategyquant.tradinglib.strategy.DataItemsGenerator;
import java.util.HashMap;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsPanelData {
   private static final Logger Log = LoggerFactory.getLogger(ResultsPanelData.class);

   public static void load(ResultsGroup var0, JSONObject var1, JSONObject var2) throws Exception {
      OverviewTemplate var3 = (OverviewTemplate)OverviewTemplates.getInstance().findClassByName("SQDefaultWithPortfolio");
      String var4 = var3.drawValues(var0, "Portfolio", new StatsTypeCombination((byte)0, (byte)10, (byte)127));
      BestResultOverview var5 = new BestResultOverview();
      var1.put("fullResult", "NA");
      var1.put("result", var5.print(var0, (byte)127));
      var2.put("product", "AlgoWizard");
      var2.put("projectName", "AlgoWizard");
      var2.put("databankName", "AlgoWizard");
      var2.put("strategyName", String.format("%s - %d", "AlgoWizard", System.currentTimeMillis()));
      var2.put("fileName", var0.getName());
      var2.put("taskType", "AlgoWizard");
      var2.put("taskName", "AlgoWizard");
      var2.put("isTaskManager", false);
      var2.put("isAlgoWizard", true);
      var2.put("resultInfo", "AlgoWizard");
      var2.put("isWFResult", false);
      var2.put("isPortfolio", var0.isPortfolio());
      DataItemsGenerator.get(var2, var0);
      JSONArray var6 = new JSONArray();

      for (String var8 : var0.getResultKeys()) {
         var6.put(var8);
      }

      var2.put("dataItems", var6);
      var1.put("resultsParams", var2);
      var1.put("equityChart", new JSONObject(getEquityData(var0)));
   }

   public static void loadAC(JSONObject var0, String var1, ResultsGroup var2) throws Exception {
      String var3 = null;

      try {
         var3 = MiniEquityChartComputer.getValue(var2, var2.getMainResultKey(), (byte)127);
      } catch (Throwable var12) {
         Log.error("Cannot get mini equity chart value (backtestID: " + var1 + ")", var12);
      }

      var0.put("miniEquityChart", var3);

      try {
         Result var4 = var2.mainResult();
         var0.put("symbol", var4.get(SpecialValues.Symbol));
         var0.put("timeframe", var4.get(SpecialValues.Timeframe));
         JSONObject var5 = new JSONObject();
         String[] var6 = new String[]{"NetProfit", "NumberOfTrades", "DrawdownPct", "SharpeRatio", "ProfitFactor", "AvgProfitPerDay"};

         for (int var7 = 0; var7 < var6.length; var7++) {
            String var8 = var6[var7];
            String var9 = "N/A";

            try {
               var9 = ((DatabankColumn)DatabankColumns.get().findClassByName(var8)).getValue(var2, var2.getMainResultKey(), (byte)0, (byte)10, (byte)127);
            } catch (Exception var11) {
               Log.error("Cannot get value of " + var8, var11);
            }

            var5.put(var8, var9);
         }

         var0.put("stats", var5);
      } catch (Throwable var13) {
         Log.error("Cannot get strategy stats (backtestID: " + var1 + ")", var13);
      }
   }

   public static void loadACExample(JSONObject var0, String var1, ResultsGroup var2) throws Exception {
      String var3 = var2.getLastSettings();
      var0.put("strategyXML", XMLUtil.elementToString(var2.getStrategyXml()));
      var0.put("lastSettingsXML", var3);
      OverviewTemplate var4 = (OverviewTemplate)OverviewTemplates.getInstance().findClassByName("DefaultOpenDD");
      var0.put("overviewHTML", var4.drawValues(var2, var2.getMainResultKey(), new StatsTypeCombination((byte)0, (byte)10, (byte)127)));
      Element var5 = StrategyXMLModifier.getUpdatedStrategyXML(var2, var2.getStrategyXml(), "fromStrategy", var2.getName(), var3, null);
      XMLIndicatorsAnalyzer.init();
      XMLIndicatorsAnalyzer.fixItemAttributes(var5);
      SourceCodeGenerator var6 = SourceCodeGenerators.getInstance().getGeneratorFromName("Pseudo Code(*.TXT)");
      var0.put("sourceCode", var6.getSource(var2.getName(), var5, 1.0, 1.0));
   }

   public static String getEquityData(ResultsGroup var0) {
      EquityChart var1 = new EquityChart();
      var1.reset();
      var1.setPLType((byte)10);
      var1.setDomainAxisType("trade");
      var1.zoom(true, 350, 0L, Long.MAX_VALUE);
      JsonCreator var2 = new JsonCreator();
      var2.beginObject();

      try {
         OrdersList var3 = var0.orders().filter("Portfolio", (byte)0, (byte)127, true);
         MainChartDataset var4 = EquityChartUtils.generateDataset(var3, "trade");
         var4.title = "Balance";
         var4.filled = true;
         var4.fillColor = "#DAF5FF";
         var4.fillNegativeColor = "#F49C9C";
         var4.thickness = 1;
         var1.addMainChartDataset(var4);

         for (IEquityChartAddon var6 : SQPluginManager.getPlugins(IEquityChartAddon.class)) {
            try {
               if (var6.getName() != null && var6.getName().equals("Drawdown")) {
                  HashMap var7 = new HashMap();
                  var7.put("drawdown", new String[]{"111"});
                  var6.print(var1, var0, var0.getMainResultKey(), var3, var7, "trade");
                  break;
               }
            } catch (Exception var8) {
               Log.error("Error while rendering EquityChart addon '" + var6.getName() + "'. Exc.", var8);
            }
         }

         var1.fillJSON(var2);
      } catch (Exception var9) {
         Log.error("Cannot create equity chart dataset", var9);
         var2.put("error", var9.getMessage(), false);
      }

      var2.endObject(false);
      return var2.toJson();
   }
}
