package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import org.jdom2.Element;

public class FixedProfit extends AbstractExitLevel {
   private int Type;
   private int FixedPips;
   private double AtrMultiplicator;
   private int AtrPeriod;

   public FixedProfit(Element var1, int var2) throws Exception {
      super(var1, var2);
      this.Type = XMLUtil.getItemIntParam(var1, "Type", 1);
      this.FixedPips = XMLUtil.getItemIntParam(var1, "FixedPips", 50);
      this.AtrMultiplicator = XMLUtil.getItemDoubleParam(var1, "AtrMultiplicator", 1.2);
      this.AtrPeriod = XMLUtil.getItemIntParam(var1, "AtrPeriod", 20);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("ExitLevel");
      var1.setAttribute("key", "FixedProfit");
      XMLUtil.setItemIntParam(var1, "Type", this.Type);
      XMLUtil.setItemIntParam(var1, "FixedPips", this.FixedPips);
      XMLUtil.setItemDoubleParam(var1, "AtrMultiplicator", this.AtrMultiplicator);
      XMLUtil.setItemIntParam(var1, "AtrPeriod", this.AtrPeriod);
      return var1;
   }

   @Override
   public void setForOrder(ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException {
      if (var1.isFilled()) {
         this.setStrategy(var2);
         this.setOrderPTValue(var1, var2);
      }
   }

   @Override
   public double getNettingPrice(StrategyBase var1, ILiveOrder var2, double var3, double var5) throws TradingException {
      if (var5 == 0.0) {
         return 0.0;
      }

      int var7 = var2.isLong() ? 1 : -1;
      double var8 = var2.isNettingMode() && var2.getLastOpenPrice() > 0.0 ? var2.getLastOpenPrice() : var2.getOpenPrice();
      double var10 = this.getByType(var2, var1);
      return var7 > 0 ? var8 + var10 : var8 - var10;
   }

   private void setOrderPTValue(ILiveOrder var1, StrategyBase var2) throws TradingException {
      int var3 = var1.isLong() ? 1 : -1;
      double var4 = var1.isNettingMode() && var1.getLastOpenPrice() > 0.0 ? var1.getLastOpenPrice() : var1.getOpenPrice();
      double var6 = this.getByType(var1, var2);
      double var8;
      if (var3 > 0) {
         var8 = var4 + var6;
      } else {
         var8 = var4 - var6;
      }

      this.setNewPT(var1, var8, var4);
   }

   private double getByType(ILiveOrder var1, StrategyBase var2) throws TradingException {
      if (this.Type == 1) {
         return var2.convertPipsToRealPrice(var1.getSymbol(), this.FixedPips);
      }

      ChartData var3 = var2.MarketData.Chart(var1.getSymbol());
      double var4 = var2.getATRValue(var3, this.AtrPeriod, 1);
      return this.AtrMultiplicator * SQUtils.round(var4, 6);
   }

   @Override
   public String toString() {
      return this.Type == 1
         ? String.format("at %d pips above Order Open price", this.FixedPips)
         : String.format("at fixed profit %s * ATR(%d)", this.AtrMultiplicator, this.AtrPeriod);
   }

   public static Element generate(IRandomGenerator var0, ATMGenerateConfig var1) {
      Element var2 = new Element("ExitLevel");
      var2.setAttribute("key", "FixedProfit");
      var2.setAttribute("use", "true");
      int var4 = 50;
      double var5 = 1.2;
      int var7 = 20;
      byte var3;
      if (var0.nextBool()) {
         var3 = 1;
         var4 = var0.nextInt(var1.fixedProfitFixedPipsMin, var1.fixedProfitFixedPipsMax, 1);
      } else {
         var3 = 2;
         var5 = var0.nextDouble(var1.fixedProfitATRMutliplierMin, var1.fixedProfitATRMutliplierMax, 0.05, 2);
         var7 = var0.nextInt(var1.fixedProfitATRPeriodMin, var1.fixedProfitATRPeriodMax, 1);
      }

      XMLUtil.setItemIntParam(var2, "Type", var3);
      XMLUtil.setItemIntParam(var2, "FixedPips", var4);
      XMLUtil.setItemDoubleParam(var2, "AtrMultiplicator", var5);
      XMLUtil.setItemIntParam(var2, "AtrPeriod", var7);
      return var2;
   }
}
