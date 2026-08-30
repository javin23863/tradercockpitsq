package com.strategyquant.plugin.Saver.impl.SQ3;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.file.SQ3Settings;
import java.io.File;
import java.util.Map;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

public class SQ3FileSaver {
   public void save(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception {
      Element var4 = new Element("StrategyFile");
      var4.setAttribute(new Attribute("Version", "SQ " + MainApp.printAppVersion(false)));
      Document var5 = new Document(var4);
      var4.addContent(this.getStrategyXml(var1, var3));
      Element var6 = new Element("TestResults");
      var4.addContent(var6);
      var6.addContent(this.getResultsXml(var1, var1.isPortfolio() ? "Portfolio" : var1.getMainResultKey()));
      var6.addContent(this.getOrdersXml(var1, var1.isPortfolio() ? "Portfolio" : var1.getMainResultKey()));
      if (var1.isPortfolio()) {
         var6.addContent(this.getAdditionalData(var1));
      }

      File var7 = new File(var2);
      XMLUtil.xmlToFile(var5, var7);
   }

   protected String getParam(Map<String, String[]> var1, String var2, String var3) throws Exception {
      return var1 != null && var1.containsKey(var2) ? ((String[])var1.get(var2))[0] : var3;
   }

   private Element getStrategyXml(ResultsGroup var1, Map<String, String[]> var2) throws Exception {
      Element var3 = new Element("Strategy").setAttribute("portfolio", var1.isPortfolio() + "");
      String var4 = var1.specialValues().getString(SpecialValues.Note, "");
      boolean var5 = Boolean.parseBoolean(this.getParam(var2, "setNoteActive", "false"));
      if (var5) {
         String var6 = this.getParam(var2, "setNoteType", "");
         String var7 = this.getParam(var2, "setNoteCustomText", "");
         Object var8 = null;
         Object var9 = null;
         if (var1.mainResult().containsKey(SpecialValues.Symbol)) {
            var8 = var1.mainResult().getString(SpecialValues.Symbol);
         } else {
            var8 = var1.specialValues().getString(SpecialValues.Symbol, "N/A");
         }

         if (var1.mainResult().containsKey(SpecialValues.Timeframe)) {
            var9 = var1.mainResult().getString(SpecialValues.Timeframe);
         } else {
            var9 = var1.specialValues().getString(SpecialValues.Timeframe, "N/A");
         }

         switch (var6) {
            case "symbol":
               var4 = (String)var8;
               break;
            case "timeframe":
               var4 = (String)var9;
               break;
            case "symbolTimeframe":
               var4 = var8 + "_" + var9;
               break;
            default:
               var4 = var7;
         }
      }

      var3.setAttribute("note", var4);
      Element var12 = new Element("StrategyNode").setAttribute("Class", "com.sonarbytes.gn.geneng.nodes.special.StrategyNode");
      var3.addContent(var12);
      Element var13 = new Element("Array");
      var12.addContent(var13);
      var13.addContent(new Element("EmptyNode").setAttribute("Class", "com.sonarbytes.gn.geneng.nodes.special.EmptyNode"));
      var13.addContent(new Element("EmptyNode").setAttribute("Class", "com.sonarbytes.gn.geneng.nodes.special.EmptyNode"));
      var13.addContent(new Element("EmptyNode").setAttribute("Class", "com.sonarbytes.gn.geneng.nodes.special.EmptyNode"));
      var13.addContent(new Element("EmptyNode").setAttribute("Class", "com.sonarbytes.gn.geneng.nodes.special.EmptyNode"));
      return var3;
   }

   private Element getSettingsXml(ResultsGroup var1, String var2) throws Exception {
      Element var3 = new Element("SettingsFile");
      var3.setAttribute(new Attribute("Version", "1.0"));
      var3.setAttribute(new Attribute("Name", ""));
      SQ3Settings var4 = new SQ3Settings();
      var3.addContent(var4.convertToSQ3Xml(var1));
      return var3;
   }

   private Element getOrdersXml(ResultsGroup var1, String var2) throws Exception {
      Element var3 = new Element("Orders");
      var3.setAttribute(new Attribute("Version", "2.0"));
      if (!var2.equals("Portfolio")) {
         OrdersList var4 = var1.orders().filterExcludingControlOrders(var2, (byte)0, (byte)127, false);

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Order var6 = var4.get(var5);
            var3.addContent(this.orderToXml(var6));
         }
      }

      return var3;
   }

