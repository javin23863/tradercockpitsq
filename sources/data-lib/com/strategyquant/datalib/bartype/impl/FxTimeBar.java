package com.strategyquant.datalib.bartype.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.bartype.TimeframeNotSupportedException;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.lib.SQTime;
import java.util.Calendar;

public class FxTimeBar extends TimeBar {
   private final long weeklyPeriod = this.getPeriodInSeconds("Weekly", false);
   private final long monthlyPeriod = this.getPeriodInSeconds("Monthly", false);
   private long curWeekStart;
   private long nextWeekStart;
   private long curMonthStart;
   private long nextMonthStart;

   public FxTimeBar(String var1) throws TimeframeNotSupportedException {
      super(1, var1);
   }

   @Override
   public void processTickImplementation(TickEvent var1, BarTypeStatus var2, int var3) throws DataException {
      long var4 = var1.getTime();
      long var6 = var4 - this.currentBarTime;
      if (this.currentBarTime > Long.MIN_VALUE && var6 < this.periodInMs) {
         if (this.period >= this.weeklyPeriod) {
            var2.status = 2;
            var2.barTime = this.currentBarTime;
            return;
         }

         long var8 = SQTime.getDateInMs(var4);
         long var10 = SQTime.getDateInMs(this.currentBarTime);
         if (var8 == var10) {
            var2.status = 2;
            var2.barTime = this.currentBarTime;
            return;
         }
      }

      this.currentBarTime = this.getCorrectBarTime(
         var4, var2.convertingToHigherTF ? var1.getSessionStartTime() : this.getCorrectSessionStartTime(var1.getSessionStartTime())
      );
      var2.status = 1;
      var2.barTime = this.currentBarTime;
   }

   long getCorrectBarTime(long var1, long var3) throws DataException {
      if (this.period == 0L) {
         return var1;
      }

      if (this.period == this.weeklyPeriod) {
         if (var1 >= this.nextWeekStart) {
            this.updateWeekStartTimes(var1);
         }

         return this.curWeekStart;
      } else if (this.period == this.monthlyPeriod) {
         if (var1 >= this.nextMonthStart) {
            this.updateMonthStartTimes(var1);
         }

         return this.curMonthStart;
      } else {
         long var5 = var3 == Long.MIN_VALUE ? SQTime.correctDayStart(var1) : var3;
         if (var1 == var5) {
            return var5;
         }

         long var7 = this.periodInMs;
         long var9 = var5 + var7;
         long var11 = SQTime.correctDayEnd(var1) + 1L;

         for (int var13 = 0; var13 < 100000; var13++) {
            if (var1 >= var5 && var1 < var9) {
               return Math.min(var5, var11);
            }

            var5 += var7;
            var9 = var5 + var7;
         }

         throw new DataException(
            4, "Cannot find correct bar time for tick: " + SQTime.toDateMinuteString(var1) + " and session time:" + SQTime.toDateMinuteString(var3)
         );
      }
   }

   private void updateWeekStartTimes(long var1) {
      Calendar var3 = Calendar.getInstance();
      var3.setTimeInMillis(SQTime.correctDayStart(var1));

      while (var3.get(7) != 1) {
         var3.add(7, -1);
      }

      this.curWeekStart = var3.getTimeInMillis();
      var3.add(5, 7);
      this.nextWeekStart = var3.getTimeInMillis();
   }

   private void updateMonthStartTimes(long var1) {
      Calendar var3 = Calendar.getInstance();
      var3.setTimeInMillis(SQTime.correctDayStart(var1));

      while (var3.get(5) != 1) {
         var3.add(5, -1);
      }

      this.curMonthStart = var3.getTimeInMillis();
      var3.add(2, 1);
      this.nextMonthStart = var3.getTimeInMillis();
      this.periodInMs = this.nextMonthStart - this.curMonthStart;
   }

   @Override
   public BarType clone(String var1) {
      try {
         return new FxTimeBar(var1);
      } catch (TimeframeNotSupportedException var3) {
         Log.error("Exception during clone - it should never happen (1)", var3);
         return null;
      }
   }

   @Override
   public BarType clone() {
      try {
         return new FxTimeBar(this.timeframe);
      } catch (TimeframeNotSupportedException var2) {
         Log.error("Exception during clone - it should never happen (2)", var2);
         return null;
      }
   }
}
