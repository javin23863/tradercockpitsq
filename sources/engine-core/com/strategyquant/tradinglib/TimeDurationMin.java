package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class TimeDurationMin extends TimeDuration {
   public TimeDurationMin(int var1) {
      super(var1);
   }

   @Override
   public String toString() {
      return this.value + " " + L.t("mins", new Object[0]);
   }

   @Override
   public int toSeconds(int var1) {
      return var1 * 60;
   }
}