   private Element getResultsXml(ResultsGroup var1, String var2) throws Exception {
      Element var3 = new Element("Results");
      var3.addContent(this.getStatsDataXml(var1, var2, "InSample", (byte)10, (byte)10));
      var3.addContent(this.getStatsDataXml(var1, var2, "OutOfSample", (byte)10, (byte)20));
      var3.addContent(this.getStatsDataXml(var1, var2, "FullSample", (byte)10, (byte)127));
      var3.addContent(this.getStatsDataXml(var1, var2, "InSampleTicks", (byte)30, (byte)10));
      var3.addContent(this.getStatsDataXml(var1, var2, "OutOfSampleTicks", (byte)30, (byte)20));
      var3.addContent(this.getStatsDataXml(var1, var2, "FullSampleTicks", (byte)30, (byte)127));
      var3.addContent(this.getStatsDataXml(var1, var2, "InSamplePct", (byte)20, (byte)10));
      var3.addContent(this.getStatsDataXml(var1, var2, "OutOfSamplePct", (byte)20, (byte)20));
      var3.addContent(this.getStatsDataXml(var1, var2, "FullSamplePct", (byte)20, (byte)127));
      var3.addContent(this.getSettingsXml(var1, var2));
      return var3;
   }

   private Element getStatsDataXml(ResultsGroup var1, String var2, String var3, byte var4, byte var5) throws Exception {
      Result var6 = var1.subResult(var2);
      Element var7 = new Element("StatsData");
      var7.setAttribute("SampleType", var3);
      var7.addContent(this.getValuesXml(0, var6.statsOrNull((byte)0, var4, var5)));
      var7.addContent(this.getValuesXml(1, var6.statsOrNull((byte)0, var4, var5)));
      var7.addContent(this.getValuesXml(2, var6.statsOrNull((byte)0, var4, var5)));
      return var7;
   }

