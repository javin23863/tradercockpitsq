package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.project.ComplexDurationStats;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashMap;

public class ProjectRunInfo {
   public static final int ERROR_TYPE_GENERAL = 10;
   public static final int ERROR_TYPE_FATAL = 50;
   public long backtestsPerformed;
   public long strategiesGenerated;
   public long strategiesRejected;
   public double strategiesRejectedPct;
   public long strategiesAccepted;
   public double strategiesAcceptedPct;
   public long timePerStrategy;
   public long timePerAcceptedStrategy;
   public double strategiesPerHour;
   public double acceptedStrategiesPerHour;
   public long strategiesPassed;
   public long strategiesFailed;
   public long startTime;
   public long totalProjectRunningTime;
   public long projectRunningTime;
   public long taskRunningTime;
   public long timeToFinish;
   public int jobExceptionsCount;
   public long totalJobsDone;
   public long runningJobs;
   public long waitingJobs;
   public int totalSteps;
   public int step;
   public boolean infiniteProgress;
   public double progressPercent;
   public String info;
   public String error;
   public String errorInfo;
   public int errorType;
   private Int2IntOpenHashMap dismissalCounts = new Int2IntOpenHashMap();
   private Int2ObjectOpenHashMap<String> dismissalReasons = new Int2ObjectOpenHashMap();
   private Int2IntOpenHashMap acceptedCounts = new Int2IntOpenHashMap();
   private HashMap<String, ComplexDurationStats> complexDurationStats = new HashMap<>();

   public void copyFrom(ProjectRunInfo var1, boolean var2) {
      this.jobExceptionsCount = var1.jobExceptionsCount;
      this.totalJobsDone = var1.totalJobsDone;
      this.runningJobs = var1.runningJobs;
      this.waitingJobs = var1.waitingJobs;
      this.taskRunningTime = var1.taskRunningTime;
      this.timeToFinish = var1.timeToFinish;
      this.backtestsPerformed = var1.backtestsPerformed;
      this.strategiesGenerated = var1.strategiesGenerated;
      this.strategiesRejected = var1.strategiesRejected;
      this.strategiesRejectedPct = var1.strategiesRejectedPct;
      this.strategiesAccepted = var1.strategiesAccepted;
      this.strategiesAcceptedPct = var1.strategiesAcceptedPct;
      this.timePerStrategy = var1.timePerStrategy;
      this.timePerAcceptedStrategy = var1.timePerAcceptedStrategy;
      this.strategiesPerHour = var1.strategiesPerHour;
      this.acceptedStrategiesPerHour = var1.acceptedStrategiesPerHour;
      this.strategiesPassed = var1.strategiesPassed;
      this.strategiesFailed = var1.strategiesFailed;
      this.totalSteps = var1.totalSteps;
      this.step = var1.step;
      this.infiniteProgress = var1.infiniteProgress;
      this.progressPercent = var1.progressPercent;
      this.error = var1.error;
      this.errorInfo = var1.errorInfo;
      this.errorType = var1.errorType;
      this.info = var1.info;
      if (var2) {
         try {
            this.acceptedCounts.clear();
            this.acceptedCounts.putAll(var1.acceptedCounts);
         } catch (NullPointerException var7) {
         }

         try {
            this.dismissalCounts.clear();
            this.dismissalCounts.putAll(var1.dismissalCounts);
         } catch (NullPointerException var6) {
         }

         try {
            this.dismissalReasons.clear();
            this.dismissalReasons.putAll(var1.dismissalReasons);
         } catch (NullPointerException var5) {
         }

         try {
            this.complexDurationStats.clear();
            this.complexDurationStats.putAll(var1.complexDurationStats);
         } catch (NullPointerException var4) {
         }
      }
   }

   public void increaseDismissalReasonCount(int var1) {
      IntArrayList var2 = BadStrategyException.getSubReasons(var1);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         int var4 = var2.getInt(var3);
         if (!this.dismissalCounts.containsKey(var4)) {
            this.dismissalCounts.put(var4, 1);
         } else {
            int var5 = this.dismissalCounts.get(var4);
            this.dismissalCounts.put(var4, var5 + 1);
         }
      }
   }

   public Int2ObjectOpenHashMap<String> getDismissalReasons() {
      return this.dismissalReasons;
   }

   public Int2IntOpenHashMap getDismissalCounts() {
      return this.dismissalCounts;
   }

   public void resetDismissalCounts() {
      this.dismissalCounts.clear();
      this.dismissalReasons.clear();
   }

   public void increaseAcceptedReasonCount(int var1) {
      if (!this.acceptedCounts.containsKey(var1)) {
         this.acceptedCounts.put(var1, 1);
      } else {
         int var2 = this.acceptedCounts.get(var1);
         this.acceptedCounts.put(var1, var2 + 1);
      }
   }

   public Int2IntOpenHashMap getAcceptedCounts() {
      return this.acceptedCounts;
   }

   public void resetAcceptedCounts() {
      this.acceptedCounts.clear();
   }

   public synchronized void updateDurationStats(DurationStats var1) {
      HashMap var2 = var1.getMap();
      if (var2 != null) {
         for (String var4 : var2.keySet()) {
            ComplexDurationStats var5;
            if (!this.complexDurationStats.containsKey(var4)) {
               var5 = new ComplexDurationStats();
               this.complexDurationStats.put(var4, var5);
            } else {
               var5 = this.complexDurationStats.get(var4);
            }

            var5.update(var4, (Double)var2.get(var4));
         }
      }
   }

   public HashMap<String, ComplexDurationStats> getDurationStats() {
      return this.complexDurationStats;
   }

   public void resetDurationStats() {
      this.complexDurationStats.clear();
   }

   public void reset(boolean var1, boolean var2) {
      this.jobExceptionsCount = 0;
      this.totalJobsDone = 0L;
      this.runningJobs = 0L;
      this.waitingJobs = 0L;
      if (var2) {
         this.startTime = System.currentTimeMillis();
         this.projectRunningTime = 0L;
         this.totalProjectRunningTime = 0L;
      }

      this.taskRunningTime = 0L;
      this.backtestsPerformed = 0L;
      this.strategiesGenerated = 0L;
      this.strategiesRejected = 0L;
      this.strategiesRejectedPct = 0.0;
      this.strategiesAccepted = 0L;
      this.strategiesAcceptedPct = 0.0;
      this.timePerStrategy = 0L;
      this.timePerAcceptedStrategy = 0L;
      this.strategiesPerHour = 0.0;
      this.acceptedStrategiesPerHour = 0.0;
      this.strategiesPassed = 0L;
      this.strategiesFailed = 0L;
      this.infiniteProgress = var1;
      this.error = null;
      this.errorInfo = null;
      this.errorType = 0;
      this.progressPercent = 0.0;
      this.resetDismissalCounts();
      this.resetAcceptedCounts();
      this.resetDurationStats();
   }
}
