package com.strategyquant.tradinglib.quality;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.gridlib.compute.common.RingBuffer;
import com.strategyquant.lib.SQTime;

public class DataProblemEvaluator {
   private static final int AVERAGE_COUNT = 30;
   private static final double SPIKE_CHECK_MULTIPLICATOR = 5.0;
   private long lastTime = -1L;
   private int lastDayOfWeek = -1;
   private int minHour;
   private int maxHour;
   private long milisForTimeframe;
   private long beginGap = -1L;
   private long endGap = -1L;
   private long storedCount = 0L;
   private RingBuffer<Double> ringBuffer = new RingBuffer(30);
   private double lastOpen;

   public DataProblemEvaluator.ProblemKind evalProblemKind(VersatileData var1, boolean var2) {
      int var3 = SQTime.getDayOfWeek(var1.time);

      try {
         if (!var2 && !this.checkGap(var1.time, var3)) {
            return DataProblemEvaluator.ProblemKind.GAP;
         } else if (!this.checkLow(var1)) {
            return DataProblemEvaluator.ProblemKind.LOW_PROBLEM;
         } else if (!this.checkHigh(var1)) {
            return DataProblemEvaluator.ProblemKind.HIGH_PROBLEM;
         } else {
            return !this.checkOpenSpike(var1) ? DataProblemEvaluator.ProblemKind.SPIKE : null;
         }
      } finally {
         this.storeLast(var1, var3);
      }
   }

   public long getMilisForTimeframe() {
      return this.milisForTimeframe;
   }

   public long getBeginGap() {
      return this.beginGap;
   }

   public long getEndGap() {
      return this.endGap;
   }

   private boolean checkGap(long var1, int var3) {
      if (this.lastTime == -1L || var3 == 7) {
         return true;
      }

      if (var3 != this.lastDayOfWeek) {
         if (this.lastDayOfWeek != -1 && this.lastDayOfWeek != 7 && SQTime.getHour(this.lastTime) < this.maxHour) {
            this.beginGap = this.lastTime + this.milisForTimeframe;
            this.endGap = SQTime.setHour(this.lastTime, this.maxHour);
            return false;
         } else {
            int var6 = SQTime.getHour(var1);
            if (var6 != this.minHour) {
               this.beginGap = SQTime.setHour(var1, this.minHour);
               this.endGap = var1 - this.milisForTimeframe;
               return false;
            } else {
               return true;
            }
         }
      } else {
         long var4 = var1 - this.lastTime;
         if (var4 > this.milisForTimeframe) {
            this.beginGap = this.lastTime + this.milisForTimeframe;
            this.endGap = var1 - this.milisForTimeframe;
            return false;
         } else {
            return true;
         }
      }
   }

   private boolean checkOpenSpike(VersatileData var1) {
      if (this.storedCount < 30L) {
         return true;
      }

      double var2 = 0.0;

      for (Double var5 : this.ringBuffer) {
         var2 += var5;
      }

      double var11 = var2 / 30.0;
      double var6 = 5.0 * var11;
      double var8 = Math.abs(var1.high - var1.low);
      return var8 < var6;
   }

   private void storeLast(VersatileData var1, int var2) {
      this.lastTime = var1.time;
      this.lastOpen = var1.open;
      this.lastDayOfWeek = var2;
      this.ringBuffer.add(Math.abs(var1.high - var1.low));
      this.storedCount++;
   }

   private boolean checkHigh(VersatileData var1) {
      return !(var1.high < var1.low) && !(var1.high < var1.open) && !(var1.high < var1.close);
   }

   private boolean checkLow(VersatileData var1) {
      return !(var1.low > var1.high) && !(var1.low > var1.open) && !(var1.low > var1.close);
   }

   public void setMinMaxHour(int var1, int var2) {
      this.minHour = var1;
      this.maxHour = var2;
   }

   public void setTimeframe(String var1) throws TradingException {
      this.milisForTimeframe = TimeframeManager.getMillis(var1);
      if (this.milisForTimeframe == 0L) {
         throw new IllegalArgumentException("Not supported timeframe:" + var1);
      }
   }

   public enum ProblemKind {
      GAP("gap"),
      LOW_PROBLEM("bad OHLC - low value"),
      HIGH_PROBLEM("bad OHLC - high value"),
      SPIKE("Spike - this candle is more than 5x bigger than average");

      private String desc;

      ProblemKind(String nullxx) {
         this.desc = nullxx;
      }

      public String getDesc() {
         return this.desc;
      }
   }
}
