package SQ.Stats.Overview;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsDontExistException;
import com.strategyquant.tradinglib.StatsTypeCombination;
import java.util.TreeMap;
import java.util.Map.Entry;

public class AWSharedDefault extends OverviewTemplate {
   public AWSharedDefault() {
      this.setName(L.tsq("AW Shared Default"));
      this.setScreenshotName("sqdefault_screenshot.jpg");
      this.setHtmlTemplateName("awshareddefault_template.htm");

      try {
         this.loadTemplate();
      } catch (Exception var2) {
         Log.error("Overview template couldn't be loaded. Exc. ", var2);
      }
   }

   public String drawValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      this.reset();
      this.addErrorsTable(var1, var2);
      this.replaceValues(var1, var2, var3);
      return this.print();
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
            var5.append(this.printProblems(var3));
            var5.append("</table>");
            var5.append("</div>");
            var5.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");
            this.template = this.template.replace("<div id=\"summaryBox\">", var5.toString());
         }
      }
   }

   private String printProblems(int var1) {
      StringBuilder var2 = new StringBuilder("");
      this.printProblem(1, var1, var2);
      this.printProblem(2, var1, var2);
      this.printProblem(4, var1, var2);
      this.printProblem(8, var1, var2);
      this.printProblem(16, var1, var2);
      this.printProblem(32, var1, var2);
      this.printProblem(64, var1, var2);
      this.printProblem(256, var1, var2);
      return var2.toString();
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
      this.replace("1_1", L.t("# OF TRADES", new Object[0]), var5 == null ? NA : var5.getInt("NumberOfTrades") + "");
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
      this.replace("S4_1", L.t("Stagnation in Days", new Object[0]), var5 == null ? NA : var5.getInt("Stagnation") + "");
      this.replace("S4_2", L.t("Stagnation in %", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("StagnationPct"), (byte)20));
      this.replace("S4_3", "", "");
      this.replace("T1_1", "", "");
      this.replace("T1_2", L.t("# of Wins", new Object[0]), var5 == null ? NA : var5.getInt("NumberOfProfits") + "");
      this.replace("T1_3", L.t("# of Losses", new Object[0]), var5 == null ? NA : var5.getInt("NumberOfLosses") + "");
      this.replace("T1_4", L.t("# of Cancelled/Expired", new Object[0]), var5 == null ? NA : var5.getInt("NumberOfCanceled") + "");
      this.replace("T2_1", L.t("Gross Profit", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossProfit"), var3.getPLType()));
      this.replace("T2_2", L.t("Gross Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossLoss"), var3.getPLType()));
      this.replace("T2_3", L.t("Average Win", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgWin"), var3.getPLType()));
      this.replace("T2_4", L.t("Average Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgLoss"), var3.getPLType()));
      this.replace("T3_1", L.t("Largest Win", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxProfit"), var3.getPLType()));
      this.replace("T3_2", L.t("Largest Loss", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxLoss"), var3.getPLType()));
      this.replace("T3_3", L.t("Max Consec Wins", new Object[0]), var5 == null ? NA : var5.getInt("MaxConsecWins") + "");
      this.replace("T3_4", L.t("Max Consec Losses", new Object[0]), var5 == null ? NA : var5.getInt("MaxConsecLosses") + "");
      this.replace("T4_1", L.t("Avg Consec Wins", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgConsecWins")));
      this.replace("T4_2", L.t("Avg Consec Loss", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgConsecLosses")));
      this.replace("T4_3", L.t("Avg # of Bars in Wins", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgBarsWin")));
      this.replace("T4_4", L.t("Avg # of Bars in Losses", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("AvgBarsLoss")));
      this.addMonthlyPerformanceTable(var1, var2, var3);
   }

   private void addMonthlyPerformanceTable(ResultsGroup var1, String var2, StatsTypeCombination var3) {
      String var4 = this.printTableBody(var1, var2, var3);
      String var5 = "<div class=\"performance\"><h1>Monthly Performance ("
         + PlTypes.print(var3.getPLType())
         + ")</h1><table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr class=\"months\"><td>Year</td><td>Jan</td><td>Feb</td><td>Mar</td><td>Apr</td><td>May</td><td>Jun</td><td>Jul</td><td>Aug</td><td>Sep</td><td>Oct</td><td>Nov</td><td>Dec</td><td>YTD</td></tr>"
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
