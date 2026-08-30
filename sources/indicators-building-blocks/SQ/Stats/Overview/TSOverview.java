package SQ.Stats.Overview;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TSOverview extends OverviewTemplate {
   public TSOverview() {
      this.setName(L.tsq("TS Overview"));
      this.setScreenshotName("ts_screenshot.jpg");
      this.setHtmlTemplateName("ts_template.htm");

      try {
         this.loadTemplate();
      } catch (Exception var2) {
         Log.error("Overview template couldn't be loaded. Exc. ", var2);
      }
   }

   public String drawValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      this.reset();
      this.replaceValues(var1, var2, var3);
      return this.print();
   }

   public void replaceValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      SQStats var4 = null;
      SQStats var5 = null;
      SQStats var6 = null;
      Result var7 = null;
      if (var1 != null) {
         var7 = var1.subResult(var2);
         if (var7 != null) {
            var4 = var7.stats((byte)0, var3.getPLType(), (byte)127);
            var5 = var7.stats((byte)1, var3.getPLType(), (byte)127);
            var6 = var7.stats((byte)-1, var3.getPLType(), (byte)127);
         }
      }

      this.replaceLabel("1", L.t("Total Net Profit", new Object[0]));
      this.replaceValue(
         "1_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("NetProfit"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("NetProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "1_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("NetProfit"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("NetProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "1_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("NetProfit"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("NetProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("2", L.t("Gross Profit", new Object[0]));
      this.replaceValue(
         "2_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("GrossProfit"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("GrossProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "2_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossProfit"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("GrossProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "2_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("GrossProfit"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("GrossProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("3", L.t("Gross Loss", new Object[0]));
      this.replaceValue(
         "3_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("GrossLoss"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("GrossLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "3_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("GrossLoss"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("GrossLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "3_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("GrossLoss"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("GrossLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("4", L.t("Profit Factor", new Object[0]));
      this.replaceValue("4_All", var4 == null ? NA : this.d2(var4.getDouble("ProfitFactor")));
      this.replaceValue("4_Long", var5 == null ? NA : this.d2(var5.getDouble("ProfitFactor")));
      this.replaceValue("4_Short", var6 == null ? NA : this.d2(var6.getDouble("ProfitFactor")));
      this.replaceLabel("5", L.t("Total Number of trades", new Object[0]));
      this.replaceValue("5_All", var4 == null ? NA : var4.getInt("NumberOfTrades") + "");
      this.replaceValue("5_Long", var5 == null ? NA : var5.getInt("NumberOfTrades") + "");
      this.replaceValue("5_Short", var6 == null ? NA : var6.getInt("NumberOfTrades") + "");
      this.replaceLabel("6", L.t("Percent Profitable", new Object[0]));
      this.replaceValue("6_All", var4 == null ? NA : this.d2WithPlType(var4.getDouble("WinningPct"), (byte)20));
      this.replaceValue("6_Long", var5 == null ? NA : this.d2WithPlType(var5.getDouble("WinningPct"), (byte)20));
      this.replaceValue("6_Short", var6 == null ? NA : this.d2WithPlType(var6.getDouble("WinningPct"), (byte)20));
      this.replaceLabel("7", L.t("Winning Trades", new Object[0]));
      this.replaceValue("7_All", var4 == null ? NA : var4.getInt("NumberOfProfits") + "");
      this.replaceValue("7_Long", var5 == null ? NA : var5.getInt("NumberOfProfits") + "");
      this.replaceValue("7_Short", var6 == null ? NA : var6.getInt("NumberOfProfits") + "");
      this.replaceLabel("8", L.t("Losing Trades", new Object[0]));
      this.replaceValue("8_All", var4 == null ? NA : var4.getInt("NumberOfLosses") + "");
      this.replaceValue("8_Long", var5 == null ? NA : var5.getInt("NumberOfLosses") + "");
      this.replaceValue("8_Short", var6 == null ? NA : var6.getInt("NumberOfLosses") + "");
      this.replaceLabel("9", L.t("Avg. Trade Net Profit", new Object[0]));
      this.replaceValue(
         "9_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgTrade"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("AvgTrade") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "9_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgTrade"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgTrade") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "9_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgTrade"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("AvgTrade") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("10", L.t("Avg. Winning Trade", new Object[0]));
      this.replaceValue("10_All", var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgWin"), var3.getPLType()));
      this.replaceValue("10_Long", var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgWin"), var3.getPLType()));
      this.replaceValue("10_Short", var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgWin"), var3.getPLType()));
      this.replaceLabel("11", L.t("Payout Ratio (Avg Win/Loss)", new Object[0]));
      this.replaceValue("11_All", var4 == null ? NA : this.d2(var4.getDouble("PayoutRatio")));
      this.replaceValue("11_Long", var5 == null ? NA : this.d2(var5.getDouble("PayoutRatio")));
      this.replaceValue("11_Short", var6 == null ? NA : this.d2(var6.getDouble("PayoutRatio")));
      this.replaceLabel("12", L.t("Avg. Losing Trade", new Object[0]));
      this.replaceValue(
         "12_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgLoss"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("AvgLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "12_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgLoss"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "12_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgLoss"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("AvgLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("13", L.t("Avg. Profit by Day", new Object[0]));
      this.replaceValue(
         "13_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgProfitPerDay"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("AvgProfitPerDay") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "13_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerDay"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgProfitPerDay") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "13_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgProfitPerDay"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("AvgProfitPerDay") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("14", L.t("Avg. Profit by Month", new Object[0]));
      this.replaceValue(
         "14_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgProfitPerMonth"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("AvgProfitPerMonth") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "14_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerMonth"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgProfitPerMonth") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "14_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgProfitPerMonth"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("AvgProfitPerMonth") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("15", L.t("Avg. Profit by Year", new Object[0]));
      this.replaceValue(
         "15_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("AvgProfitPerYear"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("AvgProfitPerYear") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "15_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("AvgProfitPerYear"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("AvgProfitPerYear") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "15_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("AvgProfitPerYear"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("AvgProfitPerYear") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("16", L.t("Largest Winning Trade", new Object[0]));
      this.replaceValue(
         "16_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("MaxProfit"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("MaxProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "16_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxProfit"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("MaxProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "16_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("MaxProfit"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("MaxProfit") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("17", L.t("Largest Losing Trade", new Object[0]));
      this.replaceValue(
         "17_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("MaxLoss"), var3.getPLType()),
         var4 == null ? null : (var4.getDouble("MaxLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "17_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("MaxLoss"), var3.getPLType()),
         var5 == null ? null : (var5.getDouble("MaxLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "17_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("MaxLoss"), var3.getPLType()),
         var6 == null ? null : (var6.getDouble("MaxLoss") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("18", L.t("Avg Consecutive Winning Trades", new Object[0]));
      this.replaceValue("18_All", var4 == null ? NA : this.d2(var4.getDouble("AvgConsecWins")));
      this.replaceValue("18_Long", var5 == null ? NA : this.d2(var5.getDouble("AvgConsecWins")));
      this.replaceValue("18_Short", var6 == null ? NA : this.d2(var6.getDouble("AvgConsecWins")));
      this.replaceLabel("19", L.t("Avg Consecutive Losing Trades", new Object[0]));
      this.replaceValue("19_All", var4 == null ? NA : this.d2(var4.getDouble("AvgConsecLosses")));
      this.replaceValue("19_Long", var5 == null ? NA : this.d2(var5.getDouble("AvgConsecLosses")));
      this.replaceValue("19_Short", var6 == null ? NA : this.d2(var6.getDouble("AvgConsecLosses")));
      this.replaceLabel("20", L.t("Max. Consecutive Winning Trades", new Object[0]));
      this.replaceValue("20_All", var4 == null ? NA : var4.getInt("MaxConsecWins") + "");
      this.replaceValue("20_Long", var5 == null ? NA : var5.getInt("MaxConsecWins") + "");
      this.replaceValue("20_Short", var6 == null ? NA : var6.getInt("MaxConsecWins") + "");
      this.replaceLabel("21", L.t("Max. Consecutive Losing Trades", new Object[0]));
      this.replaceValue("21_All", var4 == null ? NA : var4.getInt("MaxConsecLosses") + "");
      this.replaceValue("21_Long", var5 == null ? NA : var5.getInt("MaxConsecLosses") + "");
      this.replaceValue("21_Short", var6 == null ? NA : var6.getInt("MaxConsecLosses") + "");
      this.replaceLabel("22", L.t("Avg. Bars in Total Trades", new Object[0]));
      this.replaceValue("22_All", var4 == null ? NA : this.d2(var4.getDouble("AvgBarsInTrade")));
      this.replaceValue("22_Long", var5 == null ? NA : this.d2(var5.getDouble("AvgBarsInTrade")));
      this.replaceValue("22_Short", var6 == null ? NA : this.d2(var6.getDouble("AvgBarsInTrade")));
      this.replaceLabel("23", L.t("Avg. Bars in Winning Trades", new Object[0]));
      this.replaceValue("23_All", var4 == null ? NA : this.d2(var4.getDouble("AvgBarsWin")));
      this.replaceValue("23_Long", var5 == null ? NA : this.d2(var5.getDouble("AvgBarsWin")));
      this.replaceValue("23_Short", var6 == null ? NA : this.d2(var6.getDouble("AvgBarsWin")));
      this.replaceLabel("24", L.t("Avg. Bars in Losing Trades", new Object[0]));
      this.replaceValue("24_All", var4 == null ? NA : this.d2(var4.getDouble("AvgBarsLoss")));
      this.replaceValue("24_Long", var5 == null ? NA : this.d2(var5.getDouble("AvgBarsLoss")));
      this.replaceValue("24_Short", var6 == null ? NA : this.d2(var6.getDouble("AvgBarsLoss")));
      String var8 = var3.getPLType() == 20 ? "DrawdownPct" : (var3.getPLType() == 30 ? "DrawdownPips" : "Drawdown");
      this.replaceLabel("25", L.t("Drawdown", new Object[0]));
      this.replaceValue(
         "25_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble(var8), var3.getPLType()),
         var4 == null ? null : (var4.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "25_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble(var8), var3.getPLType()),
         var5 == null ? null : (var5.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "25_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble(var8), var3.getPLType()),
         var6 == null ? null : (var6.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("26", L.t("% Drawdown", new Object[0]));
      this.replaceValue(
         "26_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("DrawdownPct"), (byte)20),
         var4 == null ? null : (var4.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "26_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("DrawdownPct"), (byte)20),
         var5 == null ? null : (var5.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "26_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("DrawdownPct"), (byte)20),
         var6 == null ? null : (var6.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("25", L.t("Drawdown", new Object[0]));
      this.replaceValue(
         "25_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble(var8), var3.getPLType()),
         var4 == null ? null : (var4.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "25_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble(var8), var3.getPLType()),
         var5 == null ? null : (var5.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "25_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble(var8), var3.getPLType()),
         var6 == null ? null : (var6.getDouble(var8) > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("26", L.t("% Drawdown", new Object[0]));
      this.replaceValue(
         "26_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("DrawdownPct"), (byte)20),
         var4 == null ? null : (var4.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "26_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("DrawdownPct"), (byte)20),
         var5 == null ? null : (var5.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "26_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("DrawdownPct"), (byte)20),
         var6 == null ? null : (var6.getDouble("DrawdownPct") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("27", L.t("Total Slippage ($)", new Object[0]));
      this.replaceValue(
         "27_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("SlippageInMoney"), (byte)10),
         var4 == null ? null : (var4.getDouble("SlippageInMoney") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "27_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("SlippageInMoney"), (byte)10),
         var5 == null ? null : (var5.getDouble("SlippageInMoney") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "27_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("SlippageInMoney"), (byte)10),
         var6 == null ? null : (var6.getDouble("SlippageInMoney") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceLabel("28", L.t("Total Commission ($)", new Object[0]));
      this.replaceValue(
         "28_All",
         var4 == null ? NA : this.d2WithPlType(var4.getDouble("Commission"), (byte)10),
         var4 == null ? null : (var4.getDouble("Commission") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "28_Long",
         var5 == null ? NA : this.d2WithPlType(var5.getDouble("Commission"), (byte)10),
         var5 == null ? null : (var5.getDouble("Commission") > 0.0 ? "positiveNum" : "negativeNum")
      );
      this.replaceValue(
         "28_Short",
         var6 == null ? NA : this.d2WithPlType(var6.getDouble("Commission"), (byte)10),
         var6 == null ? null : (var6.getDouble("Commission") > 0.0 ? "positiveNum" : "negativeNum")
      );
   }
}
