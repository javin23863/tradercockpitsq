package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import org.jdom2.Element;

public class MultipleOfOriginalPT extends AbstractExitLevel {
   private double Multiplicator;

   public MultipleOfOriginalPT(Element var1, int var2) throws Exception {
      super(var1, var2);
      this.Multiplicator = XMLUtil.getItemDoubleParam(var1, "Multiplicator", 0.01);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("ExitLevel");
      var1.setAttribute("key", "MultipleOfOriginalPT");
      XMLUtil.setItemDoubleParam(var1, "Multiplicator", this.Multiplicator);
      return var1;
   }

   @Override
   public void setForOrder(ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException {
      if (var1.isFilled()) {
         this.setStrategy(var2);
         this.setOrderPTValue(var1, var2, var5);
      }
   }

   @Override
   public double getNettingPrice(StrategyBase var1, ILiveOrder var2, double var3, double var5) {
      if (var5 == -9.9999999E7) {
         return -9.9999999E7;
      }

      int var7 = var2.isLong() ? 1 : -1;
      double var8 = var2.isNettingMode() && var2.getLastOpenPrice() > 0.0 ? var2.getLastOpenPrice() : var2.getOpenPrice();
      double var10 = Math.abs(var8 - var5);
      double var12 = this.Multiplicator * var10;
      return var8 + var7 * var12;
   }

   private void setOrderPTValue(ILiveOrder var1, StrategyBase var2, double var3) throws TradingException {
      if (var3 != -9.9999999E7) {
         int var5 = var1.isLong() ? 1 : -1;
         double var6 = var1.isNettingMode() && var1.getLastOpenPrice() > 0.0 ? var1.getLastOpenPrice() : var1.getOpenPrice();
         double var8 = Math.abs(var6 - var3);
         double var10 = this.Multiplicator * var8;
         double var12 = var6 + var5 * var10;
         this.setNewPT(var1, var12, var6);
      }
   }

   @Override
   public String toString() {
      return String.format("at %s x Original Profit Target size", this.Multiplicator);
   }

   public static Element generate(IRandomGenerator var0, ATMGenerateConfig var1) {
      Element var2 = new Element("ExitLevel");
      var2.setAttribute("key", "MultipleOfOriginalPT");
      var2.setAttribute("use", "true");
      double var3 = var0.nextDouble(var1.ptMultipleMin, var1.ptMultipleMax, 0.05, 2);
      XMLUtil.setItemDoubleParam(var2, "Multiplicator", var3);
      return var2;
   }
}
