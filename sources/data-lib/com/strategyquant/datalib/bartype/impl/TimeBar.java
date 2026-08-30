package com.strategyquant.datalib.bartype.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.bartype.TimeframeNotSupportedException;
import com.strategyquant.datalib.data.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeBar extends BarType {
   public static final Logger Log = LoggerFactory.getLogger("TimeBar");
   protected final long period;
   protected long periodInMs;
   protected int basePeriodInMs;
   protected long currentBarTime = Long.MIN_VALUE;

   public TimeBar(String var1) throws TimeframeNotSupportedException {
      super(2, var1);
      this.period = this.getPeriodInSeconds(var1, true);
      this.periodInMs = this.period * 1000L;
   }

   TimeBar(int var1, String var2) throws TimeframeNotSupportedException {
      super(var1, var2);
      this.period = this.getPeriodInSeconds(var2, true);
      this.periodInMs = this.period * 1000L;
   }

   public long getPeriodInSeconds(String var1, boolean var2) throws TimeframeNotSupportedException {
      if (var1.equals("TICK")) {
         return 0L;
      }

      if (var1.equals("Weekly")) {
         return 604800L;
      }

      if (var1.equals("Monthly")) {
         return 2419200L;
      }

      String var3 = var1.substring(0, 1);
      String var4 = var1.substring(1, var1.length());
      if (!var3.equals("S") && !var3.equals("M") && !var3.equals("H") && !var3.equals("D")) {
         throw new TimeframeNotSupportedException("Timeframe '" + var1 + "' is not supported by this Bar Type!");
      }

      try {
         long var5 = Integer.parseInt(var4);
         if (var3.equals("S")) {
            if (var2) {
               this.basePeriodInMs = 1000;
            }

            return var5;
         } else if (var3.equals("M")) {
            if (var2) {
               this.basePeriodInMs = 60000;
            }

            return var5 * 60L;
         } else if (var3.equals("H")) {
            if (var2) {
               this.basePeriodInMs = 3600000;
            }

            return var5 * 3600L;
         } else if (var3.equals("D")) {
            if (var2) {
               this.basePeriodInMs = 86400000;
            }

            return var5 * 86400L;
         } else {
            throw new TimeframeNotSupportedException("Timeframe '" + var1 + "' is not supported by this Bar Type!");
         }
      } catch (NumberFormatException var7) {
         throw new TimeframeNotSupportedException("Timeframe '" + var1 + "' is not supported by this Bar Type!");
      }
   }

   @Override
   public void processTickImplementation(TickEvent var1, BarTypeStatus var2, int var3) throws DataException {
      long var4 = var1.getTime();
      var2.barTime = var4;
      if (var1.isBarOpen()) {
         var2.status = 1;
      } else {
         var2.status = 2;
      }
   }

   @Override
   public BarType clone(String var1) {
      try {
         return new TimeBar(var1);
      } catch (TimeframeNotSupportedException var3) {
         Log.error("Exception during clone - it should never happen (1)", var3);
         return null;
      }
   }

   @Override
   public BarType clone() {
      try {
         return new TimeBar(this.timeframe);
      } catch (TimeframeNotSupportedException var2) {
         Log.error("Exception during clone - it should never happen (2)", var2);
         return null;
      }
   }

   @Override
   public boolean isTickBar() {
      return this.period == 0L;
   }

   @Override
   public String checkCanBeComputedFrom(String var1) throws DataException {
      if (this.timeframe.equals(var1)) {
         return null;
      }

      BarType var2 = BarTypeFactory.getBarType(var1, this.getBarTimeType());
      if (var2.isTickBar()) {
         return null;
      }

      if (!(var2 instanceof TimeBar)) {
         return null;
      }

      TimeBar var3 = (TimeBar)var2;
      long var4 = var3.getPeriod();
      return this.period >= var4 && (this.period % var4 <= 0L || this.period % 86400L <= 0L)
         ? null
         : "Timeframe '" + this.toString(this.timeframe) + "' cannot be computed from timeframe '" + var2.toString(var1) + "' of the stored data !";
   }

   public long getPeriod() {
      return this.period;
   }

   @Override
   public String getBaseTF() {
      return "M1";
   }

   @Override
   public String getTickTF() {
      return "TICK";
   }

   @Override
   public long estimateStartDate(int var1, long var2) {
      return this.period == 0L ? 0L : var2 - var1 * this.period * 1000L;
   }

   @Override
   public boolean checkTimeframeIsSupported(String var1) {
      var1 = var1.toUpperCase();
      if (var1.equals("TICK")) {
         return true;
      }

      if (var1.equals("Weekly".toUpperCase())) {
         return true;
      }

      if (var1.equals("Monthly".toUpperCase())) {
         return true;
      }

      String var2 = var1.substring(0, 1);
      String var3 = var1.substring(1, var1.length());
      if (!var2.equals("S") && !var2.equals("M") && !var2.equals("H") && !var2.equals("D")) {
         return false;
      }

      try {
         int var4 = Integer.parseInt(var3);
         return true;
      } catch (NumberFormatException var5) {
         return false;
      }
   }

   protected long getCorrectSessionStartTime(long var1) {
      if (var1 == Long.MIN_VALUE) {
         return var1;
      }

      long var3 = this.periodInMs;
      return this.periodInMs > 0L ? var1 / var3 * var3 : var1;
   }

   @Override
   public int getPeriodInMS() {
      return this.basePeriodInMs;
   }
}
