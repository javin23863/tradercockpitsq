package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class TimeDurationHour extends TimeDuration {
   public TimeDurationHour(int var1) {
      super(var1);
   }

   @Override
   public String toString() {
      return this.value + " " + L.t("hours", new Object[0]);
   }

   @Override
   public int toSeconds(int var1) {
      return var1 * 3600;
   }
}
