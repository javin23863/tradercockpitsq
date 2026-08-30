package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.atm.exits.helpers.TrailingFormula;
import com.strategyquant.tradinglib.exit.TrailingStopHandler;
import org.jdom2.Element;

public class TrailingStop extends AbstractExitLevel {
   private int Type;
   private int FixedPips;
   private double AtrMultiplicator;
   private int AtrPeriod;
   public IFormula TrailingStop;
   private double trailingSL = -9.9999999E7;
   private boolean debugTrailingStops = false;

   public TrailingStop(Element var1, int var2) throws Exception {
      super(var1, var2);
      this.Type = XMLUtil.getItemIntParam(var1, "Type", 1);
      this.FixedPips = XMLUtil.getItemIntParam(var1, "FixedPips", 50);
      this.AtrMultiplicator = XMLUtil.getItemDoubleParam(var1, "AtrMultiplicator", 1.2);
      this.AtrPeriod = XMLUtil.getItemIntParam(var1, "AtrPeriod", 20);
      this.TrailingStop = new TrailingFormula(this.Type, this.FixedPips, this.AtrMultiplicator, this.AtrPeriod);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("ExitLevel");
      var1.setAttribute("key", "TrailingStop");
      XMLUtil.setItemIntParam(var1, "Type", this.Type);
      XMLUtil.setItemIntParam(var1, "FixedPips", this.FixedPips);
      XMLUtil.setItemDoubleParam(var1, "AtrMultiplicator", this.AtrMultiplicator);
      XMLUtil.setItemIntParam(var1, "AtrPeriod", this.AtrPeriod);
      return var1;
   }

   @Override
   public void setForOrder(final ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException {
      if (var1.isFilled()) {
         this.setStrategy(var2);
         if (this.TrailingStop != null && !this.TrailingStop.isNoneValue()) {
            this.trailingSL = -9.9999999E7;
            var1.registerEvent(
               2,
               new IActionEventListener() {
                  @Override
                  public void OnActionEvent(StrategyBase var1x) throws TradingException {
                     TrailingStop.this.trailingSL = TrailingStopHandler.checkTrailingStop(
                        var1,
                        TrailingStop.this.TrailingStop,
                        null,
                        var1x,
                        TrailingStop.this.trailingSL,
                        TrailingStop.this.debugTrailingStops,
                        (byte)TrailingStop.this.exitIndex
                     );
                  }
               }
            );
         }
      }
   }

   @Override
   public double getNettingPrice(StrategyBase var1, ILiveOrder var2, double var3, double var5) throws TradingException {
      return TrailingStopHandler.calculatePrice(var2, this.TrailingStop, null, var1);
   }

   @Override
   public String toString() {
      return this.Type == 1
         ? String.format("at trailing stop %d pips", this.FixedPips)
         : String.format("at trailing stop %s * ATR(%d)", this.AtrMultiplicator, this.AtrPeriod);
   }

   public static Element generate(IRandomGenerator var0, ATMGenerateConfig var1) {
      Element var2 = new Element("ExitLevel");
      var2.setAttribute("key", "TrailingStop");
      var2.setAttribute("use", "true");
      int var4 = 50;
      double var5 = 1.2;
      int var7 = 20;
      byte var3;
      if (var0.nextBool()) {
         var3 = 1;
         var4 = var0.nextInt(var1.tsFixedPipsMin, var1.tsFixedPipsMax, 1);
      } else {
         var3 = 2;
         var5 = var0.nextDouble(var1.tsATRMutliplierMin, var1.tsATRMutliplierMax, 0.05, 2);
         var7 = var0.nextInt(var1.tsATRPeriodMin, var1.tsATRPeriodMax, 1);
      }

      XMLUtil.setItemIntParam(var2, "Type", var3);
      XMLUtil.setItemIntParam(var2, "FixedPips", var4);
      XMLUtil.setItemDoubleParam(var2, "AtrMultiplicator", var5);
      XMLUtil.setItemIntParam(var2, "AtrPeriod", var7);
      return var2;
   }
}
