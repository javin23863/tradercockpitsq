package com.strategyquant.plugin.Engine.impl.Panel;

import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.MemoryCleaner;
import com.strategyquant.tradinglib.enginecharts.EngineChartsList;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.TaskStats;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(EngineServlet.class);

   public EngineServlet() {
      try {
         EngineDb.init(SQStructure.PLUGINS_DIR + "EnginePanel");
         this.generate();
      } catch (Exception var2) {
         Log.error("Exc.", var2);
      }
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getTypes":
            return this.onGetTypes(var2);
         case "saveSelection":
            return this.onSaveSelection(var2);
         case "getInfo":
            return this.onGetInfo(var2);
         case "clearLog":
            return this.onClearLog(var2);
         case "cleanupMemory":
            return this.onCleanupMemory(var2);
         case "loadSettings":
            return this.onLoadSettings(var2);
         case "saveSettings":
            return this.onSaveSettings(var2);
         case "loadTextLog":
            return this.onLoadTextLog(var2);
         case "loadVisualLog":
            return this.onLoadVisualLog(var2);
         case "getFitnessEvolutionStats":
            return this.onGetFitnessEvolutionStats(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetFitnessEvolutionStats(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      int var3 = 0;

      try {
         SQProject var4 = ProjectEngine.get("BUILDER");
         GeneticInfo var5 = var4.getGeneticInfo();
         if (var5.isGeneticBuild()) {
            GPFitnessEvolutionData[] var6 = var5.getIslandsEvoData();
            if (var6 != null) {
               var3 = var6.length;
            }
         }
      } catch (Exception var7) {
      }

      var2.put("islands", var3);
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onLoadTextLog(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "projectName")[0];
      JSONObject var3 = new JSONObject();
      SQProject var4 = ProjectEngine.get(var2);
      var3.put("text", var4.printGlobalLog());
      var3.put("success", "ok");
      return var3.toString();
   }

   private String onLoadVisualLog(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "projectName")[0];
      String var3 = this.tryGetParam(var1, "type")[0];
      JSONObject var4 = new JSONObject();
      JSONArray var5 = new JSONArray();
      SQProject var6 = ProjectEngine.get(var2);

      for (int var7 = 0; var7 < var6.getTasksHashes().size(); var7++) {
         try {
            int var8 = var6.getTasksHashes().getInt(var7);
            ISQTask var9 = var6.getTaskByHash(var8);
            JSONObject var10 = new JSONObject()
               .put("title", var9.getIndex() + ". " + var9.getTitle())
               .put("type", var9.getType())
               .put("iterations", var9.getNumberOfRuns());
            TaskStats var11 = var6.getTaskStats(var8);
            if (var11 != null) {
               var10.put("stats", var11.getStatsJSONArray());
            }

            var5.put(var10);
         } catch (Exception var12) {
         }
      }

      var4.put("tasks", var5);
      var4.put("success", "ok");
      return var4.toString();
   }

   private String onLoadSettings(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "product")[0];
      JSONObject var3 = new JSONObject();
      boolean var4 = false;

      try {
         var4 = Boolean.parseBoolean(EngineDb.getSetting(var2 + "_deleteLogOnStart", "false"));
      } catch (Exception var6) {
      }

      var3.put("deleteLogOnStart", var4);
      var3.put("success", "ok");
      return var3.toString();
   }

   private String onSaveSettings(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "product")[0];
      String var3 = this.tryGetParam(var1, "deleteLogOnStart")[0];
      JSONObject var4 = new JSONObject();
      EngineDb.setSetting(var2 + "_deleteLogOnStart", var3);
      var4.put("success", "ok");
      return var4.toString();
   }

   private String onGetTypes(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "projectName")[0];
      if (!var2.equals("TaskManager")) {
         ProjectEngine.get(var2).publisher.resetLastData("engine-channel");
      }

      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();

      for (EngineChart var6 : EngineChartsList.getInstance().getAvailableClasses()) {
         JSONObject var7 = new JSONObject();
         var7.put("type", var6.getClassName());
         var7.put("name", var6.toString());
         var4.put(var7);
      }

      var3.put("types", var4);
      var3.put("settings", this.getSettings(var2));
      var3.put("success", "ok");
      return var3.toString();
   }

   private JSONArray getSettings(String var1) throws Exception {
      JSONArray var2 = new JSONArray();
      var2.put(EngineDb.getSetting(this.getKey(var1, 0), "DatabankFitnessIS"));
      var2.put(EngineDb.getSetting(this.getKey(var1, 1), "HeapMemoryChart"));
      var2.put(EngineDb.getSetting(this.getKey(var1, 2), "OffHeapMemoryChart"));
      var2.put(EngineDb.getSetting(this.getKey(var1, 3), "OffHeapMemoryInfoChart"));
      var2.put(EngineDb.getSetting(this.getKey(var1, 4), "AcceptedStrategiesPerHour"));
      var2.put(EngineDb.getSetting(this.getKey(var1, 5), "AverageStrategiesPerHourChart"));
      return var2;
   }

   private String getKey(String var1, int var2) {
      return var1 + "_chart" + var2;
   }

   private String onSaveSelection(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "projectName")[0];
      String var3 = this.tryGetParam(var1, "type")[0];
      int var4 = Integer.parseInt(this.tryGetParam(var1, "number")[0]);
      JSONObject var5 = new JSONObject();
      EngineChartsList.getInstance().findClassByName(var3);
      ProjectEngine.get(var2);
      EngineDb.setSetting(this.getKey(var2, var4), var3);
      this.generateCharts();
      var5.put("success", "ok");
      return var5.toString();
   }

   private String onGetInfo(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      ProjectEngine.get(var3).publisher.resetLastData("engine-channel");
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onCleanupMemory(Map<String, String[]> var1) throws Exception {
      String var2 = MemoryCleaner.cleanNow();
      JSONObject var3 = new JSONObject();
      var3.put("success", var2);
      return var3.toString();
   }

   private String onClearLog(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      ProjectEngine.get(var3).log.clear();
      var2.put("success", "ok");
      return var2.toString();
   }

   private void generate() {
      (new Thread() {
         @Override
         public void run() {
            while (true) {
               try {
                  EngineServlet.this.generateCharts();
                  Thread.sleep(5000L);
               } catch (Exception var2) {
               }
            }
         }
      }).start();
   }

   private void generateCharts() {
      for (SQProject var2 : ProjectEngine.list()) {
         var2.engineCharts.generate();
      }

      ArrayList var4 = EngineChartsList.getInstance().globalCharts;

      for (int var5 = 0; var5 < var4.size(); var5++) {
         EngineChart var3 = (EngineChart)var4.get(var5);
         var3.addNextValue();
      }
   }
}
