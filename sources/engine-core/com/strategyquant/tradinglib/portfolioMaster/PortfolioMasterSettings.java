package com.strategyquant.tradinglib.portfolioMaster;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioMasterSettings implements Serializable {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioMasterSettings.class);
   public static String PMFitnessValue = "PMFitnessValue";
   public boolean useBruteForce;
   public int populationSize;
   public int generations;
   public boolean runContinually;
   public boolean restartAfterStagnation;
   public int generationLimit;
   public BigInteger numberOfPossiblePortfolios;
   public Databank databankSource;
   public Databank databankTarget;
   public int maxStrategiesInDatabank;
   public boolean limitDateRange;
   public long dateRangeFrom;
   public long dateRangeTo;
   public int IsPct;
   public boolean reverseSample;
   public int maxStrategiesSector;
   public int minStrategiesInPortfolio;
   public int maxStrategiesInPortfolio;
   public PortfolioMasterFitness fitnessFunction;
   public byte fitnessSampleType;
   public double maxCorrelation;
   public CorrelationType correlationType;
   public byte correlationSampleType;
   public int correlationPeriod;
   public boolean allowNegativeCorrelation;
   public boolean addEmptyPeriods;
   public ArrayList<Condition> conditions = null;
   public ArrayList<String> selectedKeys = null;
   public float initialCapital;
   public boolean mmMethodOptional;
   public MoneyManagementMethod mmMethod;

   public void loadFromXml(Element var1, String var2) throws Exception {
      String var3 = XMLUtil.getNodeValue(var1, "SearchType", "bruteforce");
      this.useBruteForce = var3.equals("bruteforce");
      this.databankSource = ProjectConfigHelper.getInputDatabank(var2, var1.getParentElement());
      if (this.databankSource == null) {
         throw new Exception(L.t("Source databank not found", new Object[0]));
      }

      this.databankTarget = ProjectConfigHelper.getOutputDatabank(var2, var1.getParentElement());
      if (this.databankTarget == null) {
         throw new Exception(L.t("Target databank not found", new Object[0]));
      }

      Element var4 = var1.getParentElement().getChild("Databanks");
      if (var4 == null) {
         throw new Exception("Databanks element not found");
      }

      boolean var5 = var4.getAttributeValue("retestSelected", "false").equals("true");
      if (var5) {
         Element var6 = XMLUtil.getChildElem(var1.getParentElement(), "SelectedStrategies");
         ArrayList var7 = new ArrayList();
         List var8 = var6.getChildren("Strategy");

         for (int var9 = 0; var9 < var8.size(); var9++) {
            Element var10 = (Element)var8.get(var9);
            var7.add(var10.getText());
         }

         this.selectedKeys = var7;
      }

      this.maxStrategiesInDatabank = XMLUtil.getInt(var1, "MaxStrategiesDatabank", 100);
      this.populationSize = XMLUtil.getInt(var1, "GenPopulationSize", 100);
      this.generations = XMLUtil.getInt(var1, "GenGenerations", 700);
      this.runContinually = XMLUtil.getBoolean(var1, "GenRunContinually", false);
      this.restartAfterStagnation = XMLUtil.getBoolean(var1, "GenRestartAfterStagnation", true);
      this.generationLimit = XMLUtil.getInt(var1, "GenGenerationLimit", 500);
      this.minStrategiesInPortfolio = XMLUtil.getInt(var1, "MinStrategies", 2);
      this.maxStrategiesInPortfolio = XMLUtil.getInt(var1, "MaxStrategies", 8);
      this.maxStrategiesSector = XMLUtil.getInt(var1, "MaxStrategiesSector", 3);
      String var12 = XMLUtil.getNodeValue(var1, "DateRange", "full");
      this.limitDateRange = var12.equals("limited");
      if (this.limitDateRange) {
         this.dateRangeFrom = XMLUtil.parseDate(XMLUtil.getNodeValue(var1, "DateRangeFrom", "0"));
         this.dateRangeTo = XMLUtil.parseDate(XMLUtil.getNodeValue(var1, "DateRangeTo", "0"));
      }

      this.IsPct = XMLUtil.getInt(var1, "DateRangeIsPct", 70);
      this.reverseSample = XMLUtil.getBoolean(var1, "DateRangeReverse", false);
      this.maxCorrelation = XMLUtil.getDouble(var1, "CorrMax", 0.3);
      String var13 = XMLUtil.getNodeValue(var1, "CorrType", "ProfitLoss");
      this.correlationType = (CorrelationType)CorrelationTypes.getInstance().findClassByName(var13);
      this.correlationPeriod = XMLUtil.getInt(var1, "CorrPeriod", 10);
      this.correlationSampleType = XMLUtil.getByte(var1, "CorrSampleType", (byte)10);
      this.allowNegativeCorrelation = XMLUtil.getBoolean(var1, "CorrAllowNegative", true);
      this.addEmptyPeriods = XMLUtil.getBoolean(var1, "CorrAddEmptyPeriods", false);
      this.fitnessSampleType = 10;
      String var14 = XMLUtil.getNodeValue(var1, "FitnessType", "NetProfitIS");
      if (var14.endsWith(PortfolioMasterFitness.SampleKeyIS)) {
         var14 = SQUtils.replaceLast(var14, PortfolioMasterFitness.SampleKeyIS, "");
      } else if (var14.endsWith(PortfolioMasterFitness.SampleKeyFull)) {
         var14 = SQUtils.replaceLast(var14, PortfolioMasterFitness.SampleKeyFull, "");
         this.fitnessSampleType = 127;
      }

      this.fitnessFunction = (PortfolioMasterFitness)PortfolioMasterFitnessList.getInstance().findClassByName(var14);
      this.conditions = ProjectConfigHelper.getConditions(var1.getChild("Conditions"));
      this.mmMethodOptional = XMLUtil.getBoolean(var1, "MMMethodOptional", false);

      try {
         Element var15 = var1.getChild("MoneyManagement");
         if (var15 == null) {
            throw new Exception("MoneyManagement method missing in XML, null value.");
         }

         this.mmMethod = ProjectConfigHelper.getMoneyManagement(var15);
      } catch (Exception var11) {
         Log.error("Error loading Money Management method, using FixedSize(0.1) - " + var11.getMessage(), var11);
         this.mmMethod = MoneyManagementMethodsList.create("FixedSize", 0.1);
      }

      this.initialCapital = (float)this.mmMethod.getInitialCapital();
   }

   public ArrayList<String> getKeys() throws Exception {
      ArrayList var1;
      if (this.selectedKeys != null) {
         var1 = this.selectedKeys;
      } else {
         var1 = this.databankSource.getRecordKeys();
      }

      if (!MainApp.v571hfnsHw().aDm88fRJB2() && var1.size() > 4) {
         throw new Exception(
            L.t(
               "Portfolio Master in Pro version is a trial, and limited to maximum 4 source strategies for portfolio builder. Upgrade your license to Ultimate version to build portfolio from more strategies.",
               new Object[0]
            )
         );
      } else {
         return var1;
      }
   }
}
