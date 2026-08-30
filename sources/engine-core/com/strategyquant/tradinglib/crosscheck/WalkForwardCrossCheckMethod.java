package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.databank.DatabankColumnTypes;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.BadOptimizationParamsException;
import com.strategyquant.tradinglib.optimization.INewResultListener;
import com.strategyquant.tradinglib.optimization.IStepsListener;
import com.strategyquant.tradinglib.optimization.OptimizationEngine;
import com.strategyquant.tradinglib.optimization.OptimizationParams;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.optimization.WalkForwardParametersStability;
import com.strategyquant.tradinglib.project.ILastEventListener;
import java.util.List;
import org.jdom2.Element;

public abstract class WalkForwardCrossCheckMethod extends CrossCheckMethod {
   protected static final String SeqOpt_CROSSCHECK = "SequentialOptimization_CrossCheck";
   protected static final String WFO_CROSSCHECK = "WalkForwardOptimization_CrossCheck";
   protected static final String WFM_CROSSCHECK = "WalkForwardMatrix_CrossCheck";
   protected OptimizationSettings optimizationSettings;
   protected ValuesMap paramTypes = new ValuesMap();
   protected boolean paramSymmetry = true;
   protected Element elRankings;
   private String taskName;

   public WalkForwardCrossCheckMethod(String var1) {
      this.taskName = var1;
      this.optimizationSettings = new OptimizationSettings(var1);
      this.paramTypes.set("ParamTypeRecommended", true);
   }

   @Override
   public String getSettingName() {
      return this.getClass().getSimpleName();
   }

   @Override
   public int getType() {
      return 2;
   }

   @Override
   public int getNumberOfSimulations() {
      return 0;
   }

   @Override
   public boolean doesRetest() {
      return true;
   }

   @Override
   public boolean doesForEverySetup() {
      return false;
   }

   @Override
   public boolean runTest(final ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      String var9 = var1.getMainResultKey();
      Result var10 = var1.subResult(var9);
      SettingsMap var11 = var10.getSettings();
      Element var12 = var10.getStrategyXml();
      SettingsMap var13 = this.prepareSettings(var1.getName(), var11, var12, var6);
      OptimizationEngine var14 = new OptimizationEngine(this.taskName, this.stopPauseEngine, var5, "WFCC", var7, null);
      var14.initialize(var13);
      var14.setNewResultListener(new INewResultListener() {
         @Override
         public void newResult(ResultsGroup var1x) throws Exception {
            WalkForwardCrossCheckMethod.this.processResult(var1, var1x);
         }
      });
      var14.setStepsListener(new IStepsListener() {
         @Override
         public void step(int var1) {
            WalkForwardCrossCheckMethod.this.backtestProgressListener.increaseProgressStep();
         }
      });

      try {
         var14.optimize();
      } catch (BadOptimizationParamsException var16) {
         if (var16.getMessage().equals("Misconfiguration - no parameter was chosen to be optimized")) {
            Log.debug("CrossCheck Optimization exception", var16);
            return true;
         }

         throw var16;
      }

      return this.checkConditions(var1, var2);
   }

   protected void processResult(ResultsGroup var1, ResultsGroup var2) {
      try {
         Result var3 = var2.mainResult();
         Result var4 = var1.mainResult();
         WalkForwardMatrixResult var5 = (WalkForwardMatrixResult)var3.get("WalkForwardResult");
         if (var5 != null) {
            var4.set("WalkForwardResult", var5);
            var1.specialValues().set("WalkForwardConditions", var2.specialValues().get("WalkForwardConditions"));
         }

         List var6 = var2.getResultKeys();
         OrdersList var7 = var2.orders();

         for (int var8 = 0; var8 < var7.size(); var8++) {
            Order var9 = var7.get(var8);
            var9.IsInPortfolio = 0;
         }

         for (int var11 = 0; var11 < var6.size(); var11++) {
            String var12 = (String)var6.get(var11);
            if (var12.startsWith("WF:")) {
               var1.moveResultFrom(var12, var2, true);
            }
         }

         var2.orders().clear();
         WalkForwardParametersStability.compute(var1, this.optimizationSettings.parameters);
      } catch (Exception var10) {
         Log.info("Error processing WF Optimization result", var10);
      }
   }

   protected OptimizationParams initializeParams(StrategyBase var1, int var2, int var3, int var4) {
      OptimizationParams var5 = new OptimizationParams();

      for (Variable var7 : var1.variables()) {
         if (!var7.getName().startsWith("Magic")
            && !var7.getName().contains("EntrySignal")
            && !var7.getName().contains("ExitSignal")
            && !var7.getName().contains("ReplaceExistingOrders")
            && var7.getInternalType() != 3) {
            var5.addParam(ParametersSettings.createParameter(var7, var2, var3, var4));
         }
      }

      if (Log.isDebugEnabled()) {
         Log.debug("Variables count: {}", var1.variables().size());
      }

      int var9 = 1;

      for (Parameter var8 : var5) {
         var9 *= var8.getCombinationsCount();
         if (var9 == 0) {
            break;
         }
      }

      Log.debug("Total combinations: {}", var9);
      return var5;
   }

