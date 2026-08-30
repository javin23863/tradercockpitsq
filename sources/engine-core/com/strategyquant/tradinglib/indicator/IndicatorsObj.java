package com.strategyquant.tradinglib.indicator;

import com.strategyquant.tradinglib.debug.Debugger;

public class IndicatorsObj extends Debugger {
   public int Engine;
   protected IndicatorsCache indicatorsCache = new IndicatorsCache();

   public static IndicatorsObj newInstance() {
      return new IndicatorsObj();
   }

   public void destroy() {
      if (this.indicatorsCache != null) {
         this.indicatorsCache.destroy();
         this.indicatorsCache = null;
      }
   }

   public IndicatorsCache getIndicatorsCache() {
      return this.indicatorsCache;
   }
}