   public Element getValuesXml(int var1, SQStats var2) {
      Element var3 = new Element("Values");
      var3.setAttribute("Index", Integer.valueOf(var1).toString());
      if (var2 == null) {
         return var3;
      }

      var3.addContent(new Element("netProfit").addContent(var2.getDouble("NetProfit") + ""));
      var3.addContent(new Element("numberOfTrades").addContent(var2.getInt("NumberOfTrades") + ""));
      var3.addContent(new Element("pctWins").addContent(var2.getDouble("WinningPct") + ""));
      var3.addContent(new Element("profitFactor").addContent(var2.getDouble("ProfitFactor") + ""));
      var3.addContent(new Element("drawdown").addContent(var2.getDouble("Drawdown") + ""));
      var3.addContent(new Element("pctDrawdown").addContent(var2.getDouble("DrawdownPct") + ""));
      var3.addContent(new Element("winLossRatio").addContent(var2.getDouble("WinLossRatio") + ""));
      var3.addContent(new Element("retDDRatio").addContent(var2.getDouble("ReturnDDRatio") + ""));
      var3.addContent(new Element("avgWin").addContent(var2.getDouble("AvgWin") + ""));
      var3.addContent(new Element("avgLoss").addContent(var2.getDouble("AvgLoss") + ""));
      var3.addContent(new Element("avgBarsWin").addContent(var2.getDouble("AvgBarsWin") + ""));
      var3.addContent(new Element("avgBarsLoss").addContent(var2.getDouble("AvgBarsLoss") + ""));
      var3.addContent(new Element("avgTrade").addContent(var2.getDouble("AvgTrade") + ""));
      var3.addContent(new Element("avgBarsTrade").addContent(var2.getDouble("AvgBarsInTrade") + ""));
      var3.addContent(new Element("payoutRatio").addContent(var2.getDouble("PayoutRatio") + ""));
      var3.addContent(new Element("numberOfWins").addContent(var2.getInt("NumberOfProfits") + ""));
      var3.addContent(new Element("numberOfLoss").addContent(var2.getInt("NumberOfLosses") + ""));
      var3.addContent(new Element("grossProfit").addContent(var2.getDouble("GrossProfit") + ""));
      var3.addContent(new Element("grossLoss").addContent(var2.getDouble("GrossLoss") + ""));
      var3.addContent(new Element("largestWin").addContent(var2.getDouble("MaxProfit") + ""));
      var3.addContent(new Element("largestLoss").addContent(var2.getDouble("MaxLoss") + ""));
      var3.addContent(new Element("maxConsecWins").addContent(var2.getInt("MaxConsecWins") + ""));
      var3.addContent(new Element("maxConsecLoss").addContent(var2.getInt("MaxConsecLosses") + ""));
      var3.addContent(new Element("avgConsecWin").addContent(var2.getDouble("AvgConsecWins") + ""));
      var3.addContent(new Element("avgConsecLoss").addContent(var2.getDouble("AvgConsecLosses") + ""));
      var3.addContent(new Element("correlation").addContent(var2.getDouble("Correlation") + ""));
      var3.addContent(new Element("symmetry").addContent(var2.getDouble("Symmetry") + ""));
      var3.addContent(new Element("ordersHash").addContent("0"));
      var3.addContent(new Element("exposure").addContent(var2.getDouble("Exposure") + ""));
      var3.addContent(new Element("stagnationFrom").addContent(var2.getLong("StagnationFrom") / 1000L + ""));
      var3.addContent(new Element("stagnationTo").addContent(var2.getLong("StagnationTo") / 1000L + ""));
      var3.addContent(new Element("stagnationPeriod").addContent(var2.getInt("Stagnation") + ""));
      var3.addContent(new Element("stagnationPeriodPct").addContent(var2.getDouble("StagnationPct") + ""));
      var3.addContent(new Element("degreesOfFreedom").addContent(var2.getInt("DegreesOfFreedom") + ""));
      var3.addContent(new Element("complexity").addContent("0"));
      var3.addContent(new Element("AHPR").addContent(var2.getDouble("AHPR") + ""));
      var3.addContent(new Element("sharpeRatio").addContent(var2.getDouble("SharpeRatio") + ""));
      var3.addContent(new Element("pctLoss").addContent("0"));
      var3.addContent(new Element("expectancy").addContent(var2.getDouble("Expectancy") + ""));
      var3.addContent(new Element("standardDev").addContent(var2.getDouble("StandardDev") + ""));
      var3.addContent(new Element("zProbability").addContent(var2.getDouble("ZProbability") + ""));
      var3.addContent(new Element("zScore").addContent(var2.getDouble("ZScore") + ""));
      var3.addContent(new Element("avgProfitByDay").addContent(var2.getDouble("AvgProfitPerDay") + ""));
      var3.addContent(new Element("avgProfitByMonth").addContent(var2.getDouble("AvgProfitPerMonth") + ""));
      var3.addContent(new Element("avgProfitByYear").addContent(var2.getDouble("AvgProfitPerYear") + ""));
      var3.addContent(new Element("rexpectancy").addContent(var2.getDouble("RExpectancy") + ""));
      var3.addContent(new Element("rexpectancyopp").addContent(var2.getDouble("RExpectancyScore") + ""));
      var3.addContent(new Element("aarDDRatio").addContent(var2.getDouble("AnnualPctReturnDDRatio") + ""));
      var3.addContent(new Element("avgPctProfitByYear").addContent("0"));
      var3.addContent(new Element("avgPctProfitByMonth").addContent("0"));
      var3.addContent(new Element("sqn").addContent(var2.getDouble("SQN") + ""));
      var3.addContent(new Element("sqn_score").addContent(var2.getDouble("SQNScore") + ""));
      var3.addContent(new Element("reached100PctDD").addContent("0"));
      var3.addContent(new Element("totalDaysOfTrading").addContent(var2.getInt("TotalTradingDays") + ""));
      return var3;
   }