   @Override
   protected boolean checkConditions(ResultsGroup var1, int var2) {
      try {
         WalkForwardMatrixResult var3 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
         if (var3 != null && !var3.passed) {
            this.dismissalReason = 300000 + var2 * 1000;
            this.dismissalMessage = String.format("Cross Check filter in '%s': %s", this.getName(), var3.dismissalMessage);
            return false;
         }
      } catch (Exception var4) {
         Log.error("Error while evaluating WF conditions. Exc.", var4);
      }

      return true;
   }

   @Override
   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      Object var5 = null;
      if (var4.length > 1 && var4[1] instanceof WalkForwardResult) {
         var5 = (WalkForwardResult)var4[1];
      } else {
         WalkForwardMatrixResult var6 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
         if (var6 == null) {
            throw new CrossCheckDataNotExistException(this.getName());
         }

         var5 = var6.getWFResult(var1.getBestWFResultKey(), true);
         if (var5 == null) {
            var5 = var6.getResultList().get(0);
         }
      }

      byte var14 = XMLUtil.getByteAttr(var3, "subresult", (byte)30);
      byte var7 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var8 = XMLUtil.getByteAttr(var3, "sampleType", (byte)127);
      byte var9 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      if (WalkForwardColumns.get().checkClassExists(var2)) {
         var14 = 33;
      }

      switch (var14) {
         case 30:
            DatabankColumn var16 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
            SQStats var11 = var1.subResult(((WalkForwardResult)var5).resultName).stats(var7, var9, var8);
            return var16.getNumericValue(var11);
         case 31:
            DatabankColumn var15 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
            return var15.getNumericValue(((WalkForwardResult)var5).statsStability);
         case 32:
            DatabankColumn var10 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
            return var10.getNumericValue(((WalkForwardResult)var5).statsScore);
         case 33:
            WalkForwardColumn var12 = (WalkForwardColumn)WalkForwardColumns.get().findClassByName(var2);
            return var12.getNumericValue(((WalkForwardResult)var5).statsSpecial);
         default:
            return 0.0;
      }
   }

   @Override
   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (var4.length > 1 && var4[1] instanceof WalkForwardResult) {
         return true;
      }

      WalkForwardMatrixResult var5 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
      return var5 != null;
   }

   @Override
   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      WalkForwardMatrixResult var5 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
      if (var5 == null) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      DatabankColumn var6 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var6.getValue(var1, var1.getMainResultKey(), (byte)0, (byte)10, (byte)127);
   }

   @Override
   public String getColumnTitle(String var1, Element var2, Object... var3) {
      byte var4 = XMLUtil.getByteAttr(var2, "subresult", (byte)30);
      String var5 = this.getColumnTitleTemplate();
      if (var5 != null && var1 != null) {
         var5 = var5.replace("#columnName#", var1);
         var5 = var5.replace("#subresult#", DatabankColumnTypes.typeToString(var4));
      }

      return var5;
   }

   @Override
   public String getColumnTitleTemplate() {
      return "#subresult# #columnName#";
   }

   @Override
   public boolean doesCreateSubjobs() {
      return true;
   }

   protected void readWhatToParametrize(Element var1) {
      boolean var2 = true;

      try {
         var2 = XMLUtil.getNodeValue(var1, "Recommended", "true").equals("true");
      } catch (Exception var4) {
      }

      if (var2) {
         this.paramTypes.set("ParamTypeRecommended", true);
         this.paramSymmetry = true;
      } else {
         this.paramTypes.set("ParamTypeRecommended", false);
         this.paramTypes.set("ParamTypePeriod", XMLUtil.getNodeValue(var1, "Periods", "false").equals("true"));
         this.paramTypes.set("ParamTypeShift", XMLUtil.getNodeValue(var1, "Shifts", "false").equals("true"));
         this.paramTypes.set("ParamTypeConstant", XMLUtil.getNodeValue(var1, "Constants", "false").equals("true"));
         this.paramTypes.set("ParamTypeOtherParam", XMLUtil.getNodeValue(var1, "OtherParams", "false").equals("true"));
         this.paramTypes.set("ParamTypeExitUsed", XMLUtil.getNodeValue(var1, "ExitParamsUsed", "false").equals("true"));
         this.paramTypes.set("ParamTypeExitUnused", XMLUtil.getNodeValue(var1, "ExitParamsUnused", "false").equals("true"));
         this.paramTypes.set("ParamTypeBoolean", XMLUtil.getNodeValue(var1, "BooleanParams", "false").equals("true"));
         this.paramTypes.set("ParamTypeTradingOptions", XMLUtil.getNodeValue(var1, "TradingOptions", "false").equals("true"));
         this.paramTypes.set("ParamTypeEntryLevel", XMLUtil.getNodeValue(var1, "EntryParams", "false").equals("true"));
         this.paramTypes.set("ParamTypeEntryLogic", XMLUtil.getNodeValue(var1, "EntryLogic", "false").equals("true"));
         this.paramSymmetry = XMLUtil.elementIs(var1, "symmetricVariables");
      }
   }

   @Override
   public abstract ICrossCheck clone(SettingsMap var1);

   protected abstract SettingsMap prepareSettings(String var1, SettingsMap var2, Element var3, boolean var4) throws Exception;
}
