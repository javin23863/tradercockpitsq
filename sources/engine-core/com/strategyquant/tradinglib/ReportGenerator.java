package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.strategy.DataItemsGenerator;
import com.strategyquant.tradinglib.tradeanalysis.TradeAnalysisChartsList;
import com.strategyquant.tradinglib.tradelist.TradelistTableColumnEntry;
import com.strategyquant.tradinglib.tradelist.TradelistTableView;
import com.strategyquant.tradinglib.tradelist.TradelistViewsManager;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReportGenerator extends OverviewTemplate {
   protected File file;
   private ResultsGroup strategyResult;
   private JSONArray chartsResolved;
   private DataToSend wsData;
   private Thread waitingThread;
   private volatile boolean resolved = false;
   private String format;

   public ReportGenerator(String var1) {
      this.format = var1;
      this.wsData = new DataToSend("GenerateCharts", new JSONObject().put("format", var1));
   }

   @Override
   public String drawValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      this.strategyResult = var1;
      this.reset();
      this.addInfo(var1, var2);
      this.addErrorsTable(var1, var2);
      this.generateCharts(var1, var2);
      this.replaceValues(var1, var2, var3);
      return this.print();
   }

   private void addInfo(ResultsGroup var1, String var2) {
      String var3 = DataItemsGenerator.getResultInfo(var1);
      this.template = this.template.replace("${Strategy_Info}", var3);
   }

   private void generateCharts(ResultsGroup var1, String var2) throws Exception {
      this.chartsResolved = null;
      HashMap var3 = new HashMap();
      var3.put("projectName", new String[]{var1.getDatabank().getProject()});
      var3.put("resultKey", new String[]{"Portfolio"});
      var3.put("databankName", new String[]{var1.getDatabank().getName()});
      var3.put("strategyName", new String[]{var1.getName()});
      var3.put("sampleType", new String[]{"127"});
      var3.put("direction", new String[]{"0"});
      var3.put("dailychart", new String[]{"false"});
      var3.put("drawdown", new String[]{"10"});
      var3.put("equity", new String[]{"off"});
      var3.put("height", new String[]{"619"});
      var3.put("history", new String[]{"false"});
      var3.put("points", new String[]{"off"});
      var3.put("stagnation", new String[]{"0"});
      var3.put("trendline", new String[]{"false"});
      var3.put("volatility", new String[]{"false"});
      var3.put("volume", new String[]{"5"});
      var3.put("width", new String[]{"1153"});
      var3.put("xaxis", new String[]{"time"});
      var3.put("yearmarker", new String[]{"true"});
      var3.put("zoom", new String[]{"false"});
      var3.put("zoommaxX", new String[]{"-1"});
      var3.put("zoomminX", new String[]{"-1"});
      var3.put("zoomwidth", new String[]{"-1"});
      var3.put("benchmarkActive", new String[]{"false"});
      var3.put("benchmarkSymbol", new String[]{"SPY.D"});
      var3.put("benchmarkNormalization", new String[]{"off"});
      JSONObject var4 = new JSONObject((String)Program.get("ResultsEquityChart").call("printResultsGroup", new Object[]{var1, var3})).getJSONObject("chart");
      this.wsData.getDataObject().put("equityChart", var4);
      var3.clear();
      var3.put("project", new String[]{var1.getDatabank().getProject()});
      var3.put("projectName", new String[]{var1.getDatabank().getProject()});
      var3.put("resultKey", new String[]{"Portfolio"});
      var3.put("databank", new String[]{var1.getDatabank().getName()});
      var3.put("databankName", new String[]{var1.getDatabank().getName()});
      var3.put("strategy", new String[]{var1.getName()});
      var3.put("strategyName", new String[]{var1.getName()});
      var3.put("sampleType", new String[]{"127"});
      var3.put("direction", new String[]{"0"});
      String var5 = "PLbyHourChart,PLbyWeekdayChart,TradesByWeekdayChart,LongShortProfitLossChart,WinLossCountByWeekdayChart,WinLossPLByMonthChart,LongShortTradesChart,PLGrowthByDurationChart,TradesByHourChart,PLLongChart,PLShortChart,TradesByDurationChart";
      var3.put("charts", new String[]{var5});
      var3.put("period", new String[]{"1"});
      var3.put("year", new String[]{"All"});
      JSONArray var6 = new JSONObject((String)Program.get("ResultsTradeAnalysis").call("printResultsGroup", new Object[]{var1, var3})).getJSONArray("charts");
      this.wsData.getDataObject().put("charts", var6);
      this.wsData.getDataObject().put("types", new JSONArray(Arrays.asList(var5.split(","))));
      SQWebSocketManager.addToDataQueue(this.wsData, "RESULTS");
      this.resolved = false;
      this.waitingThread = new Thread() {
         @Override
         public void run() {
            while (!ReportGenerator.this.resolved) {
               try {
                  Thread.sleep(50L);
               } catch (Exception var2x) {
               }
            }
         }
      };
      this.waitingThread.start();

      try {
         this.waitingThread.join(10000L);
      } catch (InterruptedException var20) {
         Log.error("Waiting for confirmation interrupted.", var20);
      }

      this.waitingThread = null;
      if (this.chartsResolved != null) {
         int var7 = 1;

         for (int var10 = 0; var10 < this.chartsResolved.length(); var10++) {
            JSONObject var11 = this.chartsResolved.getJSONObject(var10);
            String var12 = var11.getString("key");
            String var13 = var11.getString("img");
            var13 = var13.replaceAll("data:image/png;base64,", "");
            var13 = var13.replaceAll(" ", "+");
            String var8;
            if (var12.contains("equityChart")) {
               String var9 = this.escapeChartName(var1.getName() + "_equityChart") + ".png";
               this.replace("Equity_Chart", "Equity chart", var9);
               var8 = this.file.getParentFile().getAbsolutePath() + "/" + var9;
            } else {
               String var21 = this.escapeChartName(var1.getName() + "_" + var12) + ".png";
               TradeAnalysisChart var14 = (TradeAnalysisChart)TradeAnalysisChartsList.getInstance().findClassByName(var12);
               this.replace("Chart_" + var7, var14.name, var21);
               var8 = this.file.getParentFile().getAbsolutePath() + "/" + var21;
               var7++;
            }

            byte[] var24 = Base64.decodeBase64(var13);
            FileOutputStream var15 = new FileOutputStream(var8);

            try {
               var15.write(var24);
               var15.close();
            } catch (Throwable var19) {
               try {
                  var15.close();
               } catch (Throwable var18) {
                  var19.addSuppressed(var18);
               }

               throw var19;
            }

            var15.close();
         }
      }
   }

   private String escapeChartName(String var1) {
      return var1.replaceAll("[^a-zA-Z0-9_-]+", "_");
   }

   protected void resolveCharts(JSONObject var1) throws Exception {
      try {
         this.chartsResolved = var1.getJSONArray("charts");
      } finally {
         this.resolved = true;
      }
   }

   protected void addErrorsTable(ResultsGroup var1, String var2) throws Exception {
      if (var1 != null) {
         int var3 = var1.specialValues().getInt("StrategyProblems", 0);
         if (var3 != 0) {
            String var4 = "table.problems { width: 100%; background-color: #e0e2e3; color: #C00000; border: 1px solid #C00000;}\ntable.problems td { padding: 5px 5px 5px 30px; text-align: left; background-color: white; height: 15px; font-size: 11px;}\ntable.problems th { padding: 5px 5px 5px 10px; text-align: left; background-color: white; color: #606060; font-weight: normal; font-size: 11px;}\ntable.problems td.name {white-space: nowrap;}\n.problems h1 {color: #C00000; border-bottom: 0px;}";
            this.template = this.template.replace("</style>", var4 + "\n</style>");
            StringBuilder var5 = new StringBuilder("<div class=\"problems\">");
            var5.append("<h1>Strategy Problems</h1>");
            var5.append("<table class=\"problems\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
            var5.append(
               "<tr><th>SQ identified some problems when testing the strategy - strategy is most probably not suitable for trading on real account !<br/>These problems affect either strategy logic or accuracy of backtesting.</th></tr>"
            );
            var5.append(this.printProblems(var3, var1));
            var5.append("</table>");
            var5.append("</div>");
            var5.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");
            this.template = this.template.replace("<div id=\"summaryBox\">", var5.toString());
         }
      }
   }

   private String printProblems(int var1, ResultsGroup var2) {
      StringBuilder var3 = new StringBuilder("");
      if (var2.isStockpickerStrategy()) {
         this.printProblem(2048, var1, var3);
      } else {
         this.printProblem(1, var1, var3);
         this.printProblem(2, var1, var3);
         this.printProblem(4, var1, var3);
         this.printProblem(8, var1, var3);
         this.printProblem(16, var1, var3);
         this.printProblem(32, var1, var3);
         this.printProblem(64, var1, var3);
         this.printProblem(256, var1, var3);
      }

      return var3.toString();
   }

   private void printProblem(int var1, int var2, StringBuilder var3) {
      if ((var2 & var1) != 0) {
         var3.append("<tr><td>");
         var3.append(BadStrategyException.getExplanation(var1));
         var3.append("</td></tr>");
      }
   }

   public void replaceValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      Result var4 = null;
      SQStats var5 = null;
      if (var1 != null) {
         var4 = var1.subResult(var2);
         if (var4 != null) {
            try {
               var5 = var4.stats(var3);
            } catch (StatsDontExistException var8) {
               if (!var8.getMessage().contains("Result doesn't contain stats")) {
                  Log.debug(var8.getMessage());
               }
            }
         }
      }

      String var6 = var3.getPLType() == 30 ? " small" : "";
      this.replace(
         "Big",
         L.t("TOTAL PROFIT", new Object[0]),
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("NetProfit"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("NetProfit") > 0.0 ? "positiveNum" + var6 : "negativeNum" + var6)
      );
      if (var3.getPLType() == 10) {
         this.replace(
            "Small1",
            L.t("PROFIT IN PIPS", new Object[0]),
            var5 == null ? NA : this.d2WithPlType(var5.getDouble("NetProfitInPips"), (byte)30),
            var5 == null ? null : (var5.getDouble("NetProfitInPips") > 0.0 ? "positiveNum" : "negativeNum")
         );
      } else {
         SQStats var7 = null;
         if (var4 != null) {
            var7 = var4.stats(var3.getDirection(), (byte)10, var3.getSampleType());
         }

         this.replace(
            "Small1",
            L.t("PROFIT IN MONEY", new Object[0]),
            var7 == null ? NA : this.d2WithPlType(var7.getDouble("NetProfit"), (byte)10),
            var7 == null ? null : (var7.getDouble("NetProfit") > 0.0 ? "positiveNum" : "negativeNum")
         );
      }

      this.replace(
         "Small2",
         L.t("YEARLY AVG PROFIT", new Object[0]),
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerYear"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgProfitPerYear") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replace(
         "Small3",
         L.t("YEARLY AVG % RETURN", new Object[0]),
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgPctProfitPerYear"), (byte)20),
         var5 == null ? null : (var5.getDouble("AvgPctProfitPerYear") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replace(
         "Small4",
         L.t("CAGR", new Object[0]),
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("CAGR"), (byte)20),
         var5 == null ? null : (var5.getDouble("CAGR") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replace("1_1", L.t("# OF TRADES", new Object[0]), var5 == null ? NA : "" + var5.getInt("NumberOfTrades"));
      this.replace("1_2", L.t("SHARPE RATIO", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("SharpeRatio")));
      this.replace("1_3", L.t("PROFIT FACTOR", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ProfitFactor")));
      this.replace("1_4", L.t("RETURN / DD RATIO", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ReturnDDRatio")));
      this.replace("1_5", L.t("WINNING PERCENTAGE", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("WinningPct")) + " %");
      String var9 = var3.getPLType() == 20 ? "DrawdownPct" : (var3.getPLType() == 30 ? "DrawdownPips" : "Drawdown");
      this.replace("2_1", L.t("DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble(var9), var3.getPLType()));
      this.replace("2_2", L.t("% DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("DrawdownPct"), (byte)20));
      this.replace("2_3", L.t("DAILY AVG PROFIT", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerDay"), var3.getPLType()));
      this.replace(
         "2_4", L.t("MONTHLY AVG PROFIT", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerMonth"), var3.getPLType())
      );
      this.replace("2_5", L.t("AVERAGE TRADE", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgTrade"), var3.getPLType()));
      this.replace("3_1", L.t("ANNUAL % / Max DD %", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AnnualPctReturnDDRatio")));
      this.replace("3_2", L.t("R EXPECTANCY", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("RExpectancy")));
      this.replace("3_3", L.t("R EXPECTANCY SCORE", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("RExpectancyScore")));
      this.replace("3_4", L.t("STR QUALITY NUMBER", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("SQN")));
      this.replace("3_5", L.t("SQN SCORE", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("SQNScore")));
      this.replace("S1_1", L.t("Wins / Losses Ratio", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("WinLossRatio")));
      this.replace("S1_2", L.t("Payout Ratio (Avg Win/Loss)", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("PayoutRatio")));
      this.replace("S1_3", L.t("Average # of Bars in Trade", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgBarsInTrade")));
      this.replace("S2_1", L.t("AHPR", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AHPR")));
      this.replace("S2_2", L.t("Z-Score", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ZScore")));
      this.replace("S2_3", L.t("Z-Probability", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ZProbability")) + " %");
      this.replace("S3_1", L.t("Expectancy", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("Expectancy")));
      this.replace("S3_2", L.t("Deviation", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("StandardDev"), var3.getPLType()));
      this.replace("S3_3", L.t("Exposure", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("Exposure", 0.0)) + " %");
      this.replace("S4_1", L.t("Stagnation in Days", new Object[0]), var5 == null ? NA : "" + var5.getInt("Stagnation"));
      this.replace("S4_2", L.t("Stagnation in %", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("StagnationPct"), (byte)20));
      this.replace("S4_3", "", "");
      this.replace("T1_1", "", "");
      this.replace("T1_2", L.t("# of Wins", new Object[0]), var5 == null ? NA : "" + var5.getInt("NumberOfProfits"));
      this.replace("T1_3", L.t("# of Losses", new Object[0]), var5 == null ? NA : "" + var5.getInt("NumberOfLosses"));
      this.replace("T1_4", L.t("# of Cancelled/Expired", new Object[0]), var5 == null ? NA : "" + var5.getInt("NumberOfCanceled"));
      this.replace("T2_1", L.t("Gross Profit", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossProfit"), var3.getPLType()));
      this.replace("T2_2", L.t("Gross Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossLoss"), var3.getPLType()));
      this.replace("T2_3", L.t("Average Win", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgWin"), var3.getPLType()));
      this.replace("T2_4", L.t("Average Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgLoss"), var3.getPLType()));
      this.replace("T3_1", L.t("Largest Win", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxProfit"), var3.getPLType()));
      this.replace("T3_2", L.t("Largest Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxLoss"), var3.getPLType()));
      this.replace("T3_3", L.t("Max Consec Wins", new Object[0]), var5 == null ? NA : "" + var5.getInt("MaxConsecWins"));
      this.replace("T3_4", L.t("Max Consec Losses", new Object[0]), var5 == null ? NA : "" + var5.getInt("MaxConsecLosses"));
      this.replace("T4_1", L.t("Avg Consec Wins", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgConsecWins")));
      this.replace("T4_2", L.t("Avg Consec Loss", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgConsecLosses")));
      this.replace("T4_3", L.t("Avg # of Bars in Wins", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgBarsWin")));
      this.replace("T4_4", L.t("Avg # of Bars in Losses", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgBarsLoss")));
      this.addMonthlyPerformanceTable(var1, var2, var3);
      this.addTrades(var1, var2, var3);
   }

   private void addTrades(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      StringBuilder var4 = new StringBuilder("");
      OrdersList var5 = var1.orders();
      TradelistTableView var6 = TradelistViewsManager.getInstance().getSelectedTradelistView();
      ArrayList var7 = var6.getData(var5, 0, var5.size(), true);
      int var8 = var6.columns.size();
      var4.append("<tr class=\"tr" + L.t("Trades", new Object[0]) + "\">");

      for (int var9 = 0; var9 < var8; var9++) {
         var4.append("<td>" + var6.columns.get(var9).getColumnEntryName() + "</td>");
      }

      var4.append("</tr>");

      for (int var16 = 0; var16 < var7.size(); var16++) {
         if (this.format.equals("pdf") && var16 == 1000) {
            var4.append("<tr>");
            var4.append(
               "<td colspan=\"" + var6.columns.size() + "\">The trade list was shortened to 1000 rows (total " + var7.size() + ") to reduce file size.</td>"
            );
            var4.append("</tr>");
            break;
         }

         var4.append("<tr>");
         Object[] var10 = (Object[])var7.get(var16);

         for (int var11 = 0; var11 < var6.columns.size(); var11++) {
            Object var12 = var10[var11];
            TradelistTableColumnEntry var13 = var6.columns.get(var11);

            try {
               String var14 = this.formatValue(var13, String.valueOf(var12), false);
               if (var16 % 2 == 1) {
                  var4.append("<td bgcolor=\"#E0E0E0\">" + var14 + "</td>");
               } else {
                  var4.append("<td >" + var14 + "</td>");
               }
            } catch (Exception var15) {
               var4.append("<td>N/A</td>");
            }
         }

         var4.append("</tr>");
      }

      this.template = this.template.replace("<!-- trades -->", var4.toString());
   }

   private String formatValue(TradelistTableColumnEntry var1, String var2, boolean var3) {
      var2 = SQUtils.removeHtmlTags(var2);
      if (var3) {
         switch (var1.tableColumn.getType()) {
            case "Integer":
            case "Text":
            case "Time":
               return var2;
            default:
               return var2.replace('.', ',');
         }
      } else {
         return var2;
      }
   }

   private void addMonthlyPerformanceTable(ResultsGroup var1, String var2, StatsTypeCombination var3) {
      String var4 = this.printTableBody(var1, var2, var3);
      String var5 = "<div class=\"performance\"><h1>"
         + L.t("Monthly Performance", new Object[0])
         + " ("
         + PlTypes.print(var3.getPLType())
         + ")</h1><table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr class=\"months\"><td>"
         + L.t("Year", new Object[0])
         + "</td><td>"
         + L.t("Jan", new Object[0])
         + "</td><td>"
         + L.t("Feb", new Object[0])
         + "</td><td>"
         + L.t("Mar", new Object[0])
         + "</td><td>"
         + L.t("Apr", new Object[0])
         + "</td><td>"
         + L.t("May", new Object[0])
         + "</td><td>"
         + L.t("Jun", new Object[0])
         + "</td><td>"
         + L.t("Jul", new Object[0])
         + "</td><td>"
         + L.t("Aug", new Object[0])
         + "</td><td>"
         + L.t("Sep", new Object[0])
         + "</td><td>"
         + L.t("Oct", new Object[0])
         + "</td><td>"
         + L.t("Nov", new Object[0])
         + "</td><td>"
         + L.t("Dec", new Object[0])
         + "</td><td>"
         + L.t("YTD", new Object[0])
         + "</td></tr>"
         + var4
         + "</table></div>";
      this.template = this.template.replace("<!-- end of data -->", var5 + "\n<!-- end of data -->");
   }

   private String printTableBody(ResultsGroup var1, String var2, StatsTypeCombination var3) {
      StringBuilder var4 = new StringBuilder("");
      if (var1 != null) {
         try {
            TreeMap var5 = null;
            OrdersList var6 = var1.orders().filter(var2, var3.getDirection(), var3.getSampleType());
            var5 = computeMonthlyPerformance(var6, var3.getPLType());
            int var8 = 0;

            for (Entry var10 : var5.descendingMap().entrySet()) {
               String var11 = ((Integer)var10.getKey()).toString();
               Double[] var12 = (Double[])var10.getValue();
               String var7;
               if (var8 % 2 == 0) {
                  var7 = "oddrow";
               } else {
                  var7 = "evenrow";
               }

               var4.append("<tr class=\"" + var7 + "\">");
               var4.append("<td class=\"bold\">" + var11 + "</td>");

               for (int var13 = 0; var13 < var12.length; var13++) {
                  if (var12[var13] < 0.0) {
                     var4.append("<td class=\"negativeNum\">" + this.d2(var12[var13]) + "</td>");
                  } else {
                     var4.append("<td>" + this.d2(var12[var13]) + "</td>");
                  }
               }

               var4.append("</tr>");
               var8++;
            }

            return var4.toString();
         } catch (Exception var14) {
            Log.error("Cannot get list of orders. ", var14);
         }
      }

      return "<tr class=\"oddrow\"><td class=\"bold\">NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td></tr>";
   }

   public static TreeMap<Integer, Double[]> computeMonthlyPerformance(OrdersList var0, byte var1) {
      TreeMap var2 = new TreeMap();
      int var3 = -1;
      int var4 = -1;
      int var5 = -1;
      int var6 = -1;
      Object var7 = null;

      for (int var8 = 0; var8 < var0.size(); var8++) {
         Order var9 = var0.get(var8);
         var5 = SQTime.getYear(var9.CloseTime) + 1900;
         if (var3 == -1) {
            var3 = var5;
         }

         if (var4 == -1) {
            var4 = var5;
         }

         if (var3 > var5) {
            var3 = var5;
         }

         if (var4 < var5) {
            var4 = var5;
         }

         if (var2.containsKey(var5)) {
            var7 = (Double[])var2.get(var5);
         } else {
            var7 = new Double[13];

            for (int var10 = 0; var10 < 13; var10++) {
               ((Object[])var7)[var10] = 0.0;
            }

            var2.put(var5, var7);
         }

         var6 = SQTime.getMonth(var9.CloseTime);
         Double[] var22 = (Double[])var7;
         int var11 = var6;
         var22[var11] = var22[var11] + var9.getPLByType(var1);
      }

      for (Entry var21 : var2.entrySet()) {
         var7 = (Double[])var21.getValue();
         double var23 = 0.0;

         for (int var12 = 0; var12 < 12; var12++) {
            var23 += ((Object[])var7)[var12];
         }

         ((Object[])var7)[12] = var23;
      }

      if (var3 > 0 && var4 > 0) {
         for (int var14 = var3; var14 < var4; var14++) {
            if (!var2.containsKey(var14)) {
               var7 = new Double[13];

               for (int var20 = 0; var20 < 13; var20++) {
                  ((Object[])var7)[var20] = 0.0;
               }

               var2.put(var14, var7);
            }
         }
      }

      return var2;
   }
}