   private Element orderToXml(Order var1) {
      Element var2 = new Element("Order");
      DataInfo var3 = DataManager.getDataInfo("History", var1.Symbol);
      var2.addContent(new Element("ticket").addContent(var1.Ticket + ""));
      var2.addContent(new Element("type").addContent(var1.Type + ""));
      var2.addContent(new Element("originalType").addContent(var1.OriginalType + ""));
      var2.addContent(new Element("volume").addContent(var1.Size + ""));
      var2.addContent(new Element("openPrice").addContent(var1.OpenPrice + ""));
      var2.addContent(new Element("closePrice").addContent(var1.ClosePrice + ""));
      var2.addContent(new Element("stoploss").addContent(var1.StopLoss + ""));
      var2.addContent(new Element("takeprofit").addContent(var1.TakeProfit + ""));
      var2.addContent(new Element("openTime").addContent(var1.OpenTime / 1000L + ""));
      var2.addContent(new Element("closeTime").addContent(var1.CloseTime / 1000L + ""));
      var2.addContent(new Element("closeType").addContent(var1.CloseType + ""));
      var2.addContent(new Element("barsInTrade").addContent(var1.BarsInTrade + ""));
      var2.addContent(new Element("pl").addContent(var1.PL + ""));
      var2.addContent(new Element("pctPL").addContent(var1.PctPL + ""));
      var2.addContent(new Element("pipsPL").addContent(var1.PipsPL + ""));
      var2.addContent(new Element("outOfSample").addContent((var1.Type == 20) + ""));
      var2.addContent(new Element("inSampleValidation").addContent("false"));
      var2.addContent(new Element("status").addContent("0"));
      double var4 = 0.0;
      double var6 = 0.0;
      if (var3 != null && var4 != 0.0 && var6 != 0.0) {
         if (OrderTypes.isLongOrder(var1.Type)) {
            var4 = this.moneyToPips(-1.0F * var1.MAE, var3, var1) * var3.symbolInfo.tickSize + var1.OpenPrice;
            var6 = this.moneyToPips(var1.MFE, var3, var1) * var3.symbolInfo.tickSize + var1.OpenPrice;
         } else {
            var4 = this.moneyToPips(-1.0F * var1.MAE, var3, var1) * var3.symbolInfo.tickSize - var1.OpenPrice;
            var6 = this.moneyToPips(var1.MFE, var3, var1) * var3.symbolInfo.tickSize - var1.OpenPrice;
         }
      }

      var2.addContent(new Element("mae").addContent(Math.abs(var4) + ""));
      var2.addContent(new Element("mfe").addContent(Math.abs(var6) + ""));
      var2.addContent(new Element("dd").addContent(var1.DD + ""));
      var2.addContent(new Element("pctDD").addContent(var1.PctDD + ""));
      var2.addContent(new Element("pipsDD").addContent(var1.PipsDD + ""));
      var2.addContent(new Element("commSwap").addContent(-var1.CommSwap + ""));
      return var2;
   }

   private double moneyToPips(double var1, DataInfo var3, Order var4) {
      return var1 / var4.Size / var3.symbolInfo.pointValue / var3.symbolInfo.tickSize;
   }

   private Element getAdditionalData(ResultsGroup var1) throws Exception {
      Element var2 = new Element("AdditionalDataResults");
      Element var3 = new Element("PortfolioResult");
      var3.addContent(this.getAdditionalDataResult(var1, "Portfolio"));
      var2.addContent(var3);
      Element var4 = new Element("AdResults");
      var2.addContent(var4);

      for (String var6 : var1.getResultKeys()) {
         if (!var6.equals("Portfolio")) {
            var4.addContent(this.getAdditionalDataResult(var1, var6));
         }
      }

      return var2;
   }

   private Element getAdditionalDataResult(ResultsGroup var1, String var2) throws Exception {
      Result var3 = var1.subResult(var2);
      Element var4 = new Element("AdditionalDataResult");
      SettingsMap var5 = var1.portfolio().getSettings();
      ChartSetup var6 = var5 == null ? null : (ChartSetup)var5.get("BacktestChart");
      ChartDef var7 = var6 == null ? null : (ChartDef)var6.getCharts().get(0);
      if (!var2.equals("Portfolio")) {
         var4.addContent(new Element("generationId").addContent(var2));
         var4.addContent(new Element("symbol").addContent(var2));
         String var8 = var7 == null ? null : var7.getTimeframe();
         var4.addContent(new Element("timeframe").addContent(SQ3Settings.translateSQ4TFToInt(var8) + ""));
         var4.addContent(new Element("dateFrom").addContent(var7 == null ? "0" : var7.getHistoryFrom() / 1000L + ""));
         var4.addContent(new Element("dateTo").addContent(var7 == null ? "0" : var7.getHistoryTo() / 1000L + ""));
         var4.addContent(new Element("testPrecision").addContent("1"));
         var4.addContent(new Element("spread").addContent("3"));
         var4.addContent(new Element("slippage").addContent("0"));
         var4.addContent(new Element("minDistance").addContent("5"));
         var4.addContent(this.getOrdersXml(var1, var2));
      }

      Element var12 = new Element("AdditionalStrategyStatsData");
      var4.addContent(var12);
      Element var9 = new Element("ValuesAll");
      var12.addContent(var9);
      var9.addContent(this.getValuesXml(0, var3.stats((byte)0, (byte)10, (byte)127)));
      Element var10 = new Element("ValuesLong");
      var12.addContent(var10);
      var10.addContent(this.getValuesXml(0, var3.stats((byte)1, (byte)10, (byte)127)));
      Element var11 = new Element("ValuesShort");
      var12.addContent(var11);
      var11.addContent(this.getValuesXml(0, var3.stats((byte)-1, (byte)10, (byte)127)));
      return var4;
   }
}
