package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class TimeDurationOther extends TimeDuration {
   public TimeDurationOther() {
      super(0);
   }

   @Override
   public String toString() {
      return L.t("Others", new Object[0]);
   }

   @Override
   public int toSeconds(int var1) {
      return Integer.MAX_VALUE;
   }
}
