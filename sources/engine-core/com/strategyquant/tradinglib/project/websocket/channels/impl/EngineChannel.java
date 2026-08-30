package com.strategyquant.tradinglib.project.websocket.channels.impl;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.project.ComplexDurationStats;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.IProjectChannel;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineChannel implements IProjectChannel {
   private static final Logger Log = LoggerFactory.getLogger(EngineChannel.class);
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();

   @Override
   public Object getData(SQProject var1) {
      JSONObject var2 = var1.getInfo();
      ProjectRunInfo var3 = var1.getTrackingInfo();

      try {
         var2.put("backtestsPerformed", var3.backtestsPerformed);
         String var4 = SQUtils.d2(100.0 * var3.strategiesAcceptedPct);
         String var5 = SQUtils.d2(100.0 * var3.strategiesRejectedPct);
         if (var3.strategiesAccepted > 0L && var4.equals("0.00")) {
            var4 = "0.01";
            var5 = "99.99";
         }

         var2.put("strategiesRejected", var3.strategiesRejected);
         var2.put("strategiesRejectedPct", var5);
         var2.put("strategiesAccepted", var3.strategiesAccepted);
         var2.put("strategiesAcceptedPct", var4);
         var2.put("strategiesPassed", var3.strategiesPassed);
         var2.put("strategiesFailed", var3.strategiesFailed);
         var2.put("jobExceptionsCount", var3.jobExceptionsCount);
         var2.put("strategiesPerHour", this.formatIntOrDouble(var3.strategiesPerHour));
         var2.put("acceptedStrategiesPerHour", this.formatIntOrDouble(var3.acceptedStrategiesPerHour));
         var2.put("step", var3.step);
         var2.put("totalJobsDone", var3.totalJobsDone);
         var2.put("totalSteps", var3.totalSteps);
         var2.put("timePerStrategy", SQUtils.formatDuration(var3.timePerStrategy));
         var2.put("timePerAcceptedStrategy", SQUtils.formatDuration(var3.timePerAcceptedStrategy));
         var2.put("totalRunningTime", SQUtils.formatDuration(var3.totalProjectRunningTime));
         var2.put("taskRunningTime", SQUtils.formatDuration(var3.taskRunningTime));
         var2.put("timeToFinish", SQUtils.formatDuration(var3.timeToFinish));
         var2.put("infiniteProgress", var3.infiniteProgress);
         var2.put("progressPercent", (int)var3.progressPercent);
         var2.put("error", var3.error);
         var2.put("errorType", var3.errorType);
         var2.put("errorInfo", var3.errorInfo);

         try {
            var2.put("acceptedStats", this.acceptedStats(var1));
         } catch (Exception var14) {
            Log.error("Failed to get acceptedStats. Exc.", var14);
         }

         try {
            var2.put("dismissalStats", this.dismissalStats(var1));
         } catch (Exception var13) {
            Log.error("Failed to get dismissalStats. Exc.", var13);
         }

         try {
            var2.put("durationStats", this.durationStats(var1));
         } catch (Exception var12) {
            Log.error("Failed to get durationStats. Exc.", var12);
         }

         var2.put("runningStatus", var1.getRunningStatus());
         var2.put("lastEvent", var1.getLastEvent());
         return var2;
      } finally {
         var3.error = null;
      }
   }

   private String formatIntOrDouble(double var1) {
      return var1 > 1000.0 ? String.format("%.0f", var1) : String.format("%.2f", var1);
   }

   private String durationStats(SQProject var1) {
      JsonCreator var2 = new JsonCreator();
      var2.beginArray();
      HashMap var3 = this.projectRunInfo.getDurationStats();
      int var4 = 0;
      double var5 = 0.0;

      for (String var8 : var3.keySet()) {
         var5 += ((ComplexDurationStats)var3.get(var8)).totalTime;
      }

      for (String var13 : var3.keySet()) {
         if (var4 != 0) {
            var2.separator();
         }

         ComplexDurationStats var9 = (ComplexDurationStats)var3.get(var13);
         var2.beginArray();
         var2.setValue(var13, true);
         var2.setValue(var9.count, true);
         var2.setValue(SQUtils.d2String(var9.avgTime, 4) + " s.", true);
         var2.setValue(SQUtils.d2String(var9.totalTime, 4) + " s.", true);
         double var10 = SQUtils.round2(100.0 * (var9.totalTime / var5));
         var2.setValue(SQUtils.d2(var10) + " %", false);
         var2.endArray(false);
         var4++;
      }

      var2.endArray(false);
      return var2.toJson();
   }

   private String acceptedStats(SQProject var1) {
      JsonCreator var2 = new JsonCreator();
      var2.beginArray();
      var1.loadTrackingInfo(this.projectRunInfo, true);
      Int2IntOpenHashMap var3 = this.projectRunInfo.getAcceptedCounts();
      int var4 = 0;
      IntSet var6 = var3.keySet();

      for (IntIterator var7 = var6.iterator(); var7.hasNext(); var4++) {
         int var8 = (Integer)var7.next();
         String var5;
         if (var8 == 0) {
            var5 = "In databank";
         } else if (var8 == 10007) {
            var5 = BadStrategyException.TxtLongReplacedWithBetterStrategy;
         } else if (var8 == 10006) {
            var5 = BadStrategyException.TxtLongReasonStrategyTooSimilar;
         } else {
            var5 = BadStrategyException.ExplanationUnknown + ", code: " + var8;
         }

         int var9 = var3.get(var8);
         double var10 = SQUtils.round2(100.0 * ((double)var9 / this.projectRunInfo.strategiesAccepted));
         if (var4 != 0) {
            var2.separator();
         }

         var2.beginArray();
         var2.setValue(var5, true);
         var2.setValue(var9, true);
         var2.setValue(var10 + " %", false);
         var2.endArray(false);
      }

      var2.endArray(false);
      return var2.toJson();
   }

   private String dismissalStats(SQProject var1) {
      JsonCreator var2 = new JsonCreator();
      var2.beginArray();
      var1.loadTrackingInfo(this.projectRunInfo, true);
      Int2IntOpenHashMap var3 = this.projectRunInfo.getDismissalCounts();
      Int2ObjectOpenHashMap var4 = this.projectRunInfo.getDismissalReasons();
      int var5 = 0;
      IntSet var7 = var3.keySet();

      for (IntIterator var8 = var7.iterator(); var8.hasNext(); var5++) {
         int var9 = (Integer)var8.next();
         String var6;
         if (var4.containsKey(var9)) {
            var6 = (String)var4.get(var9);
         } else {
            if (var9 < 100000) {
               var6 = BadStrategyException.getReasonAsString(var9);
            } else if (var9 >= 100000 && var9 < 200000) {
               try {
                  ISQTask var26 = var1.getLastRunningTask();
                  TaskSettingsData var29 = var26.getSettingsData();
                  int var32 = var9 - 100000;
                  var6 = L.t("Initial population filter", new Object[0]) + ": ";
                  ArrayList var34 = (ArrayList)var29.getParams().get("InitialGenerationConditions");
                  Condition var36 = (Condition)var34.get(var32);
                  var6 = var6 + var36.toString();
               } catch (Exception var20) {
                  Log.debug("Cannot print initial condition, code: " + var9 + ". Exc.", var20);
                  var6 = L.t("initial condition, code: %s", new Object[]{var9});
               }
            } else if (var9 >= 200000 && var9 < 300000) {
               try {
                  ISQTask var25 = var1.getLastRunningTask();
                  TaskSettingsData var28 = var25.getSettingsData();
                  int var31 = var9 - 200000;
                  var6 = L.t("Global filter", new Object[0]) + ":  ";
                  ArrayList var33 = (ArrayList)var28.getParams().get("Conditions");
                  Condition var35 = (Condition)var33.get(var31);
                  var6 = var6 + var35.toString();
               } catch (Exception var19) {
                  Log.debug("Cannot print global condition, code: " + var9 + ". Exc.", var19);
                  var6 = L.t("global condition, code: %s", new Object[]{var9});
               }
            } else if (var9 >= 300000) {
               try {
                  ISQTask var10 = var1.getLastRunningTask();
                  TaskSettingsData var11 = var10.getSettingsData();
                  int var12 = var9 - 300000;
                  int var13 = var12 / 1000;
                  int var14 = var12 - var13 * 1000;
                  ArrayList var15 = (ArrayList)var11.getParams().get("CrossChecks");
                  ICrossCheck var16 = (ICrossCheck)var15.get(var13);
                  var6 = L.t("Cross Check filter in '%s'", new Object[]{var16.getName()}) + ":  ";
                  if (WalkForwardCrossCheckMethod.class.isAssignableFrom(var16.getClass()) && !var16.getSettingName().equals("OptProfileSysParamPermutation")) {
                     var6 = var6 + L.t("Robustness score didn't pass.", new Object[0]);
                  } else {
                     ArrayList var17 = (ArrayList)var16.getSettings().get("Conditions");
                     Condition var18 = (Condition)var17.get(var14);
                     var6 = var6 + var18.toString();
                  }
               } catch (Exception var21) {
                  Log.debug("Cannot print cross check condition, code: " + var9 + ". Exc.", var21);
                  var6 = L.t("cross check condition, code: %s", new Object[]{var9});
               }
            } else {
               var6 = BadStrategyException.ExplanationUnknown + ", code: " + var9;
            }

            if (!var6.contains("code:")) {
               var1.setTrackingInfoReasonToDismiss(var9, var6);
            }
         }

         int var27 = var3.get(var9);
         double var30 = SQUtils.round2(100.0 * ((double)var27 / this.projectRunInfo.strategiesRejected));
         if (var5 != 0) {
            var2.separator();
         }

         var2.beginArray();
         var2.setValue(var6, true);
         var2.setValue(var27, true);
         var2.setValue(var30 + " %", false);
         var2.endArray(false);
      }

      var2.endArray(false);
      return var2.toJson();
   }

   @Override
   public String getChannelName() {
      return "engine-channel";
   }
}
