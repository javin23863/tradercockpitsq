package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.jdom2.Element;

public abstract class AbstractExitLevel extends ExitMethod {
   protected int exitIndex = 0;

   public AbstractExitLevel(Element var1, int var2) {
      this.exitIndex = var2;
   }

   public abstract void setForOrder(ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException;

   public abstract Element getXML();

   public abstract double getNettingPrice(StrategyBase var1, ILiveOrder var2, double var3, double var5) throws TradingException;

   public void onBarOpen(StrategyBase var1, ILiveOrder var2, ILiveOrder var3, double var4, double var6) throws TradingException {
   }

   @Override
   public double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException {
      throw new TradingException("Method shouldn't be called (ATM)!");
   }

   @Override
   public void setForOrder(ILiveOrder var1, StrategyBase var2) throws TradingException {
      throw new TradingException("Method shouldn't be called (ATM)!");
   }

   protected void setStrategy(StrategyBase var1) {
      this.Strategy = var1;
   }

   protected void setNewPT(ILiveOrder var1, double var2, double var4) throws TradingException {
      if (var2 != var4) {
         var2 = this.correctSLPT(var1, var2, false);
         if (var2 != 0.0) {
            var1.setPT(var2).Send();
            var1.setExitIndex((byte)this.exitIndex);
            if (!var1.isSuccessful()) {
               var1.Close((byte)8);
            }
         }
      }
   }
}
