package com.strategyquant.plugin.CrossCheck.impl.SequentialOptimization;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.ISequentialOptimizationCompleteListener;
import com.strategyquant.tradinglib.optimization.SequentialOptimizationEngine;
import com.strategyquant.tradinglib.optimization.SequentialOptimizationSettings;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.robustnesstests.SequentialOptimizationResults;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

@PluginImplementation
public class SequentialOptimization extends WalkForwardCrossCheckMethod implements ILastEventListener {
   private IFitnessFunction fitnessFunction;
   private ChartSetups chartSetups;
   private SequentialOptimizationSettings seqOptSettings = new SequentialOptimizationSettings();

   public SequentialOptimization() {
      super("SequentialOptimization_CrossCheck");
   }

   public String getName() {
      return NameSequentialOptimization;
   }

   public String getShortName() {
      return L.tsq("Sequential Optimization");
   }

   public String getDescription() {
      return L.tsq("This cross check performs sequential optimization of the strategy and finds stable parameters.");
   }

   public String getSettingName() {
      return this.getClass().getSimpleName();
   }

   public int getType() {
      return 1;
   }

   public int getPreferredPosition() {
      return 300;
   }

   public int getNumberOfSimulations() {
      return 0;
   }

   public boolean doesRetest() {
      return true;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public void fixSettings(Element var1) {
      super.fixSettings(var1);
      Element var2 = XMLUtil.tryAddElement(var1, "Settings");
      Element var3 = XMLUtil.tryAddElement(var2, "ParameterSettings");
      XMLUtil.tryAddIntNode(var3, "DistributionUp", 50);
      XMLUtil.tryAddIntNode(var3, "DistributionDown", 50);
      XMLUtil.tryAddIntNode(var3, "Steps", 50);
      XMLUtil.tryAddBooleanNode(var3, "ApplyToStrategy", false);
      Element var4 = XMLUtil.tryAddElement(var1, "AcceptanceSettings");
      XMLUtil.tryAddIntNode(var4, "PctToPass", 80);
      XMLUtil.tryAddIntNode(var4, "ResultsCount", 5);
      XMLUtil.tryAddIntNode(var4, "StabilityRange", 30);
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getParentElement().getParentElement();
      Element var4 = var3.getChild("Rankings");
      this.fitnessFunction = ProjectConfigHelper.getFitnessFunction(var4);
      Element var5 = var3.getChild("Data");
      this.chartSetups = ProjectConfigHelper.getChartSetups(var5);
      Element var6 = XMLUtil.getChildElem(var1, "Settings");
      Element var7 = var6.getChild("WhatToParametrize");
      this.readWhatToParametrize(var7);
      Element var8 = XMLUtil.getChildElem(var6, "ParameterSettings");
      this.seqOptSettings.distributionUp = XMLUtil.getNodeIntValue(var8, "DistributionUp");
      this.seqOptSettings.distributionDown = XMLUtil.getNodeIntValue(var8, "DistributionDown");
      this.seqOptSettings.steps = XMLUtil.getNodeIntValue(var8, "Steps");
      this.seqOptSettings.applyToStrategy = XMLUtil.getNodeBooleanValue(var8, "ApplyToStrategy", false);
      Element var9 = XMLUtil.getChildElem(var1, "AcceptanceSettings");
      this.seqOptSettings.pctToPass = XMLUtil.getNodeIntValue(var9, "PctToPass");
      this.seqOptSettings.stableAreaCount = XMLUtil.getNodeIntValue(var9, "ResultsCount");
      this.seqOptSettings.stabilityRangePct = XMLUtil.getNodeIntValue(var9, "StabilityRange");
      this.seqOptSettings.paramTypes = this.paramTypes;
      this.seqOptSettings.symmetry = this.paramSymmetry;
   }

   public boolean runTest(final ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      this.checkSpecialTrialLimitation();
      SequentialOptimizationEngine var9 = new SequentialOptimizationEngine(this.seqOptSettings, this.stopPauseEngine, var7, null);
      String var10 = var1.getMainResultKey();
      Result var11 = var1.subResult(var10);
      final double var12 = var11.getFitness((byte)127);
      if (var7 != null) {
         String var14 = L.t("Running sequential optimization for %s", new Object[]{var8});
         var7.setLastEvent(var14);
      }

      SettingsMap var17 = var11.getSettings().clone();
      ChartSetups var15 = this.chartSetups;
      if (var15 == null) {
         var15 = new ChartSetups((ChartSetup)var17.get("BacktestChart"));
      }

      var17.set("ChartSetups", var15);
      var17.set("FitnessFunction", this.fitnessFunction);
      var9.initialize(var1.getName(), var1.getStrategyXml().clone(), var17);
      boolean var16 = var9.optimize(false, var6, var5, new ISequentialOptimizationCompleteListener() {
         public void onFinish(boolean var1x, StrategyBase var2x, SequentialOptimizationResults var3x, ResultsGroup var4) {
            try {
               if (var1x) {
                  if (SequentialOptimization.this.seqOptSettings.applyToStrategy) {
                     var1.mainResult().addStrategy(var2x);
                     var1.mainResult().addStrategyXml(var2x.getStrategyXml());
                  }

                  if (var4 != null) {
                     String var5x = "CrossCheck_SequentialOptimization";
                     var4.setName(var5x);
                     var1.mergeWith(var4);
                     var1.createPortfolioResult();
                     var1.portfolio().setFitness((byte)127, var12);
                     var1.portfolio().setFitness((byte)10, var12);
                     var1.portfolio().setFitness((byte)20, var12);
                     var1.portfolio().setFitness((byte)11, var12);
                     var1.portfolio().setFitness((byte)40, var12);
                  }
               }

               var1.mainResult().setSequentialOptimizationResults(var3x);
            } catch (Exception var6x) {
               CrossCheckMethod.Log.error("Updating sequential optimized strategy failed", var6x);
            }
         }
      });
      if (!var16) {
         this.dismissalMessage = var9.getDismissalMessage();
         this.dismissalReason = 10039;
         var1.specialValues().setString(SpecialValues.FiltersResultFailedReason, this.dismissalMessage);
      }

      return var16;
   }

   protected boolean checkConditions(ResultsGroup var1, int var2) {
      return true;
   }

   public String getColumnTitleTemplate() {
      return "#columnName# (" + this.getShortName() + ", #sampleType#, #direction#, #plType#)";
   }

   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (!var1.hasResult("CrossCheck_SequentialOptimization")) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      byte var5 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var6 = this.getSampleType(var3, var4);
      byte var7 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getNumericValue(var1, "CrossCheck_SequentialOptimization", var5, var7, var6);
   }

   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      return var1.hasResult("CrossCheck_SequentialOptimization");
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      return new ChartSetups(var1);
   }

   public ICrossCheck clone(SettingsMap var1) {
      SequentialOptimization var2 = new SequentialOptimization();
      var2.seqOptSettings = this.seqOptSettings != null ? this.seqOptSettings.clone() : null;
      var2.fitnessFunction = this.fitnessFunction;
      var2.chartSetups = this.chartSetups != null ? this.chartSetups.getClone() : null;
      return var2;
   }

   public String printSettings(Element var1) throws Exception {
      if (var1 == null) {
         throw new NullPointerException();
      }

      if (!XMLUtil.elementIs(var1, "use")) {
         return "Not used";
      }

      Element var2 = var1.getChild("Settings");
      Element var3 = XMLUtil.getChildElem(var2, "ParameterSettings");
      int var4 = XMLUtil.getNodeIntValue(var3, "DistributionUp");
      int var5 = XMLUtil.getNodeIntValue(var3, "DistributionDown");
      int var6 = XMLUtil.getNodeIntValue(var3, "Steps");
      boolean var7 = XMLUtil.getNodeBooleanValue(var3, "ApplyToStrategy", true);
      String var8 = L.t("Distribution: Up: %d%%, Down: %d%%, Steps: %d, Apply to Strategy: %s", new Object[]{var4, var5, var6, var7});
      Element var9 = XMLUtil.getChildElem(var1, "AcceptanceSettings");
      int var10 = XMLUtil.getNodeIntValue(var9, "PctToPass");
      int var11 = XMLUtil.getNodeIntValue(var9, "ResultsCount");
      int var12 = XMLUtil.getNodeIntValue(var9, "StabilityRange");
      return var8 + "\n" + L.t("Filtering: Parameters to pass: %d%%, Stable area results: %d, Stability range: %d%%", new Object[]{var10, var11, var12});
   }

   public void saveDataToFile(ResultsGroup var1, JarOutputStream var2) throws Exception {
      String var3 = var1.getMainResultKey();
      Result var4 = var1.mainResult();
      var3 = SQUtils.correctResultKeyToDirname(var3);
      SequentialOptimizationResults var6 = var4.getSequentialOptimizaionResults();
      if (var6 != null) {
         Element var7 = var6.toXML();
         JarEntry var5 = new JarEntry("Results/" + var3 + "/" + this.getSettingName() + "_Results.xml");
         var2.putNextEntry(var5);
         Document var8 = new Document(var7);
         BufferedWriter var9 = new BufferedWriter(new OutputStreamWriter(var2, "UTF8"));
         XMLOutputter var10 = new XMLOutputter(Format.getPrettyFormat());
         var10.output(var8, var9);
      }
   }

   public void loadDataFromFile(ResultsGroup var1, JarFile var2) throws Exception {
      String var3 = var1.getMainResultKey();
      Result var4 = var1.mainResult();
      var3 = SQUtils.correctResultKeyToDirname(var3);
      JarEntry var5 = var2.getJarEntry("Results/" + var3 + "/" + this.getSettingName() + "_Results.xml");
      if (var5 != null) {
         Element var6 = XMLUtil.readXmlFile(var2.getInputStream(var5));
         SequentialOptimizationResults var7 = new SequentialOptimizationResults().fromXML(var6);
         var4.setSequentialOptimizationResults(var7);
      }
   }

   public int getBadStrategyReason() {
      return 10039;
   }

   public boolean doesCreateSubjobs() {
      return true;
   }

   public void setLastEvent(String var1) {
   }

   protected SettingsMap prepareSettings(String var1, SettingsMap var2, Element var3, boolean var4) throws Exception {
      return null;
   }

   public boolean disabledForSpecialTrial() {
      return true;
   }
}
