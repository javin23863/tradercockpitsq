package SQ.Stats.Overview;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.Benchmark;
import com.strategyquant.tradinglib.BenchmarkResult;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsDontExistException;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jdom2.Element;

public class SQDefaultPct extends OverviewTemplate {
   private static final double NotTraded = -1111.0;
   private static int IndexYTD = 12;
   private static int IndexBenchmark = 13;
   private static int IndexComparison = 14;
   private static int IndexMaxDD = 15;
   private static int IndexBenchmarkMaxDD = 16;

   public SQDefaultPct() {
      this.setName(L.tsq("SQ Default (% Monthly Performance)"));
      this.setScreenshotName("sqdefault_screenshot.jpg");
      this.setHtmlTemplateName("sqdefault_template.htm");

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
            var5.append("<h1>" + L.t("Strategy Problems", new Object[0]) + "</h1>");
            var5.append("<table class=\"problems\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
            var5.append(
               "<tr><th>"
                  + L.t(
                     "SQ identified some problems when testing the strategy - strategy is most probably not suitable for trading on real account !",
                     new Object[0]
                  )
                  + "<br/>"
                  + L.t("These problems affect either strategy logic or accuracy of backtesting.", new Object[0])
                  + "</th></tr>"
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
      this.printProblem(1, var1, var3);
      this.printProblem(2, var1, var3);
      this.printProblem(4, var1, var3);
      this.printProblem(8, var1, var3);
      this.printProblem(16, var1, var3);
      this.printProblem(32, var1, var3);
      this.printProblem(64, var1, var3);
      this.printProblem(256, var1, var3);
      this.printProblem(2048, var1, var3);
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
               if (!var8.getMessage().contains(L.t("Result doesn't contain stats", new Object[0]))) {
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
      this.replace("1_5", L.t("WINNING PERCENTAGE", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("WinningPct")) + " %");
      if (var1.isStockpickerStrategy()) {
         this.replace("1_4", L.t("RETURN / OPEN DD RATIO", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ReturnOpenDDRatio")));
         this.replace("2_1", L.t("OPEN DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("OpenDrawdown"), var3.getPLType()));
         this.replace("2_2", L.t("OPEN % DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("OpenDrawdownPct"), (byte)20));
      } else {
         this.replace("1_4", L.t("RETURN / DD RATIO", new Object[0]), var5 == null ? NA : this.d2(var5.getDouble("ReturnDDRatio")));
         String var9 = var3.getPLType() == 20 ? "DrawdownPct" : (var3.getPLType() == 30 ? "DrawdownPips" : "Drawdown");
         this.replace("2_1", L.t("DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble(var9), var3.getPLType()));
         this.replace("2_2", L.t("% DRAWDOWN", new Object[0]), var5 == null ? NA : this.d2WithPlType(var5.getDouble("DrawdownPct"), (byte)20));
      }

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
      boolean var6 = false;

      try {
         var6 = var1.isStockpickerStrategy() && var1.mainResult().getMaxDailyDD() != null;
      } catch (Exception var8) {
      }

      String var4;
      if (var6) {
         String var5 = this.printStockpickerTableBody(var1, var2, var3);
         var4 = "<div class=\"performance\"><h1>"
            + L.t("Monthly Performance % with Daily Equity", new Object[0])
            + "</h1><table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr class=\"months\"><td>"
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
            + "</td><td>"
            + L.t("Benchmark", new Object[0])
            + "</td><td>"
            + L.t("Comparison", new Object[0])
            + "</td><td>"
            + L.t("Max DD", new Object[0])
            + "</td><td>"
            + L.t("Benchmark Max DD", new Object[0])
            + "</td></tr>"
            + var5
            + "</table></div>";
      } else {
         String var9 = this.printTableBody(var1, var2, var3);
         var4 = "<div class=\"performance\"><h1>"
            + L.t("Monthly Performance", new Object[0])
            + " (%)</h1><table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr class=\"months\"><td>"
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
            + var9
            + "</table></div>";
      }

      this.template = this.template.replace("<!-- end of data -->", var4 + "\n<!-- end of data -->");
   }

   private String printTableBody(ResultsGroup var1, String var2, StatsTypeCombination var3) {
      StringBuilder var4 = new StringBuilder("");
      if (var1 != null) {
         double var5 = 10000.0;

         try {
            Result var7 = var1.subResult("Portfolio");
            var5 = var7.getSettings().getDouble("MoneyManagement.InitialCapital", 10000.0);
         } catch (Exception var25) {
            Log.error("Failed to get MoneyManagement.InitialCapital value.", var25);
         }

         try {
            TreeMap var27 = null;
            OrdersList var8 = var1.orders().filter(var2, var3.getDirection(), var3.getSampleType());
            var8.sort(ComparatorByCloseTime);
            var27 = computeMonthlyPerformance(var5, var8, var3.getPLType());
            int var10 = 0;
            double var15 = var5;
            double var17 = var5;

            for (Entry var20 : var27.entrySet()) {
               String var21 = ((Integer)var20.getKey()).toString();
               Double[] var22 = (Double[])var20.getValue();
               String var9;
               if (var10 % 2 == 0) {
                  var9 = "oddrow";
               } else {
                  var9 = "evenrow";
               }

               StringBuilder var23 = new StringBuilder("");
               var23.append("<tr class=\"" + var9 + "\">");
               var23.append("<td class=\"bold\">" + var21 + "</td>");

               for (int var24 = 0; var24 < var22.length; var24++) {
                  double var11 = var22[var24];
                  if (var11 == -1111.0) {
                     var23.append("<td>0</td>");
                  } else {
                     double var13;
                     if (var24 < 12) {
                        var13 = var15 == 0.0 ? 0.0 : (var11 - var15) / Math.abs(var15) * 100.0;
                     } else {
                        var13 = var17 == 0.0 ? 0.0 : (var11 - var17) / Math.abs(var17) * 100.0;
                     }

                     if (var11 < 0.0) {
                        var23.append("<td class=\"negativeNum\">" + this.d2(var13) + "</td>");
                     } else {
                        var23.append("<td>" + this.d2(var13) + "</td>");
                     }

                     if (var24 < 12) {
                        var15 = var11;
                     } else {
                        var17 = var11;
                     }
                  }
               }

               var23.append("</tr>");
               var4.insert(0, var23);
               var10++;
            }

            return var4.toString();
         } catch (Exception var26) {
            Log.error("Cannot get list of orders. ", var26);
         }
      }

      return "<tr class=\"oddrow\"><td class=\"bold\">NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td></tr>";
   }

   public static TreeMap<Integer, Double[]> computeMonthlyPerformance(double var0, OrdersList var2, byte var3) {
      TreeMap var4 = new TreeMap();
      int var5 = -1;
      int var6 = -1;
      int var7 = -1;
      int var8 = -1;
      Object var9 = null;
      double var10 = var0;

      for (int var12 = 0; var12 < var2.size(); var12++) {
         Order var13 = var2.get(var12);
         var10 += var13.getPLByType(var3);
         var7 = SQTime.getYear(var13.CloseTime) + 1900;
         if (var5 == -1) {
            var5 = var7;
         }

         if (var6 == -1) {
            var6 = var7;
         }

         if (var5 > var7) {
            var5 = var7;
         }

         if (var6 < var7) {
            var6 = var7;
         }

         if (var4.containsKey(var7)) {
            var9 = (Double[])var4.get(var7);
         } else {
            var9 = new Double[13];

            for (int var14 = 0; var14 < 13; var14++) {
               ((Object[])var9)[var14] = -1111.0;
            }

            var4.put(var7, var9);
         }

         var8 = SQTime.getMonth(var13.CloseTime);
         ((Object[])var9)[var8] = var10;
         ((Object[])var9)[12] = var10;
      }

      if (var5 > 0 && var6 > 0) {
         for (int var16 = var5; var16 < var6; var16++) {
            if (!var4.containsKey(var16)) {
               var9 = new Double[13];

               for (int var20 = 0; var20 < 13; var20++) {
                  ((Object[])var9)[var20] = -1111.0;
               }

               var4.put(var16, var9);
            }
         }
      }

      return var4;
   }

   private String printStockpickerTableBody(ResultsGroup var1, String var2, StatsTypeCombination var3) {
      StringBuilder var4 = new StringBuilder("");
      if (var1 != null) {
         try {
            TreeMap var5 = null;
            var5 = computeStockpickerMonthlyPerformance(var1, var2, var3);
            int var7 = 0;

            for (Entry var9 : var5.descendingMap().entrySet()) {
               String var10 = ((Integer)var9.getKey()).toString();
               Double[] var11 = (Double[])var9.getValue();
               String var6;
               if (var7 % 2 == 0) {
                  var6 = "oddrow";
               } else {
                  var6 = "evenrow";
               }

               var4.append("<tr class=\"" + var6 + "\">");
               var4.append("<td class=\"bold\">" + var10 + "</td>");

               for (int var12 = 0; var12 < var11.length; var12++) {
                  if (var12 == IndexYTD) {
                     if (var11[var12] < 0.0) {
                        var4.append("<td class=\"negativeNum\"><b>" + this.d2(var11[var12]) + "</b></td>");
                     } else {
                        var4.append("<td><b>" + this.d2(var11[var12]) + "</b></td>");
                     }
                  } else if (var12 != IndexMaxDD && var12 != IndexBenchmarkMaxDD) {
                     if (var11[var12] < 0.0) {
                        var4.append("<td class=\"negativeNum\">" + this.d2(var11[var12]) + "</td>");
                     } else {
                        var4.append("<td>" + this.d2(var11[var12]) + "</td>");
                     }
                  } else {
                     var4.append("<td>" + this.d2(var11[var12]) + "</td>");
                  }
               }

               var4.append("</tr>");
               var7++;
            }

            return var4.toString();
         } catch (Exception var13) {
            Log.error("Cannot get list of orders. ", var13);
         }
      }

      return "<tr class=\"oddrow\"><td class=\"bold\">NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td><td>NA</td></tr>";
   }

   public static TreeMap<Integer, Double[]> computeStockpickerMonthlyPerformance(ResultsGroup var0, String var1, StatsTypeCombination var2) throws Exception {
      Long2FloatRBTreeMap var3 = var0.mainResult().getWorstDailyEquity();
      Long2FloatRBTreeMap var4 = var0.mainResult().getMaxDailyDDPct();
      Element var5 = XMLUtil.stringToElement(var0.getLastSettings());
      Element var6 = var5.getChild("RiskMoneyManagement");
      MoneyManagementMethod var7 = ProjectConfigHelper.getMoneyManagement(var6.getChild("MoneyManagement"));
      double var8 = var7.getInitialCapital();
      OrdersList var10 = var0.orders().filter(var1, var2.getDirection(), var2.getSampleType());
      long[] var11 = EquityChartUtils.dateRange(var10, var0.isStockpickerStrategy());
      long var12 = var11[0];
      long var14 = var11[1];
      Benchmark var16 = new Benchmark();
      BenchmarkResult var17 = var16.calculate(var0, var12, var14);
      TreeMap var18 = new TreeMap();
      int var19 = -1;
      int var20 = -1;
      int var21 = -1;
      Double[] var22 = null;
      double var37 = var8;
      double var39 = var8;
      double var41 = var8;
      double var43 = var8;
      double var45 = var8;
      double var47 = var8;
      double var49 = var8;
      ObjectBidirectionalIterator var53 = var3.long2FloatEntrySet().iterator();

      while (var53.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry var54 = (it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry)var53.next();
         long var31 = var54.getLongKey();
         double var33 = var54.getFloatValue() + var8;
         double var35 = var4.containsKey(var31) ? var4.get(var31) : -1.0;
         if (var17.equity.containsKey(var31)) {
            var45 = var17.equity.get(var31) + var8;
         }

         double var51 = var17.ddMapPct.containsKey(var31) ? var17.ddMapPct.get(var31) : -1.0;
         var19 = SQTime.getYear(var31) + 1900;
         if (var18.containsKey(var19)) {
            var22 = (Double[])var18.get(var19);
         } else {
            var22 = new Double[17];

            for (int var55 = 0; var55 < 17; var55++) {
               var22[var55] = 0.0;
            }

            var18.put(var19, var22);
         }

         int var62 = SQTime.getMonth(var31);
         if (var62 != var21) {
            if (var21 != -1) {
               double var23 = (var39 - var37) / var37 * 100.0;
               ((Double[])var18.get(var20))[var21] = var23;
               double var25 = (var43 - var41) / var41 * 100.0;
               ((Double[])var18.get(var20))[IndexYTD] = var25;
               double var27 = (var49 - var47) / var47 * 100.0;
               ((Double[])var18.get(var20))[IndexBenchmark] = var27;
               double var29 = ((Double[])var18.get(var20))[IndexYTD] - ((Double[])var18.get(var20))[IndexBenchmark];
               ((Double[])var18.get(var20))[IndexComparison] = var29;
               var37 = var39;
            }

            if (var19 != var20) {
               var41 = var43;
               var47 = var49;
            }

            var20 = var19;
            var21 = var62;
         }

         var39 = var33;
         var43 = var33;
         var49 = var45;
         if (var35 != -1.0 && (((Double[])var18.get(var19))[IndexMaxDD] == 0.0 || ((Double[])var18.get(var19))[IndexMaxDD] > var35)) {
            ((Double[])var18.get(var19))[IndexMaxDD] = var35;
         }

         if (var51 != -1.0 && (((Double[])var18.get(var19))[IndexBenchmarkMaxDD] == 0.0 || ((Double[])var18.get(var19))[IndexBenchmarkMaxDD] > var51)) {
            ((Double[])var18.get(var19))[IndexBenchmarkMaxDD] = var51;
         }
      }

      if (var21 != -1) {
         double var57 = (var39 - var37) / var37 * 100.0;
         var22[var21] = var57;
         Double[] var61 = var22;
         int var63 = IndexYTD;
         var61[var63] = var61[var63] + var57;
         double var58 = (var43 - var41) / var41 * 100.0;
         ((Double[])var18.get(var20))[IndexYTD] = var58;
         double var59 = (var49 - var47) / var47 * 100.0;
         ((Double[])var18.get(var20))[IndexBenchmark] = var59;
         double var60 = ((Double[])var18.get(var20))[IndexYTD] - ((Double[])var18.get(var20))[IndexBenchmark];
         ((Double[])var18.get(var20))[IndexComparison] = var60;
      }

      return var18;
   }
}
