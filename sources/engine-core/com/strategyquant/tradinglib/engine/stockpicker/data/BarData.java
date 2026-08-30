package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.lib.SQTime;

public class BarData {
   public long time;
   public float open;
   public float high;
   public float low;
   public float close;
   public float volume;
   public double value;

   @Override
   public String toString() {
      return String.format("%s - o=%s,h=%s,l=%s,c=%s", SQTime.toDateString(this.time), this.open, this.high, this.low, this.close);
   }
}
