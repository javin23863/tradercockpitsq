package com.strategyquant.datalib.session;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.time.SQTimeOld;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SessionElement implements Serializable {
   private int dayFrom;
   private int timeFrom;
   private int dayTo;
   private int timeTo;
   private int hourFrom;
   private int minuteFrom;
   private boolean eod;
   private int hourTo;
   private int minuteTo;
   private long computedRangeFrom;
   private long computedRangeTo;

   public SessionElement(int var1, int var2, int var3, int var4, boolean var5) {
      if (var1 == var3 && var4 == 0) {
         var3++;
      }

      var3 = var3 > 7 ? 1 : var3;
      this.dayFrom = var1;
      this.timeFrom = var2;
      this.dayTo = var3;
      this.timeTo = var4;
      this.eod = var5;
      this.computedRangeFrom = -1L;
      this.computedRangeTo = -1L;
      this.hourFrom = var2 / 100;
      this.minuteFrom = var2 % 100;
      this.hourTo = var4 / 100;
      this.minuteTo = var4 % 100;
   }

   public SessionElement(String var1, String var2, String var3, String var4, boolean var5) throws Exception {
      this(getDayInt(var1), getTimeInt(var2), getDayInt(var3), getTimeInt(var4), var5);
   }

   public int getDayFrom() {
      return this.dayFrom;
   }

   public String getDayFromStr() throws Exception {
      return getDayStr(this.dayFrom);
   }

   public int getDayTo() {
      return this.dayTo;
   }

   public String getDayToStr() throws Exception {
      return getDayStr(this.dayTo);
   }

   public int getTimeFrom() {
      return this.timeFrom;
   }

   public String getTimeFromStr() {
      return getTimeStr(this.timeFrom);
   }

   public int getTimeTo() {
      return this.timeTo;
   }

   public String getTimeToStr() {
      return getTimeStr(this.timeTo);
   }

   public boolean isEOD() {
      return this.eod;
   }

   public void setEod(boolean var1) {
      this.eod = var1;
   }

   public boolean includesDay(int var1) {
      if (this.dayFrom <= var1 && var1 <= this.dayTo) {
         return true;
      }

      if (this.dayFrom > this.dayTo) {
         if (this.dayFrom <= var1) {
            return true;
         }

         if (var1 <= this.dayTo) {
            return true;
         }
      }

      return false;
   }

   public boolean includesTime(long var1) {
      if (this.computedRangeTo == -1L || var1 > this.computedRangeTo) {
         this.computeRangesForNextPeriod(var1);
      }

      return this.computedRangeFrom <= var1 && var1 <= this.computedRangeTo;
   }

   private void computeRangesForNextPeriod(long var1) {
      int var3 = SQTime.getDayOfWeek(var1);
      int var4 = this.dayFrom - var3;
      int var5 = this.dayTo - var3;
      if (var4 > var5) {
         var4 -= 7;
      }

      if (var4 < 0 && var5 < 0) {
         var4 += 7;
         var5 += 7;
      }

      int var6 = 1900 + SQTime.getYear(var1);
      int var7 = SQTime.getMonth(var1) + 1;
      int var8 = SQTime.getDay(var1);
      boolean var9 = this.hourTo == 0 && this.minuteTo == 0;
      this.computedRangeFrom = SQTime.addDays(SQTime.toLong(var6, var7, var8, this.hourFrom, this.minuteFrom, 0), var4);
      this.computedRangeTo = SQTime.addDays(SQTime.toLong(var6, var7, var8, this.hourTo, this.minuteTo, 0), var5 + (var9 ? 1 : 0));
   }

   public long getStartTime() {
      return this.computedRangeFrom;
   }

   public long getEndTime() {
      return this.computedRangeTo;
   }

   public void clearSessionTempData() {
      this.computedRangeFrom = -1L;
      this.computedRangeTo = -1L;
   }

   private static int getDayInt(String var0) throws Exception {
      switch (var0) {
         case "Mon":
            return 1;
         case "Tue":
            return 2;
         case "Wed":
            return 3;
         case "Thu":
            return 4;
         case "Fri":
            return 5;
         case "Sat":
            return 6;
         case "Sun":
            return 7;
         default:
            throw new Exception("Day '" + var0 + "' not recognized");
      }
   }

   public static int getTimeInt(String var0) throws ParseException {
      SimpleDateFormat var1 = new SimpleDateFormat("HH:mm");
      Date var2 = var1.parse(var0);
      SQTimeOld var3 = new SQTimeOld(var2);
      return var3.getTimeAsTSTime();
   }

   private static String getDayStr(int var0) throws Exception {
      switch (var0) {
         case 1:
            return "Mon";
         case 2:
            return "Tue";
         case 3:
            return "Wed";
         case 4:
            return "Thu";
         case 5:
            return "Fri";
         case 6:
            return "Sat";
         case 7:
            return "Sun";
         default:
            throw new Exception("Day '" + var0 + "' not recognized");
      }
   }

   private static String getTimeStr(int var0) {
      int var1 = var0 / 100;
      int var2 = var0 - var1 * 100;
      long var3 = SQTime.setHour(0L, var1);
      var3 = SQTime.setMinute(var3, var2);
      return SQTime.formatTime(var3);
   }

   public SessionElement clone() {
      return new SessionElement(this.dayFrom, this.timeFrom, this.dayTo, this.timeTo, this.eod);
   }

   public void fixD1DataTime(VersatileData var1) {
      var1.time = this.computedRangeTo;
   }

   public void fixD1DataTime(TickEvent var1) {
      var1.setTime(this.computedRangeTo);
   }
}
