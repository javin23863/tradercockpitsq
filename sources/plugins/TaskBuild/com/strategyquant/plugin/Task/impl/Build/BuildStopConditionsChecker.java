package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.databank.IDatabankFilter;
import com.strategyquant.tradinglib.project.ProgressEngine;

public class BuildStopConditionsChecker {
   public static boolean shouldStop(Databank var0, SettingsMap var1, ProgressEngine var2, ProjectRunInfo var3) {
      String var4 = var1.getString("StopConditionType");
      IDatabankFilter var5 = var0.getFilter();
      switch (var4) {
         case "databank-full":
            if (var5 != null && var5.getMaxStrategies() <= var0.size()) {
               if (!var2.isFinished()) {
                  var2.printToLog(L.t("Build finished because databank is full", new Object[0]));
               }

               return true;
            }
            break;
         case "passed-count":
            int var8 = var1.getInt("MaxStrategiesPassed");
            long var9 = var3.strategiesAccepted;
            if (var9 >= var8) {
               if (!var2.isFinished()) {
                  var2.printToLog(L.t("Build finished because totally %d strategies were generated, excluding dismissed", new Object[]{var8}));
               }

               return true;
            }
         case "never":
         default:
            break;
         case "time-limit":
            long var11 = var1.getLong("MaxBuildRunTime");
            return var3.taskRunningTime >= var11;
      }

      return false;
   }
}
