package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class TimeDurationDay extends TimeDuration {
   public TimeDurationDay(int var1) {
      super(var1);
   }

   @Override
   public String toString() {
      return this.value + " " + L.t("days", new Object[0]);
   }

   @Override
   public int toSeconds(int var1) {
      return var1 * 3600 * 24;
   }
}
