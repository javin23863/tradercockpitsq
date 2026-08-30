package com.strategyquant.plugin.Results.impl.SequentialOptimization;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.robustnesstests.FitnessData;
import com.strategyquant.tradinglib.robustnesstests.SequentialOptimizationResults;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequentialOptimizationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SequentialOptimizationServlet.class);
   private static final String LOCK_SEQOPTSERVLET = "SequentialOptimizationServlet";
   private IResultsGroupProvider rgProvider;

   public SequentialOptimizationServlet(IResultsGroupProvider var1) {
      this.rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "applyParams":
            return this.onApplyParams(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = this.rgProvider.get(var1, "SequentialOptimizationServlet");

      try {
         SequentialOptimizationResults var4 = var3.mainResult().getSequentialOptimizaionResults();
         if (var4 == null) {
            return apiErrorJSON(L.t("Strategy doesn't contain Sequential Optimization results", new Object[0]), null);
         }

         JSONArray var5 = new JSONArray();

         for (int var6 = 0; var6 < var4.size(); var6++) {
            FitnessData var7 = (FitnessData)var4.get(var6);
            JSONObject var8 = var7.toJSON();
            var8.put("chart", this.getChartData(var7));
            var5.put(var8);
         }

         var2.put("results", var5);
         var2.put("parametersApplied", var4.isApplyToStrategy());
         var2.put("passed", var4.isPassed());
      } finally {
         if (var3 != null) {
            var3.releaseLock("SequentialOptimizationServlet");
            Object var12 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private JSONObject getChartData(FitnessData var1) {
      BarChart var2 = new BarChart();
      var2.maxXTicksLimit = 1000;
      var2.yAxisTitle = L.t("Fitness", new Object[0]);
      var2.xAxisTitle = L.t("Parameter values", new Object[0]);
      var2.tooltips = false;
      var2.disableEvents = true;
      var2.setCategoryColor("all", "blue");

      for (int var3 = 0; var3 < var1.values.length; var3++) {
         double var4 = var1.values[var3];
         double var6 = var1.fitness[var3];
         if (var4 < var1.bestAreaStartValue || var4 > var1.bestAreaEndValue) {
            var2.addValue("all", var4, var6);
         } else if (var4 == var1.bestValue) {
            var2.addValue("all", var4, var6, "red");
         } else {
            var2.addValue("all", var4, var6, "green");
         }
      }

      return var2.toJSON();
   }

   private String onApplyParams(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = this.rgProvider.get(var1, "SequentialOptimizationServlet");

      try {
         SequentialOptimizationResults var4 = var3.mainResult().getSequentialOptimizaionResults();
         if (var4 == null) {
            return apiErrorJSON(L.t("Strategy doesn't contain Sequential Optimization results", new Object[0]), null);
         }

         StrategyBase var5 = StrategyBase.createXmlStrategy(var3.getStrategyXml());
         var5.transformToVariables(var4.isSymmetricVariables(), var4.getParamTypes());
         Variables var6 = var5.variables();

         for (int var7 = 0; var7 < var4.size(); var7++) {
            FitnessData var8 = (FitnessData)var4.get(var7);
            if (var8.stableAreaFound) {
               Variable var9 = var6.get(var8.variable.getName());
               if (var9 == null) {
                  return apiErrorJSON(L.t("Unable to modify variable %s - variable not found", new Object[]{var8.variable.getName()}), null);
               }

               var9.setValue(var8.bestValue);
            }
         }

         var5.transformToNumbers();
         var3.mainResult().addStrategy(var5);
         var3.mainResult().addStrategyXml(var5.getStrategyXml());
      } finally {
         if (var3 != null) {
            var3.releaseLock("SequentialOptimizationServlet");
            Object var14 = null;
         }
      }

      var2.put("success", L.t("Parameters applied to strategy", new Object[0]));
      return var2.toString();
   }
}
