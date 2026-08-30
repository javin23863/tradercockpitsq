package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectGlobalLog {
   private static final Logger Log = LoggerFactory.getLogger(ProjectGlobalLog.class);
   private PrintWriter out = null;
   private File file;
   private Int2ObjectOpenHashMap<TaskStats> stats = new Int2ObjectOpenHashMap();

   public ProjectGlobalLog(String var1) throws Exception {
      this.createLogFile(var1);
   }

   private void createLogFile(String var1) throws Exception {
      String var2 = var1
         + "log/global_log_"
         + SQTime.toString(System.currentTimeMillis(), DateTimeFormat.forPattern("yyyyMMdd"))
         + "_"
         + SQTime.toString(System.currentTimeMillis(), DateTimeFormat.forPattern("HHmmss"))
         + ".log";
      this.file = new File(var2);
      this.file.getParentFile().mkdirs();
      Log.info("Creating project log at '{}'", var2);
      this.out = new PrintWriter(new FileWriter(this.file, StandardCharsets.UTF_8, false));
      this.print(L.t("Project", new Object[0]) + ": " + this.file.getParentFile().getParentFile().getName());
      this.print(L.t("Log file", new Object[0]) + ": /" + this.file.getName());
   }

   public void print(String var1, boolean var2) {
      if (var1 != null) {
         if (this.out != null) {
            this.out.write(var1);
            if (var2) {
               this.out.write("\n");
            }
         }
      }
   }

   public void print(String var1) {
      if (this.out != null) {
         if (var1 != null) {
            this.out.write(var1 + "\n");
         }
      }
   }

   public void printSeparator() {
      this.print("--------------------------------------------------------------------------------------------------------------------------");
   }

   public void printDoubleSeparator() {
      this.print("================================================================================");
   }

   public void printEmptyLine() {
      this.print("");
   }

   public String load() throws Exception {
      if (this.out != null) {
         this.out.flush();
      }

      return SQUtils.fileToString(this.file);
   }

   public void close() {
      if (this.out != null) {
         this.out.flush();
         this.out.close();
         this.out = null;
      }
   }

   public void addDurationStats(ProjectRunInfo var1) {
      this.printEmptyLine();
      this.print(L.t("Time per strategy details", new Object[0]) + ":");
      HashMap var2 = var1.getDurationStats();
      double var3 = 0.0;

      for (String var6 : var2.keySet()) {
         var3 += ((ComplexDurationStats)var2.get(var6)).totalTime;
      }

      for (String var9 : var2.keySet()) {
         ComplexDurationStats var7 = (ComplexDurationStats)var2.get(var9);
         this.print(
            var9
               + ": Count: "
               + var7.count
               + ", Avg. time: "
               + SQUtils.d2String(var7.avgTime, 4)
               + " s., Total time: "
               + SQUtils.d2String(var7.totalTime, 4)
               + " s., % of all: "
               + SQUtils.d2(SQUtils.round2(100.0 * (var7.totalTime / var3)))
               + " %"
         );
      }
   }

   public void addAcceptedStats(ProjectRunInfo var1, ISQTask var2) {
      this.printEmptyLine();
      if (var2.getType().equals("Build")) {
         this.print(L.t("Accepted details", new Object[0]) + ":");
      } else {
         this.print(L.t("Passed details", new Object[0]) + ":");
      }

      Int2IntOpenHashMap var3 = var1.getAcceptedCounts();
      IntSet var5 = var3.keySet();
      IntIterator var6 = var5.iterator();

      while (var6.hasNext()) {
         int var7 = (Integer)var6.next();
         String var4;
         if (var7 == 0) {
            var4 = "In databank";
         } else if (var7 == 10007) {
            var4 = BadStrategyException.TxtLongReplacedWithBetterStrategy;
         } else if (var7 == 10006) {
            var4 = BadStrategyException.TxtLongReasonStrategyTooSimilar;
         } else {
            var4 = BadStrategyException.ExplanationUnknown + ", code: " + var7;
         }

         int var8 = var3.get(var7);
         double var9 = SQUtils.round2(100.0 * ((double)var8 / var1.strategiesAccepted));
         this.print(var4 + ", Count: " + var8 + ", % of all: " + var9 + " %");
      }
   }

   public void addDismissalStats(ProjectRunInfo var1, ISQTask var2) {
      this.printEmptyLine();
      if (var2.getType().equals("Build")) {
         this.print(L.t("Rejected details", new Object[0]) + ":");
      } else {
         this.print(L.t("Failed details", new Object[0]) + ":");
      }

      Int2IntOpenHashMap var3 = var1.getDismissalCounts();
      Int2ObjectOpenHashMap var4 = var1.getDismissalReasons();
      IntSet var6 = var3.keySet();
      IntIterator var7 = var6.iterator();

      while (var7.hasNext()) {
         int var8 = (Integer)var7.next();
         String var5;
         if (var4.containsKey(var8)) {
            var5 = (String)var4.get(var8);
         } else if (var8 < 100000) {
            var5 = BadStrategyException.getReasonAsString(var8);
         } else if (var8 >= 100000 && var8 < 200000) {
            try {
               TaskSettingsData var24 = var2.getSettingsData();
               int var27 = var8 - 100000;
               var5 = L.t("Initial population filter", new Object[0]) + ": ";
               ArrayList var30 = (ArrayList)var24.getParams().get("InitialGenerationConditions");
               Condition var32 = (Condition)var30.get(var27);
               var5 = var5 + var32.toString();
            } catch (Exception var18) {
               var5 = L.t("initial condition, code: %s", new Object[]{var8});
            }
         } else if (var8 >= 200000 && var8 < 300000) {
            try {
               TaskSettingsData var23 = var2.getSettingsData();
               int var26 = var8 - 200000;
               var5 = L.t("Global filter", new Object[0]) + ":  ";
               ArrayList var29 = (ArrayList)var23.getParams().get("Conditions");
               Condition var31 = (Condition)var29.get(var26);
               var5 = var5 + var31.toString();
            } catch (Exception var17) {
               var5 = L.t("global condition, code: %s", new Object[]{var8});
            }
         } else if (var8 >= 300000) {
            try {
               TaskSettingsData var9 = var2.getSettingsData();
               int var10 = var8 - 300000;
               int var11 = var10 / 1000;
               int var12 = var10 - var11 * 1000;
               ArrayList var13 = (ArrayList)var9.getParams().get("CrossChecks");
               ICrossCheck var14 = (ICrossCheck)var13.get(var11);
               var5 = L.t("Cross Check filter in '%s'", new Object[]{var14.getName()}) + ":  ";
               if (WalkForwardCrossCheckMethod.class.isAssignableFrom(var14.getClass()) && !var14.getSettingName().equals("OptProfileSysParamPermutation")) {
                  var5 = var5 + L.t("Robustness score didn't pass.", new Object[0]);
               } else {
                  ArrayList var15 = (ArrayList)var14.getSettings().get("Conditions");
                  Condition var16 = (Condition)var15.get(var12);
                  var5 = var5 + var16.toString();
               }
            } catch (Exception var19) {
               var5 = L.t("cross check condition, code: %s", new Object[]{var8});
            }
         } else {
            var5 = BadStrategyException.ExplanationUnknown + ", code: " + var8;
         }

         int var25 = var3.get(var8);
         double var28 = SQUtils.round2(100.0 * ((double)var25 / var1.strategiesRejected));
         this.print(var5 + ", Count: " + var25 + ", % of all: " + var28 + " %");
      }
   }

   public TaskStats getTaskStats(int var1) {
      TaskStats var2 = (TaskStats)this.stats.get(var1);
      if (var2 == null) {
         var2 = new TaskStats();
         this.stats.put(var1, var2);
      }

      return var2;
   }

   public void increaseStats(int var1, String var2, long var3, String var5, long var6) {
      TaskStats var8 = this.getTaskStats(var1);
      var8.increaseStat(TaskStat.BAR, var2, var3, 1);
      var8.increaseStat(TaskStat.BAR, var5, var6, 2, var2);
   }

   public void increaseStats(String var1, int var2, String var3, long var4, int var6) {
      TaskStats var7 = this.getTaskStats(var2);
      var7.increaseStat(var1, var3, var4, var6);
   }

   public void setStats(String var1, int var2, String var3, long var4, int var6, String var7) {
      TaskStats var8 = this.getTaskStats(var2);
      var8.setStat(var1, var3, var4, var6, var7);
   }
}
