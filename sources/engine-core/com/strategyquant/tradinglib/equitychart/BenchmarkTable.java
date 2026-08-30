package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import org.json.JSONObject;

public class BenchmarkTable {
   public String MoneyManagement;
   public double InitialCapital;
   public String Symbol;
   public String Normalization;
   public String InitBuyAmount;
   public SQStats StatsStrategy;
   public SQStats StatsBenchmark;
   public SQStats StatsBenchmarkDDPct;
   public double Correlation;

   public String toJSON() {
      if (this.StatsStrategy == null) {
         return new JSONObject().toString();
      }

      JSONObject var1 = new JSONObject();
      var1.put("normalization", this.Normalization);
      var1.put("moneyManagement", this.MoneyManagement);
      var1.put("initialCapital", PlTypes.printPL(this.InitialCapital, (byte)10));
      var1.put("symbol", this.Symbol);
      var1.put("initBuyAmount", this.InitBuyAmount);
      double var2 = this.StatsStrategy.getDouble("NetProfit");
      double var4 = this.StatsBenchmarkDDPct.getDouble("NetProfit");
      var1.put("result", var2 > var4 ? L.tsq("better") : L.tsq("worse"));
      JSONObject var6 = new JSONObject();
      var6.put("NetProfit", SQUtils.d2(this.StatsStrategy.getDouble("NetProfit")));
      var6.put("NumberOfTrades", this.StatsStrategy.getInt("NumberOfTrades"));
      var6.put("Drawdown", SQUtils.d2(this.StatsStrategy.getDouble("Drawdown")));
      var6.put("DrawdownPct", SQUtils.d2(this.StatsStrategy.getDouble("DrawdownPct")));
      var6.put("ProfitFactor", SQUtils.d2(this.StatsStrategy.getDouble("ProfitFactor")));
      var6.put("SharpeRatio", SQUtils.d2(this.StatsStrategy.getDouble("SharpeRatio")));
      var6.put("ReturnDDRatio", SQUtils.d2(this.StatsStrategy.getDouble("ReturnDDRatio")));
      var6.put("AvgTrade", SQUtils.d2(this.StatsStrategy.getDouble("AvgTrade")));
      var6.put("CAGR", SQUtils.d2(this.StatsStrategy.getDouble("CAGR")));
      var6.put("AnnualPctReturnDDRatio", SQUtils.d2(this.StatsStrategy.getDouble("AnnualPctReturnDDRatio")));
      var6.put("Exposure", SQUtils.d2(this.StatsStrategy.getDouble("Exposure")));
      var6.put("Correlation", SQUtils.d2(this.Correlation));
      JSONObject var7 = new JSONObject();
      var7.put("NetProfit", SQUtils.d2(this.StatsBenchmark.getDouble("NetProfit")));
      var7.put("NumberOfTrades", 1);
      var7.put("Drawdown", SQUtils.d2(this.StatsBenchmark.getDouble("Drawdown")));
      var7.put("DrawdownPct", SQUtils.d2(this.StatsBenchmark.getDouble("DrawdownPct")));
      var7.put("ProfitFactor", SQUtils.d2(this.StatsBenchmark.getDouble("ProfitFactor")));
      var7.put("SharpeRatio", SQUtils.d2(this.StatsBenchmark.getDouble("SharpeRatio")));
      var7.put("ReturnDDRatio", SQUtils.d2(this.StatsBenchmark.getDouble("ReturnDDRatio")));
      var7.put("AvgTrade", SQUtils.d2(this.StatsBenchmark.getDouble("AvgTrade")));
      var7.put("CAGR", SQUtils.d2(this.StatsBenchmark.getDouble("CAGR")));
      var7.put("AnnualPctReturnDDRatio", SQUtils.d2(this.StatsBenchmark.getDouble("AnnualPctReturnDDRatio")));
      if (this.Normalization.equals("exposure")) {
         var7.put("Exposure", SQUtils.d2(this.StatsBenchmark.getDouble("Exposure", 1.0)));
      } else {
         var7.put("Exposure", 100);
      }

      JSONObject var8 = new JSONObject();
      var8.put("NetProfit", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("NetProfit")));
      var8.put("Drawdown", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("Drawdown")));
      var8.put("DrawdownPct", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("DrawdownPct")));
      var8.put("SharpeRatio", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("SharpeRatio")));
      var8.put("CAGR", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("CAGR")));
      var8.put("AnnualPctReturnDDRatio", SQUtils.d2(this.StatsBenchmarkDDPct.getDouble("AnnualPctReturnDDRatio")));
      JSONObject var9 = new JSONObject();
      var9.put("strategy", var6);
      var9.put("benchmark", var7);
      var9.put("benchmarkDD", var8);
      var1.put("stats", var9);
      return var1.toString();
   }

   public void clear() {
      this.MoneyManagement = null;
      this.InitialCapital = 0.0;
      this.Symbol = null;
      this.InitBuyAmount = null;
      this.StatsStrategy = null;
      this.StatsBenchmark = null;
      this.StatsBenchmarkDDPct = null;
   }

   public void fill(
      ResultsGroup var1, MoneyManagementMethod var2, String var3, SQStats var4, double var5, String var7, double var8, SQStats var10, SQStats var11
   ) throws Exception {
      this.MoneyManagement = var2.printFormatedName();
      this.InitialCapital = var2.getInitialCapital();
      var3 = var3.replace("_benchmark.D", "");
      this.Symbol = var3;
      this.Normalization = var7;
      this.InitBuyAmount = PlTypes.printPL(var8, (byte)10);
      this.Correlation = var5;
      this.StatsStrategy = var4;
      this.StatsBenchmark = var10;
      this.StatsBenchmarkDDPct = var11;
   }
}
