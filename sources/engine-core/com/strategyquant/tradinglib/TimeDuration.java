package com.strategyquant.tradinglib;

public abstract class TimeDuration {
   public int seconds;
   public int value;

   public TimeDuration(int var1) {
      this.value = var1;
      this.seconds = this.toSeconds(var1);
   }

   @Override
   public abstract String toString();

   public abstract int toSeconds(int var1);
}
