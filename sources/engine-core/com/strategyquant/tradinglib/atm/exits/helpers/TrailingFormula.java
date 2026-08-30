package com.strategyquant.tradinglib.atm.exits.helpers;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.StrategyBase;
import org.jdom2.Element;

public class TrailingFormula implements IFormula {
   private int type;
   private int fixedPips = -99999999;
   private double atrMultiplicator = -9.9999999E7;
   private int atrPeriod;
   private int cachedBars = -1;
   private StrategyBase cachedStrategy = null;
   private double cachedATR;

   public TrailingFormula(int var1, int var2, double var3, int var5) {
      this.type = var1;
      this.fixedPips = var2;
      this.atrMultiplicator = var3;
      this.atrPeriod = var5;
   }

   @Override
   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return this.type == 1 ? this.evaluateFixedPips(var1, var2, var3, var5) : this.evaluateATRBased(var1, var2, var3, var5);
   }

   private double evaluateATRBased(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      if (this.atrMultiplicator == -9.9999999E7) {
         return -9.9999999E7;
      }

      ChartData var6 = var1.MarketData.Chart(var2);
      double var7 = SQUtils.round(this.getCachedATR(var6, var1), 6);
      double var9 = this.atrMultiplicator * var7;
      return var5 == 1 ? SQUtils.round(var3 - var9, 5) : SQUtils.round(var3 + var9, 5);
   }

   private double getCachedATR(ChartData var1, StrategyBase var2) throws TradingException {
      int var3 = var1.Bars();
      if (var3 != this.cachedBars || this.cachedStrategy != var2) {
         this.cachedBars = var3;
         this.cachedStrategy = var2;
         this.cachedATR = SQUtils.round6(var2.getATRValue(var1, this.atrPeriod, 1));
      }

      return this.cachedATR;
   }

   private double evaluateFixedPips(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      if (this.fixedPips == -99999999) {
         return -9.9999999E7;
      }

      double var6 = var1.convertPipsToRealPrice(var2, this.fixedPips);
      return var5 == 1 ? var1.MarketData.Chart(var2).Bid() - var6 : var1.MarketData.Chart(var2).Ask() + var6;
   }

   @Override
   public IFormula newFormulaInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      return null;
   }

   @Override
   public boolean isNoneValue() {
      return false;
   }

   @Override
   public boolean isBooleanValue() {
      return false;
   }
}
