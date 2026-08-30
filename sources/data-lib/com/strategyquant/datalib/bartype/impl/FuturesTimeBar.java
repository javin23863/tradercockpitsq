package com.strategyquant.datalib.bartype.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.bartype.TimeframeNotSupportedException;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.session.MonthlyRangeCalculator;
import com.strategyquant.lib.SQTime;
import java.util.Calendar;

public class FuturesTimeBar extends TimeBar {
   private final long dayPeriod;
   private final long weeklyPeriod;
   private final long monthlyPeriod;
   private long curWeekEnd;
   private long nextWeekEnd;
   private long curMonthEnd;
   private long nextMonthEnd;
   public boolean MetaTraderEngineUsed = false;
   public boolean LoadAsIs = false;

   public FuturesTimeBar(String var1) throws TimeframeNotSupportedException {
      super(2, var1);
      this.dayPeriod = this.getPeriodInSeconds("D1", false);
      this.weeklyPeriod = this.getPeriodInSeconds("Weekly", false);
      this.monthlyPeriod = this.getPeriodInSeconds("Monthly", false);
   }

   @Override
   public void processTickImplementation(TickEvent var1, BarTypeStatus var2, int var3) throws DataException {
      long var4 = var1.getTime();
      if (this.currentBarTime > Long.MIN_VALUE && var4 <= this.currentBarTime) {
         var2.status = 2;
         var2.barTime = this.currentBarTime;
      } else {
         if (this.LoadAsIs) {
            var2.convertingToHigherTF = false;
         } else if (var3 > 0 || this.MetaTraderEngineUsed) {
            var2.convertingToHigherTF = true;
         }

         if (!var2.convertingToHigherTF && var4 != this.currentBarTime) {
            var2.status = 1;
            var2.barTime = var4;
            this.currentBarTime = var4;
         } else if (var4 == var1.getSessionStartTime() && this.period != this.weeklyPeriod && this.period != this.monthlyPeriod) {
            var2.status = 0;
         } else {
            this.currentBarTime = this.getCorrectBarTime(
               var4,
               var2.convertingToHigherTF ? var1.getSessionStartTime() : this.getCorrectSessionStartTime(var1.getSessionStartTime()),
               var1.getSessionEndTime()
            );
            var2.status = 1;
            var2.barTime = this.currentBarTime;
         }
      }
   }

   long getCorrectBarTime(long var1, long var3, long var5) throws DataException {
      if (this.period == 0L) {
         return var1;
      }

      if (this.period == this.weeklyPeriod) {
         if (var1 >= this.curWeekEnd) {
            this.updateWeekStartTimes(var1, var3, var5);
         }

         return this.curWeekEnd;
      } else if (this.period == this.monthlyPeriod) {
         if (var1 >= this.curMonthEnd) {
            this.updateMonthStartTimes(var1, var3, var5);
         }

         return this.curMonthEnd;
      } else {
         int var7 = SQTime.getDayOfWeek(var3);
         int var8 = SQTime.getDayOfWeek(var5);
         return var7 == var8 && this.period <= this.dayPeriod
            ? this.getCorrectBarTimeStandard(var1, var3, var5)
            : this.getCorrectBarTimeMultiday(var1, var3, var5);
      }
   }

   private long getCorrectBarTimeStandard(long var1, long var3, long var5) throws DataException {
      long var7 = this.periodInMs;
      long var9 = var5 != Long.MIN_VALUE ? var5 : SQTime.correctDayEnd(var1);
      long var11 = SQTime.correctDayStart(var1);
      long var13 = var11;
      if (var3 != Long.MIN_VALUE) {
         var13 = var3 - var3 % this.basePeriodInMs;
      }

      long var15 = var13 + var7;
      if (var1 == var13 && var3 == Long.MIN_VALUE) {
         if (this.period < this.dayPeriod) {
            return var13;
         } else if (this.period == this.dayPeriod) {
            return var9;
         } else {
            return this.period < this.dayPeriod ? var15 : var13;
         }
      } else {
         for (int var17 = 0; var17 < 100000; var17++) {
            if (var1 > var13 && var1 <= var15) {
               return Math.min(var15, var9);
            }

            var13 += var7;
            var15 += var7;
         }

         throw new DataException(
            4, "Cannot find correct bar time for tick: " + SQTime.toDateMinuteString(var1) + " and session time:" + SQTime.toDateMinuteString(var3)
         );
      }
   }

