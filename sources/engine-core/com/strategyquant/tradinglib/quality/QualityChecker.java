package com.strategyquant.tradinglib.quality;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.lib.utils.JsonCreator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class QualityChecker {
   private static final int TIME_CHECK_PERIOD = 20;
   private static final DateTimeFormatter formaterDateMinute = DateTimeFormat.forPattern("yyyy.MM.dd HH:mm");
   private DataProblemEvaluator evaluator = new DataProblemEvaluator();
   private int minHour;
   private int maxHour;

   private void analyseDataKind(IDataLoader var1) throws Exception {
      int var2 = 0;
      int var3 = -1;
      int var4 = -1;
      int var5 = -1;
      long var6 = -1L;

      while (var1.hasNextTick() && var2 < 20) {
         VersatileData var8 = new VersatileData();
         var1.getNextTick(var8);
         int var9 = SQTime.getDay(var8.time);
         if (var9 != var3) {
            var2++;
            int var10 = SQTime.getHour(var8.time);
            if (var4 == -1 || var4 > var10) {
               var4 = var10;
            }

            if (var6 != -1L) {
               int var11 = SQTime.getHour(var6);
               if (var5 == -1 || var5 < var11) {
                  var5 = var11;
               }
            }

            var3 = var9;
         }

         var6 = var8.time;
      }

      var1.seek(0);
      this.minHour = var4;
      this.maxHour = var5;
      this.evaluator.setMinMaxHour(var4, var5);
   }

   private void writeEndYear(JsonCreator var1, int var2, long var3) {
      if (var2 != -1) {
         var1.endArray(true);
         var1.put("end", formaterDateMinute.print(var3), false);
         var1.endObject(false);
      }
   }

   private void writeProblem(JsonCreator var1, long var2, long var4, boolean var6) {
      if (var2 != -1L) {
         if (!var6) {
            var1.separator();
         }

         var1.beginObject();
         var1.put("from", formaterDateMinute.print(var2), true);
         var1.put("to", formaterDateMinute.print(var4), false);
         var1.endObject(false);
      }
   }

   public String getSummary(String var1, String var2, String var3) throws Exception {
      this.evaluator.setTimeframe(var1);
      long var4 = TimeframeManager.getMillis(var1) - 1L;
      IDataLoader var6 = this.getLoader(var2, var1, var3);
      this.analyseDataKind(var6);
      DataInfo var7 = DataManager.getDataInfo("History", var2);
      int var8 = var7.decimals;
      JsonCreator var9 = new JsonCreator();
      var9.beginObject();
      int var10 = 0;
      int var11 = -1;
      long var12 = -1L;
      long var14 = -1L;
      var9.putBeginArray("problems");
      long var16 = 0L;
      long var18 = 0L;
      long var20 = 0L;
      long var22 = 0L;
      boolean var24 = false;

      while (var6.hasNextTick()) {
         var22++;
         VersatileData var25 = new VersatileData();
         var6.getNextTick(var25);
         var25.open = SQUtils.round(var25.open, var8);
         var25.high = SQUtils.round(var25.high, var8);
         var25.low = SQUtils.round(var25.low, var8);
         var25.close = SQUtils.round(var25.close, var8);
         int var26 = 1900 + SQTime.getYear(var25.time);
         if (var26 != var11) {
            this.writeProblem(var9, var14, var12 + var4, var24);
            this.writeEndYear(var9, var11, var12);
            if (var11 != -1) {
               var9.separator();
            }

            var9.beginObject();
            var9.put("year", var26, true);
            var9.put("start", formaterDateMinute.print(var25.time), true);
            var9.putBeginArray("problems");
            var11 = var26;
            var14 = -1L;
            var24 = true;
         }

         DataProblemEvaluator.ProblemKind var27 = this.evaluator.evalProblemKind(var25, false);
         if (var27 == DataProblemEvaluator.ProblemKind.GAP) {
            long var28 = this.evaluator.getBeginGap();
            long var30 = this.evaluator.getEndGap();
            var16++;
            var10++;
            if (var14 == -1L) {
               var14 = var28;
            }

            var12 = var30;
            var27 = this.evaluator.evalProblemKind(var25, true);
         }

         if (var27 != null) {
            var10++;
            if (var14 == -1L) {
               var14 = var25.time;
            }

            switch (var27) {
               case SPIKE:
                  var18++;
                  break;
               case HIGH_PROBLEM:
               case LOW_PROBLEM:
                  var20++;
            }
         } else if (var14 != -1L) {
            this.writeProblem(var9, var14, var12 + var4, var24);
            var24 = false;
            var14 = -1L;
         }

         var12 = var25.time;
      }

      this.writeProblem(var9, var14, var12 + var4, var24);
      this.writeEndYear(var9, var11, var12 + var4);
      var9.endArray(true);
      var9.put("pointOpacity", this.getPointOpacity(var1), true);
      var9.put("minHour", this.minHour, true);
      var9.put("maxHour", this.maxHour, true);
      var9.put("totalErrors", var10, true);
      var9.putBeginObject("gap");
      var9.put("count", var16, true);
      var9.put("percent", this.getPercent(var16, var22), false);
      var9.endObject(true);
      var9.putBeginObject("ohlc");
      var9.put("count", var20, true);
      var9.put("percent", this.getPercent(var20, var22), false);
      var9.endObject(true);
      var9.putBeginObject("spike");
      var9.put("count", var18, true);
      var9.put("percent", this.getPercent(var18, var22), false);
      var9.endObject(false);
      var9.endObject(false);
      var6.close();
      return var9.toJson();
   }

   private String getPointOpacity(String var1) {
      return "ff";
   }

   private double getPercent(long var1, long var3) {
      double var5 = var1 * 100.0 / var3;
      int var7 = (int)(var5 * 1000.0);
      return var7 / 1000.0;
   }

   private IDataLoader getLoader(String var1, String var2, String var3) throws Exception {
      DataInfo var4 = DataManager.getDataInfo("History", var1);
      int var5 = var4.sourceDataId;
      if (var5 != 0) {
         DataInfo var6 = DataManager.getDataInfo("History", var5);
         if (var6 != null) {
            var1 = var6.symbol;
         }
      }

      ChartDef var8 = new ChartDef("History", var1, var2, 0L, SQTimeOld.toLong(2100, 1, 1), 2.5, var3);
      IDataLoader var7 = DataManager.getDataLoader(var8, 1);
      var7.open();
      return var7;
   }

   private String getTimeframe(String var1) {
      DataInfo var2 = DataManager.getDataInfo("History", var1);
      String var3 = var2.timeframe;
      String var4 = var3;
      if (var4 == "TICK") {
         var4 = "M1";
      }

      return var4;
   }

   public String getDetails(String var1, String var2, String var3, int var4, int var5) throws Exception {
      JsonCreator var6 = new JsonCreator();
      var6.beginArray();
      this.evaluator.setTimeframe(var1);
      IDataLoader var7 = this.getLoader(var2, var1, var3);
      this.analyseDataKind(var7);
      DataInfo var8 = DataManager.getDataInfo("History", var2);
      int var9 = var8.decimals;
      int var10 = 0;
      int var11 = 0;

      while (var7.hasNextTick()) {
         VersatileData var12 = new VersatileData();
         var7.getNextTick(var12);
         var12.open = SQUtils.round(var12.open, var9);
         var12.high = SQUtils.round(var12.high, var9);
         var12.low = SQUtils.round(var12.low, var9);
         var12.close = SQUtils.round(var12.close, var9);
         DataProblemEvaluator.ProblemKind var13 = this.evaluator.evalProblemKind(var12, false);
         if (var13 == DataProblemEvaluator.ProblemKind.GAP) {
            long var14 = this.evaluator.getBeginGap();
            long var16 = this.evaluator.getEndGap();
            long var18 = this.evaluator.getMilisForTimeframe();

            while (var14 <= var16) {
               if (var10 >= var4 && var10 < var5) {
                  if (var11 != 0) {
                     var6.separator();
                  }

                  var6.beginObject();
                  var6.put("data");
                  var6.beginArray();
                  var6.setValue(SQTime.toFullDateMinuteString(var14), true);
                  var6.setValue("missing", true);
                  var6.setValue("missing", true);
                  var6.setValue("missing", true);
                  var6.setValue("missing", true);
                  var6.setValue("missing", true);
                  var6.setValue(var13.getDesc(), false);
                  var6.endArray(true);
                  var6.put("id", "r" + var10, false);
                  var6.endObject(false);
                  var11++;
               }

               var14 += var18;
               var10++;
            }

            var13 = this.evaluator.evalProblemKind(var12, true);
         }

         if (var13 != null) {
            if (var10 >= var4 && var10 < var5) {
               if (var11 != 0) {
                  var6.separator();
               }

               var6.beginObject();
               var6.put("data");
               var6.beginArray();
               var6.setValue(SQTime.toFullDateMinuteString(var12.time), true);
               var6.setValue(var12.open, true);
               var6.setValue(var12.high, true);
               var6.setValue(var12.low, true);
               var6.setValue(var12.close, true);
               var6.setValue(var12.volume, true);
               var6.setValue(var13.getDesc(), false);
               var6.endArray(true);
               var6.put("id", "r" + var10, false);
               var6.endObject(false);
               var11++;
            }

            var10++;
         }
      }

      var7.close();
      var6.endArray(false);
      return var6.toJson();
   }
}
