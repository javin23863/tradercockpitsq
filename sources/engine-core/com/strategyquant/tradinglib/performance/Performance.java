package com.strategyquant.tradinglib.performance;

import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.gridlib.config.Config;
import com.strategyquant.gridlib.config.SingleFolderConfig;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.memory.CpuInfo;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Performance {
   private static final Logger LOGGER = LoggerFactory.getLogger(Performance.class);
   public static int totalCores = 2;
   public static final String CORE_USAGE_SINGLE = "1";
   public static final String CORE_USAGE_RES1CORE = "-1";
   public static final String CORE_USAGE_MAXPERF = "100%";
   public static final String CORE_USAGE_CUSTOM = "X";
   public static final String benchmarkTimePerTickKey = "BenchmarkTimePerTick";
   public static double benchmarkTimePerTick = 0.0;

   public static void init() throws Exception {
      totalCores = CpuInfo.getAvailableProcessors();
      SQGrid.init(createGridConfig());
      benchmarkTimePerTick = Double.parseDouble(MainApp.settings().get("BenchmarkTimePerTick", "0"));
   }

   public static JSONObject getCoreUsageOptions() {
      JSONObject var0 = new JSONObject();
      var0.put("singleCore", "1");
      var0.put("reserve1Core", "-1");
      var0.put("maxPerformance", "100%");
      var0.put("customCores", "X");
      return var0;
   }

   public static boolean isCustomCoreUsage(String var0) {
      if (!var0.equals("1") && !var0.equals("-1") && !var0.equals("100%")) {
         try {
            Integer.parseInt(var0);
            return true;
         } catch (Exception var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Config createGridConfig() {
      Config var0 = new Config(null);
      var0.setFinishedJobsInDesc(100);
      var0.setUseAffinity(MainApp.settings().getBoolean("threadAffinity", false));
      String var1 = MainApp.settings().get("coreUsage", "-1");
      var0.setCoreUsage(var1.startsWith("X") ? var1.replaceAll("\\D+", "") : var1);
      boolean var2 = MainApp.settings().getBoolean("highPriority", false);
      LOGGER.info("High priority: " + (var2 ? "ON" : "OFF"));
      var0.setAdjustTopSizeOfCores(var2);
      var0.getFolderConfigs().setDataFolderRoot(MainApp.getDataPath());
      var0.getFolderConfigs().getFolderConfig().add(new SingleFolderConfig("internal/libs", false));
      var0.getFolderConfigs().getFolderConfig().add(new SingleFolderConfig("data", false));
      var0.getFolderConfigs().getFolderConfig().add(new SingleFolderConfig("extends", true));
      var0.setFoldersWithJars(
         new String[]{
            MainApp.getDataPath() + "internal/libs",
            MainApp.getDataPath() + "internal/plugins/ProjectBuilder",
            MainApp.getDataPath() + "internal/plugins/ProjectRetester",
            MainApp.getDataPath() + "internal/plugins"
         }
      );
      return var0;
   }
}
