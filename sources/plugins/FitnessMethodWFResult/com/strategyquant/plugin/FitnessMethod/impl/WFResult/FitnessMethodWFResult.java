package com.strategyquant.plugin.FitnessMethod.impl.WFResult;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.HideDatabankChoice;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Maros Fric")
@Name(name = "Fitness Function WF result plugin")
@Category(name = "FitnessFunction")
@License(text = "")
@ShortDesc(text = "Fitness Function WF result")
@HideDatabankChoice
@PluginImplementation
public class FitnessMethodWFResult implements IFitnessFunction {
   public static final Logger Log = LoggerFactory.getLogger(FitnessMethodWFResult.class);
   public static final String ComputeFromWFResult = "ComputeFromWFResult";
   private String databankColumnName = null;
   private ArrayList<FitnessMethodWFResult.Goal> weightedGoals = new ArrayList<>();

   public FitnessMethodWFResult() {
   }

   public FitnessMethodWFResult(String var1) {
      this.databankColumnName = var1;
   }

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODEAlgoWizardStandalone";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      return this.computeFitness(var1, var2, var3, true);
   }

   public double computeFitness(ResultsGroup var1, byte var2, byte var3, boolean var4) throws Exception {
      if (this.databankColumnName.equals("Weighted")) {
         double var5 = this.computeWeightedFitness(var1, var2, var3, var4);
         return var5 == 0.0 ? this.getFitnessValue(var1, var2, var3, "NetProfit", 0.0, (byte)0, var4) : var5;
      } else {
         return this.getFitnessValue(var1, var2, var3, this.databankColumnName, 0.0, (byte)0, var4);
      }
   }

   private double getFitnessValue(ResultsGroup var1, byte var2, byte var3, String var4, double var5, byte var7, boolean var8) throws Exception {
      DatabankColumn var9 = (DatabankColumn)DatabankColumns.get().findClassByName(var4);
      double var10 = this.getMetricValue(var1, var2, var3, var4);
      double var12 = var9.transformToFitnessRange(var10, var5, var7);
      if (var12 < 0.0) {
         var12 = 0.0;
      }

      if (var12 > 1.0) {
         var12 = 1.0;
      }

      return var12;
   }

   private double computeWeightedFitness(ResultsGroup var1, byte var2, byte var3, boolean var4) throws Exception {
      float var5 = 0.0F;

      for (int var6 = 0; var6 < this.weightedGoals.size(); var6++) {
         if (this.weightedGoals.get(var6).use) {
            var5 = (float)(var5 + this.weightedGoals.get(var6).weight);
         }
      }

      float var11 = 0.0F;

      for (int var7 = 0; var7 < this.weightedGoals.size(); var7++) {
         FitnessMethodWFResult.Goal var8 = this.weightedGoals.get(var7);
         if (var8.use) {
            double var9 = this.getFitnessValue(var1, var2, var3, var8.statsValueName, var8.target, var8.valueType, var4);
            var11 = (float)(var11 + var8.weight / var5 * var9);
         }
      }

      return var11;
   }

   public void initFitnessFromXml(Element var1) {
      this.weightedGoals.clear();
      Element var2 = var1.getChild("Ranking");
      this.databankColumnName = var2.getAttributeValue("type");

      for (Element var4 : var2.getChildren("Goal")) {
         FitnessMethodWFResult.Goal var5 = new FitnessMethodWFResult.Goal();
         String var6 = var4.getAttributeValue("use");
         var5.use = var6.equals("true") || var6.equalsIgnoreCase("1");
         var5.statsValueName = var4.getAttributeValue("type");
         var5.weight = XMLUtil.getDoubleAttr(var4, "weight", 1.0);
         var5.valueType = XMLUtil.getByteAttr(var4, "valueType", (byte)0);
         var5.target = XMLUtil.getDoubleAttr(var4, "target", 0.0);

         try {
            if (var5.use) {
               DatabankColumn var7 = (DatabankColumn)DatabankColumns.get().findClassByName(var5.statsValueName);
               this.weightedGoals.add(var5);
            }
         } catch (Exception var8) {
            Log.error("Ranking Criterium SKIPPED. Reason: ", var8);
         }
      }
   }

   public byte getFitnessType() throws Exception {
      return this.databankColumnName.equals("Weighted") ? 1 : ((DatabankColumn)DatabankColumns.get().findClassByName(this.databankColumnName)).valueType;
   }

   public String getFitnessKey() {
      return "ComputeFromWFResult";
   }

   public String getFitnessName() {
      return L.tsq("WalkForward result");
   }

   public String getFitnessDatabankColumnName() {
      return this.databankColumnName;
   }

   public ArrayList<DatabankColumn> getUsedStatValues() throws NonexistingCustomClassException {
      ArrayList var1 = new ArrayList();
      if (!this.databankColumnName.equals("Weighted")) {
         var1.add((DatabankColumn)DatabankColumns.get().findClassByName(this.databankColumnName));
      } else {
         for (int var2 = 0; var2 < this.weightedGoals.size(); var2++) {
            FitnessMethodWFResult.Goal var3 = this.weightedGoals.get(var2);
            if (var3.use) {
               var1.add((DatabankColumn)DatabankColumns.get().findClassByName(var3.statsValueName));
            }
         }
      }

      return StatsComputer.getAllDependentStatValues(var1);
   }

   public String printWeightedGoals() {
      String var1 = "";

      for (int var2 = 0; var2 < this.weightedGoals.size(); var2++) {
         FitnessMethodWFResult.Goal var3 = this.weightedGoals.get(var2);
         if (var3.use) {
            var1 = var1 + this.weightedGoals.get(var2).statsValueName;
            if (var2 < this.weightedGoals.size() - 1) {
               var1 = var1 + ", ";
            }
         }
      }

      return var1;
   }

   public ArrayList<MetricForFitness> getMetricsForFitness() {
      ArrayList var1 = new ArrayList();
      if (this.databankColumnName.equals("Weighted")) {
         for (int var2 = 0; var2 < this.weightedGoals.size(); var2++) {
            FitnessMethodWFResult.Goal var3 = this.weightedGoals.get(var2);
            if (var3.use) {
               MetricForFitness var4 = new MetricForFitness();
               var4.metric = this.weightedGoals.get(var2).statsValueName;
               var4.weight = this.weightedGoals.get(var2).weight;
               var4.valueType = this.weightedGoals.get(var2).valueType;
               var4.target = this.weightedGoals.get(var2).target;
               var1.add(var4);
            }
         }
      } else {
         MetricForFitness var5 = new MetricForFitness();
         var5.metric = this.databankColumnName;
         var5.weight = 1.0;
         var5.valueType = 0;
         var5.target = 0.0;
         var1.add(var5);
      }

      return var1;
   }

   public double getMetricValue(ResultsGroup var1, byte var2, byte var3, String var4) throws Exception {
      DatabankColumn var5 = (DatabankColumn)DatabankColumns.get().findClassByName(var4);
      WalkForwardMatrixResult var6 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
      if (var6 == null) {
         return 0.0;
      } else {
         ArrayList var7 = var6.getResultList();
         if (var7 != null && !var7.isEmpty()) {
            WalkForwardResult var8 = (WalkForwardResult)var7.get(0);
            return var8.stats.getDouble(var4, 0.0);
         } else {
            return 0.0;
         }
      }
   }

   public IFitnessFunction clone() {
      FitnessMethodWFResult var1 = new FitnessMethodWFResult();
      var1.databankColumnName = this.databankColumnName;
      var1.weightedGoals = this.cloneGoals();
      return var1;
   }

   private ArrayList<FitnessMethodWFResult.Goal> cloneGoals() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < this.weightedGoals.size(); var2++) {
         FitnessMethodWFResult.Goal var3 = this.weightedGoals.get(var2);
         FitnessMethodWFResult.Goal var4 = new FitnessMethodWFResult.Goal();
         var4.valueType = var3.valueType;
         var4.use = var3.use;
         var4.statsValueName = var3.statsValueName;
         var4.weight = var3.weight;
         var4.target = var3.target;
         var1.add(var4);
      }

      return var1;
   }

   private class Goal {
      public byte valueType;
      public boolean use;
      public String statsValueName;
      public double weight;
      public double target;

      private Goal() {
      }
   }
}