   private long getCorrectBarTimeMultiday(long var1, long var3, long var5) throws DataException {
      long var7 = this.periodInMs;
      long var9 = var5 != Long.MIN_VALUE ? var5 : SQTime.correctDayEnd(var1) + 1L;
      long var11 = SQTime.correctDayStart(var1);
      long var13 = var11;
      if (var3 != Long.MIN_VALUE) {
         var13 = var3;
      }

      long var15 = var13 + var7;
      if (var1 <= var15) {
         return var15;
      }

      if (var1 == var13) {
         return this.period < this.dayPeriod ? var15 : var13;
      }

      for (int var17 = 0; var17 < 100000; var17++) {
         if (var1 > var13 && var1 <= var15) {
            return Math.min(var15, var9);
         }

         var13 += var7;
         var15 += var7;
      }

      throw new DataException(
         4, "Cannot find correct bar time for tick: " + SQTime.toDateMinuteString(var1) + " and session time:" + SQTime.toDateMinuteString(var3)
      );
   }

   private void updateWeekStartTimes(long var1, long var3, long var5) {
      if (var3 != Long.MIN_VALUE && var5 != Long.MIN_VALUE) {
         this.curWeekEnd = var5;
         Calendar var8 = Calendar.getInstance();
         var8.setTimeInMillis(var5);
         var8.add(5, 7);
         this.nextWeekEnd = var8.getTimeInMillis();
      } else {
         Calendar var7 = Calendar.getInstance();
         var7.setTimeInMillis(SQTime.correctDayStart(var1));
         var7.add(7, 1);

         while (var7.get(7) != 1) {
            var7.add(7, 1);
         }

         this.curWeekEnd = var7.getTimeInMillis();
         var7.add(5, 7);
         this.nextWeekEnd = var7.getTimeInMillis();
      }
   }

   private void updateMonthStartTimes(long var1, long var3, long var5) {
      if (var3 != Long.MIN_VALUE && var5 != Long.MIN_VALUE) {
         Calendar var10 = Calendar.getInstance();
         var10.setTimeInMillis(var1);
         var10.set(5, 1);
         long var8 = var10.getTimeInMillis();
         this.curMonthEnd = MonthlyRangeCalculator.getSessionEndTime(var8, var3, var5);
         var10.setTimeInMillis(var8);
         var10.add(2, 1);
         this.nextMonthEnd = MonthlyRangeCalculator.getSessionEndTime(var10.getTimeInMillis(), var3, var5);
      } else {
         Calendar var7 = Calendar.getInstance();
         var7.setTimeInMillis(SQTime.correctDayStart(var1));
         var7.add(5, 1);

         while (var7.get(5) != 1) {
            var7.add(5, 1);
         }

         this.curMonthEnd = var7.getTimeInMillis();
         var7.add(2, 1);
         this.nextMonthEnd = var7.getTimeInMillis();
      }

      this.periodInMs = this.nextMonthEnd - this.curMonthEnd;
   }

   @Override
   public BarType clone(String var1) {
      try {
         return new FuturesTimeBar(var1);
      } catch (TimeframeNotSupportedException var3) {
         Log.error("Exception during clone - it should never happen (1)", var3);
         return null;
      }
   }

   @Override
   public BarType clone() {
      try {
         return new FuturesTimeBar(this.timeframe);
      } catch (TimeframeNotSupportedException var2) {
         Log.error("Exception during clone - it should never happen (2)", var2);
         return null;
      }
   }
}
