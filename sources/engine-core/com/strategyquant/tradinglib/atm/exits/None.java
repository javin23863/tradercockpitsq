package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import org.jdom2.Element;

public class None extends AbstractExitLevel {
   private boolean LimitExitAfterBars;
   private int MaxBars;
   private boolean active = true;

   public None(Element var1, int var2) throws Exception {
      super(var1, var2);
      this.LimitExitAfterBars = XMLUtil.getItemBoolParam(var1, "LimitExitAfterBars", false);
      this.MaxBars = XMLUtil.getItemIntParam(var1, "MaxBars", 50);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("ExitLevel");
      var1.setAttribute("key", "None");
      XMLUtil.setItemBoolParam(var1, "LimitExitAfterBars", this.LimitExitAfterBars);
      XMLUtil.setItemIntParam(var1, "MaxBars", this.MaxBars);
      return var1;
   }

   @Override
   public void setForOrder(final ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException {
      if (this.LimitExitAfterBars && this.MaxBars > 0) {
         var1.registerEvent(2, new IActionEventListener() {
            @Override
            public void OnActionEvent(StrategyBase var1x) throws TradingException {
               if (None.this.checkExitAfterBars(var1)) {
                  var1.Close((byte)19);
                  var1.setExitIndex((byte)None.this.exitIndex);
               }
            }
         });
      }
   }

   public boolean checkExitAfterBars(ILiveOrder var1) throws TradingException {
      return this.active && !var1.isClosedOrder()
         ? this.LimitExitAfterBars && this.MaxBars > 0 && var1.isMarketOrder() && var1.getBarsInTrade() >= this.MaxBars
         : false;
   }

   @Override
   public double getNettingPrice(StrategyBase var1, ILiveOrder var2, double var3, double var5) throws TradingException {
      return -9.9999999E7;
   }

   public void deactivate() {
      this.active = false;
   }

   @Override
   public String toString() {
      return this.LimitExitAfterBars ? String.format("by Exit rule - limit to max. %d bars", this.MaxBars) : "by Exit rule";
   }

   public static Element generate(IRandomGenerator var0, ATMGenerateConfig var1) {
      Element var2 = new Element("ExitLevel");
      var2.setAttribute("key", "None");
      var2.setAttribute("use", "true");
      boolean var3 = var0.nextBool();
      int var4 = var0.nextInt(var1.noneMinBars, var1.noneMaxBars, 1);
      XMLUtil.setItemBoolParam(var2, "LimitExitAfterBars", var3);
      XMLUtil.setItemIntParam(var2, "MaxBars", var4);
      return var2;
   }
}
