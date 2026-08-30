package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationSettings implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationSettings");
   private static final String SOURCE_FALLBACK = "<Source type=\"2\" relativePath=\"false\" />";
   private static final String WALKFORWARD_FALLBACK = "<WalkForward type=\"0\" period=\"30\" optimization=\"15\">\n      <Param1 value=\"20\" />\n      <Param2 value=\"10\" />\n    </WalkForward>";
   private String projectName;
   public int type;
   public boolean singleStrategy;
   public boolean storeAll;
   public boolean floating;
   public int periodType;
   public int optimizationType;
   public int param1Start;
   public int param1Stop;
   public int param1Step;
   public int param2Start;
   public int param2Stop;
   public int param2Step;
   public File strategyFile;
   public OptimizationParams parameters = new OptimizationParams();
   public int optimizationMethod;
   public Databank inputDatabank = null;
   public Databank outputDatabank = null;
   public int maxOptimizationBacktests = 0;
   public Element elConditions;
   public ArrayList<Condition> conditions;
   public int thresholdPct;
   public int robCombRows;
   public int robCombCols;
   public int robMinComb;
   public boolean singleThreaded = false;
   public boolean deleteFailedStr = false;
   public boolean dontSaveOriginalStr = false;
   public int stableAreaCount;
   public int stabilityRangePct;
   public int pctToPass;
   private String taskName = null;
   private HashMap<String, ParametersSettings> configParamSettings;
   private String projectLogMessage = "";
   public String debugWFFitnessPath = null;

   public OptimizationSettings(String var1) {
      this.projectName = var1;
   }

   public void setFromXML(Element var1, Element var2) throws Exception {
      this.type = XMLUtil.tryGetIntAttr(var2, "type");
      this.maxOptimizationBacktests = XMLUtil.getIntAttr(var2, "maxOptimizations", 100);
      this.taskName = this.recognizeTaskName(var1);
      Element var3 = XMLUtil.getChildElem(var2, "Source", "<Source type=\"2\" relativePath=\"false\" />");
      this.singleStrategy = XMLUtil.tryGetIntAttr(var3, "type") == 1;
      if (this.singleStrategy) {
         this.inputDatabank = ProjectEngine.get(this.projectName).getDatabanks().get("Results");

         try {
            String var4 = var3.getTextTrim();
            this.strategyFile = new File(var4);
            if (!this.strategyFile.exists()) {
               throw new Exception(String.format("Selected strategy file '%s' doesn't exist.", var4));
            }
         } catch (Exception var7) {
            throw new Exception("Cannot get input strategy. " + var7.getMessage(), var7);
         }
      } else if (this.projectName.equals("Optimizer")) {
         this.inputDatabank = ProjectEngine.get(this.projectName).getDatabanks().get("Strategies to optimize");
      } else {
         this.inputDatabank = ProjectConfigHelper.getInputDatabank(this.projectName, var2.getParentElement());
      }

      if (this.projectName.equals("Optimizer")) {
         this.outputDatabank = ProjectEngine.get(this.projectName).getDatabanks().get("Results");
      } else {
         try {
            this.outputDatabank = ProjectConfigHelper.getOutputDatabank(this.projectName, var2.getParentElement());
         } catch (Exception var6) {
            this.outputDatabank = ProjectEngine.get(this.projectName).getDatabanks().get("Results");
         }
      }

      Element var8 = XMLUtil.getChildElem(var1, "Rankings");
      Element var5 = var8.getChild("WalkForward");
      if (var5 == null) {
         throw new Exception("Walk-Forward Optimization is missing Robustness settings");
      }

      this.dontSaveOriginalStr = XMLUtil.getBoolean(var5, "DontSaveOriginalStr", false);
      switch (this.type) {
         case 1:
         case 4:
            this.loadSimpleSettings(var2);
            break;
         case 2:
            this.loadWalkForwardSettings(var2);
            break;
         case 3:
            this.loadWalkForwardSettings(var2);
            break;
         default:
            throw new Exception("Unknown optimization type '" + this.type + "'. ");
      }
   }

   private String recognizeTaskName(Element var1) {
      if (var1 == null) {
         return null;
      }

      Element var2 = var1.getParentElement();
      return var2 == null ? null : var2.getAttributeValue("type");
   }

   private void loadSimpleSettings(Element var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(var1, "SimpleOptimization");
      this.storeAll = XMLUtil.tryGetIntAttr(var2, "type") == 5;
      this.stableAreaCount = XMLUtil.tryGetIntAttr(var2, "resultsCount");
      this.stabilityRangePct = XMLUtil.tryGetIntAttr(var2, "stabilityRange");
      this.pctToPass = XMLUtil.tryGetIntAttr(var2, "pctToPass");
   }

   private void loadWalkForwardSettings(Element var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(
         var1,
         "WalkForward",
         "<WalkForward type=\"0\" period=\"30\" optimization=\"15\">\n      <Param1 value=\"20\" />\n      <Param2 value=\"10\" />\n    </WalkForward>"
      );
      this.optimizationType = XMLUtil.tryGetIntAttr(var2, "type");
      if (this.optimizationType < 0) {
         this.optimizationType = 0;
      }

      this.periodType = XMLUtil.tryGetIntAttr(var2, "period");
      if (this.periodType < 0) {
         this.periodType = 20;
      }

      this.floating = XMLUtil.tryGetIntAttr(var2, "optimization") == 15;
      Element var3 = XMLUtil.getChildElem(var2, "Param1");
      Element var4 = XMLUtil.getChildElem(var2, "Param2");
      if (this.type == 2) {
         this.param1Start = XMLUtil.tryGetIntAttr(var3, "value");
         this.param1Stop = this.param1Start;
         this.param1Step = 0;
         this.param2Start = XMLUtil.tryGetIntAttr(var4, "value");
         this.param2Stop = this.param2Start;
         this.param2Step = 0;
      } else {
         this.param1Start = XMLUtil.tryGetIntAttr(var3, "start");
         this.param1Stop = XMLUtil.tryGetIntAttr(var3, "stop");
         this.param1Step = XMLUtil.tryGetIntAttr(var3, "step");
         this.param2Start = XMLUtil.tryGetIntAttr(var4, "start");
         this.param2Stop = XMLUtil.tryGetIntAttr(var4, "stop");
         this.param2Step = XMLUtil.tryGetIntAttr(var4, "step");
      }
   }

   public void verify() throws BadOptimizationParamsException {
      if (this.isOptimizationConfigurable()) {
         this.verifyStandard();
      } else {
         this.verifyStandard();
      }
   }

   public void verifyStandard() throws BadOptimizationParamsException {
      if (this.type != 1 && this.type != 4) {
         String var1 = "";
         if (this.type == 3) {
            var1 = "start";
         }

         if (this.periodType == 10) {
            this.checkInRange(this.param1Start, 1, 95, String.format("Misconfiguration of Walk-Forward - OOS period %% %s ", var1));
            this.checkInRange(this.param2Start, 1, 95, String.format("Misconfiguration of Walk-Forward - runs %s ", var1));
         } else if (this.periodType == 20) {
            this.checkInRange(this.param1Start, 5, 100000, String.format("Misconfiguration of Walk-Forward - In Sample days  %s ", var1));
            this.checkInRange(this.param2Start, 5, 100000, String.format("Misconfiguration of Walk-Forward - Out Of Sample days %s ", var1));
         } else {
            this.checkInRange(this.param1Start, 5, 500000, String.format("Misconfiguration of Walk-Forward - In Sample bars  %s ", var1));
            this.checkInRange(this.param2Start, 5, 500000, String.format("Misconfiguration of Walk-Forward - Out Of Sample bars %s ", var1));
         }

         if (this.type == 3) {
            if (this.periodType == 10) {
               this.checkInRange(this.param1Stop, 1, 95, "Misconfiguration of Walk-Forward - OOS period % stop");
               this.checkInRange(this.param2Stop, 1, 95, "Misconfiguration of Walk-Forward - runs stop");
            } else if (this.periodType == 20) {
               this.checkInRange(this.param1Stop, 5, 100000, "Misconfiguration of Walk-Forward - In Sample days stop");
               this.checkInRange(this.param2Stop, 5, 100000, "Misconfiguration of Walk-Forward - Out Of Sample days stop");
            } else {
               this.checkInRange(this.param1Stop, 5, 500000, "Misconfiguration of Walk-Forward - In Sample bars stop");
               this.checkInRange(this.param2Stop, 5, 500000, "Misconfiguration of Walk-Forward - Out Of Sample bars stop");
            }

            if (this.periodType == 10) {
               this.checkInRange(this.param1Step, 1, 95, "Misconfiguration of Walk-Forward - OOS period % step");
               this.checkInRange(this.param2Step, 1, 95, "Misconfiguration of Walk-Forward - runs step");
            } else if (this.periodType == 20) {
               this.checkInRange(this.param1Step, 5, 5000, "Misconfiguration of Walk-Forward - In Sample days step");
               this.checkInRange(this.param2Step, 5, 5000, "Misconfiguration of Walk-Forward - Out Of Sample days step");
            } else {
               this.checkInRange(this.param1Step, 5, 50000, "Misconfiguration of Walk-Forward - In Sample bars step");
               this.checkInRange(this.param2Step, 5, 50000, "Misconfiguration of Walk-Forward - Out Of Sample bars step");
            }

            int var2 = Math.abs((this.param1Stop - this.param1Start) / this.param1Step + 1);
            if (this.periodType == 10) {
               var1 = "OOS period %";
            } else if (this.periodType == 20) {
               var1 = "In Sample days";
            } else {
               var1 = "In Sample bars";
            }

            this.checkInRange(var2, 1, 100, String.format("Misconfiguration of Walk-Forward - too many steps for %s", var1));
            var2 = Math.abs((this.param2Stop - this.param2Start) / this.param2Step + 1);
            if (this.periodType == 10) {
               var1 = "runs";
            } else if (this.periodType == 20) {
               var1 = "Out Of Sample days";
            } else {
               var1 = "Out Of Sample bars";
            }

            this.checkInRange(var2, 1, 100, String.format("Misconfiguration of Walk-Forward - too many steps for %s", var1));
         }
      }
   }

   private void checkInRange(int var1, int var2, int var3, String var4) throws BadOptimizationParamsException {
      if (var1 < var2) {
         throw new BadOptimizationParamsException(String.format("%s must be bigger than %d !", var4, var2));
      }

      if (var1 > var3) {
         throw new BadOptimizationParamsException(String.format("%s must be smaller than %d !", var4, var3));
      }
   }

   public short[] transformToFullIndexes(short[] var1) {
      short[] var2 = new short[this.parameters.size()];
      int var3 = 0;

      for (int var4 = 0; var4 < this.parameters.size(); var4++) {
         Parameter var5 = this.parameters.get(var4);
         if (!var5.isUsed()) {
            var2[var4] = 0;
         } else {
            var2[var4] = var1[var3];
            var3++;
         }
      }

      return var2;
   }

   public OptimizationSettings clone() {
      OptimizationSettings var1 = new OptimizationSettings(this.projectName);
      var1.type = this.type;
      var1.singleStrategy = this.singleStrategy;
      var1.storeAll = this.storeAll;
      var1.optimizationType = this.optimizationType;
      var1.periodType = this.periodType;
      var1.floating = this.floating;
      var1.param1Start = this.param1Start;
      var1.param1Stop = this.param1Stop;
      var1.param1Step = this.param1Step;
      var1.param2Start = this.param2Start;
      var1.param2Stop = this.param2Stop;
      var1.param2Step = this.param2Step;
      var1.strategyFile = this.strategyFile;
      var1.parameters = this.parameters.clone();
      var1.optimizationMethod = this.optimizationMethod;
      var1.inputDatabank = this.inputDatabank;
      var1.outputDatabank = this.outputDatabank;
      var1.maxOptimizationBacktests = this.maxOptimizationBacktests;
      var1.elConditions = this.elConditions;
      var1.conditions = this.conditions;
      var1.thresholdPct = this.thresholdPct;
      var1.robCombRows = this.robCombRows;
      var1.robCombCols = this.robCombCols;
      var1.robMinComb = this.robMinComb;
      var1.singleThreaded = this.singleThreaded;
      var1.deleteFailedStr = this.deleteFailedStr;
      var1.dontSaveOriginalStr = this.dontSaveOriginalStr;
      var1.debugWFFitnessPath = this.debugWFFitnessPath;
      return var1;
   }

   public boolean isOptimizationConfigurable() {
      return this.taskName != null && this.taskName.equals("OptimizeConfigurable");
   }

   public String getProjectLogMessage() {
      return this.projectLogMessage;
   }
}
