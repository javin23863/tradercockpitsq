package com.strategyquant.tradinglib.project;

import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.lib.volumeProfile.VolumeProfileBlocks;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.ResultTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.commissions.CommissionsMethodsList;
import com.strategyquant.tradinglib.conditions.ColumnConditionValue;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.IConditionValue;
import com.strategyquant.tradinglib.conditions.NumericValue;
import com.strategyquant.tradinglib.crosscheck.CrossCheckConditionValue;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.IDatabankListener;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.exception.StrategyProblem;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.montecarlo.manipulation.MonteCarloManipulationList;
import com.strategyquant.tradinglib.montecarlo.retest.MonteCarloRetestList;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.riskmanagement.RiskManagementMethodsList;
import com.strategyquant.tradinglib.robustnesstests.RobustnessTestMethod;
import com.strategyquant.tradinglib.robustnesstests.RobustnessTestMethodsList;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.tradinglib.taskImpl.TaskException;
import com.strategyquant.tradinglib.whatif.WhatIfMethodsList;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectConfigHelper {
   private static final Logger Log = LoggerFactory.getLogger("ProjectConfigHelper");

   public static ChartSetups getChartSetups(Element var0) throws Exception {
      ChartSetups var1 = new ChartSetups();
      if (var0 == null) {
         return null;
      }

      Element var2 = var0.getChild("Setups");
      List var3 = var2.getChildren("Setup");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         var1.add(getChartSetup(var5));
      }

      return var1;
   }

   private static ChartSetup getChartSetup(Element var0) throws Exception {
      List var1 = var0.getChildren("Chart");
      Element var2 = (Element)var1.get(0);
      String var3 = XMLUtil.removeXMLCharacters(var2.getAttributeValue("symbol"));
      String var4 = var2.getAttributeValue("timeframe");
      long var5 = SQTimeOld.parseDateToMilis(var0.getAttributeValue("dateFrom"));
      long var7 = SQTimeOld.parseDateToMilis(var0.getAttributeValue("dateTo"));
      double var9 = Double.parseDouble(var2.getAttributeValue("spread"));
      int var11 = Engines.getEngine(var0.getAttributeValue("engine"));
      if (var11 == -1) {
         throw new Exception(L.t("Trading engine not recognized %d", new Object[]{var11}));
      }

      if (Engines.isTradestationEngine(var11)) {
         if (var4.equals("H1")) {
            var4 = "M60";
         } else if (var4.equals("H2")) {
            var4 = "M120";
         } else if (var4.equals("H4")) {
            var4 = "M240";
         }
      }

      String var12 = "No Session";
      if (Engines.isTradestationEngine(var11)) {
         var12 = var0.getAttributeValue("session");
         if (SessionManager.getSession(var12) == null) {
            Log.debug("Session '{}' doesn't exist, using NoSession.", var12);
            var12 = "No Session";
         }
      }

      ChartSetup var13 = null;

      try {
         var13 = new ChartSetup(var3, var4, var5, var7, var9, var12);
         var13.setTestPrecision(getPrecision(var0.getAttributeValue("testPrecision")));
         var13.setCommissionsMethod(getCommissionsMethod(var0));
      } catch (DataException var21) {
         Log.debug("No data for symbol '{}' found, using first available data", var3);
         var13 = ChartSetup.createFirstAvailableData();
         var13.setTestPrecision(getPrecision(var0.getAttributeValue("testPrecision")));
         var13.setCommissionsMethod(getCommissionsMethod(var0));
      }

      var13.setBacktestEngine(var11);
      if (var1.size() > 1) {
         for (int var14 = 1; var14 < var1.size(); var14++) {
            Element var15 = (Element)var1.get(var14);
            String var16 = "History";
            String var17 = XMLUtil.removeXMLCharacters(var15.getAttributeValue("symbol"));
            String var18 = var15.getAttributeValue("timeframe");
            double var19 = Double.parseDouble(var15.getAttributeValue("spread"));
            var17 = var17.equals("Same as main chart") ? var3 : var17;
            var13.addChart(var16, var17, var18, var19);
         }
      }

      return var13;
   }

   public static ArrayList<String> getChartSetupValues(Element var0, String var1) throws Exception {
      ArrayList var2 = new ArrayList();
      Element var3 = var0.getChild("Setups");
      List var4 = var3.getChildren("Setup");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         var2.add(var6.getAttributeValue(var1));
      }

      return var2;
   }

   public static ArrayList<CommissionsMethod> getChartSetupCommissions(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();
      Element var2 = var0.getChild("Setups");
      List var3 = var2.getChildren("Setup");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         var1.add(getCommissionsMethod(var5));
      }

      return var1;
   }

   public static ArrayList<SwapMethod> getChartSetupSwaps(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();
      Element var2 = var0.getChild("Setups");
      List var3 = var2.getChildren("Setup");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         var1.add(getSwapMethod(var5));
      }

      return var1;
   }

   public static ArrayList<String> getChartSetupMainTestValues(Element var0, String var1) throws Exception {
      ArrayList var2 = new ArrayList();
      Element var3 = var0.getChild("Setups");
      List var4 = var3.getChildren("Setup");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         var2.add(var6.getChild("MainTestValues").getAttributeValue(var1));
      }

      return var2;
   }

   private static int getPrecision(String var0) throws Exception {
      try {
         return Precisions.getPrecision(Integer.parseInt(var0));
      } catch (Exception var2) {
         return Precisions.getPrecision(var0);
      }
   }

   public static MoneyManagementMethod getMoneyManagement(Element var0) throws Exception {
      for (Element var2 : var0.getChildren("Method")) {
         if (XMLUtil.elementIs(var2, "use")) {
            String var3 = var2.getAttributeValue("type");
            MoneyManagementMethod var4 = (MoneyManagementMethod)MoneyManagementMethodsList.get().createNew(var3);
            if (var4 == null) {
               throw new Exception(L.t("MoneyManagement method with name '%s' doesn't exist.", new Object[]{var3}));
            }

            var4.setFromXML(var0);
            return var4;
         }
      }

      throw new Exception(L.t("MoneyManagement method not set.", new Object[0]));
   }

   public static RiskManagementMethod getRiskManagement(Element var0) throws Exception {
      for (Element var2 : var0.getChildren("Method")) {
         if (XMLUtil.elementIs(var2, "use")) {
            String var3 = var2.getAttributeValue("type");
            RiskManagementMethod var4 = (RiskManagementMethod)RiskManagementMethodsList.get().createNew(var3);
            if (var4 == null) {
               throw new Exception(L.t("RiskManagement method with name '%s' doesn't exist.", new Object[]{var3}));
            }

            var4.setFromXML(var0);
            return var4;
         }
      }

      throw new Exception(L.t("RiskManagement method not set.", new Object[0]));
   }

   public static ArrayList<MonteCarloManipulation> getMonteCarloManipulationMethods(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (Element var3 : var0.getChildren("Method")) {
         if (XMLUtil.elementIs(var3, "use")) {
            String var4 = var3.getAttributeValue("type");

            try {
               MonteCarloManipulation var5 = (MonteCarloManipulation)MonteCarloManipulationList.get().createNew(var4);
               var5.setFromXML(var3);
               var1.add(var5);
            } catch (Exception var6) {
               Log.error("Error while loading MonteCarloManipulation method '" + var4 + "'. " + var6.getMessage(), var6);
               throw new Exception(L.t("Error while loading MonteCarloManipulation method '%s' - %s ", new Object[]{var4, var6.getMessage()}));
            }
         }
      }

      if (var1.size() == 0) {
         throw new Exception(L.t("MonteCarloManipulation method not set.", new Object[0]));
      } else {
         return var1;
      }
   }

   public static ArrayList<MonteCarloRetest> getMonteCarloRetestMethods(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (Element var3 : var0.getChildren("Method")) {
         if (XMLUtil.elementIs(var3, "use")) {
            String var4 = var3.getAttributeValue("type");

            try {
               MonteCarloRetest var5 = (MonteCarloRetest)MonteCarloRetestList.get().createNew(var4);
               var5.setFromXML(var3);
               var1.add(var5);
            } catch (Exception var6) {
               Log.error("Error while loading MonteCarloRetest method '" + var4 + "'. " + var6.getMessage(), var6);
               throw new Exception(L.t("Error while loading MonteCarloRetest method '%s' - %s.", new Object[]{var4, var6.getMessage()}));
            }
         }
      }

      if (var1.size() == 0) {
         throw new Exception(L.t("MonteCarloRetest method not set.", new Object[0]));
      } else {
         return var1;
      }
   }

   public static ArrayList<WhatIf> getWhatIfMethods(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (Element var3 : var0.getChildren("Method")) {
         if (XMLUtil.elementIs(var3, "use")) {
            String var4 = var3.getAttributeValue("type");

            try {
               WhatIf var5 = (WhatIf)WhatIfMethodsList.get().createNew(var4);
               var5.setFromXML(var3);
               var1.add(var5);
            } catch (Exception var6) {
               Log.error("Error while loading WhatIf method '" + var4 + "'. " + var6.getMessage(), var6);
               throw new Exception(L.t("Error while loading WhatIf method '%s' - %s.", new Object[]{var4, var6.getMessage()}));
            }
         }
      }

      if (var1.size() == 0) {
         throw new Exception("WhatIf method not set.");
      } else {
         return var1;
      }
   }

   public static ChartSetups cloneChartSetups(ChartSetups var0) {
      if (var0 == null) {
         return null;
      }

      ChartSetups var1 = new ChartSetups();

      for (ChartSetup var3 : var0) {
         var1.add(var3.getClone());
      }

      return var1;
   }

   public static ArrayList<Condition> getConditions(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      }

      List var2 = var0.getChildren("Condition");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);

         try {
            var1.add(getCondition(var4));
         } catch (Exception var6) {
            Log.error("Condition #" + var3 + " - " + var6.getMessage());
         }
      }

      return var1;
   }

   public static Condition getCondition(Element var0) throws Exception {
      boolean var1 = Boolean.parseBoolean(XMLUtil.getAttr(var0, "use"));
      Element var2 = var0.getChild("Left-Side");
      Element var3 = var0.getChild("Right-Side");
      Element var4 = var0.getChild("Comparator");
      IConditionValue var5 = loadConditionValue(var2);
      IConditionValue var6 = loadConditionValue(var3);
      String var7 = var4.getAttributeValue("value");
      return new Condition(var1, var5, var6, var7);
   }

   private static IConditionValue loadConditionValue(Element var0) throws Exception {
      if (var0 == null) {
         return null;
      }

      String var1 = var0.getAttributeValue("valueType");
      IConditionValue var2 = null;
      if (var1.equals("numeric")) {
         Element var3 = var0.getChild("Numeric-Value");
         if (var3 == null) {
            return null;
         }

         String var4 = XMLUtil.getAttr(var3, "value");
         double var5 = 0.0;

         try {
            var5 = Double.parseDouble(var4);
         } catch (Exception var8) {
            throw new Exception("Incorrect value '" + var4 + "'");
         }

         var2 = new NumericValue(var5);
      } else {
         Element var10 = var0.getChild("Column-Value");
         if (var10 == null) {
            return null;
         }

         var2 = getConditionValue(var10);
      }

      return var2;
   }

   public static IConditionValue getConditionValue(Element var0) throws Exception {
      try {
         String var1 = var0.getAttributeValue("column");
         if (var1 == null) {
            var1 = var0.getAttributeValue("class");
         }

         byte var2 = XMLUtil.getByteAttr(var0, "direction", (byte)0);
         byte var3 = XMLUtil.getByteAttr(var0, "sampleType", (byte)127);
         byte var4 = XMLUtil.getByteAttr(var0, "plType", (byte)10);
         String var5 = XMLUtil.getAttr(var0, "resultType", "main");
         int var6 = 0;
         int var7 = XMLUtil.getIntAttr(var0, "pctRatio", 0);
         if (var7 > 0) {
            var6 = var7;
         }

         if (ResultTypes.isCrossCheck(var5)) {
            return new CrossCheckConditionValue(var1, var5, var6, var0);
         }

         DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var1);
         return new ColumnConditionValue(var8, var5, var2, var4, var3, var6);
      } catch (Exception var9) {
         Log.error("Error while parsing condition value. " + XMLUtil.elementToString(var0), var9);
         throw new Exception("Error while parsing condition value. " + var9.getMessage(), var9);
      }
   }

   public static TradingOptions getOptions(Element var0) {
      return TradingOptionsList.getInstance().parseOptionsFromXml(var0);
   }

   public static OutOfSample getOOS(Element var0) throws ParseException {
      OutOfSample var1 = new OutOfSample();
      var1.setFromXML(var0);
      return var1;
   }

   public static Databank getOutputDatabank(String var0, Element var1) throws Exception {
      Element var2 = var1.getChild("Databanks");
      if (var2 != null) {
         for (Element var4 : var2.getChildren("Databank")) {
            String var5 = var4.getAttributeValue("name");
            if (var5.equals("Output")) {
               String var6 = var4.getAttributeValue("value");
               Databank var7 = ProjectEngine.get(var0).getDatabanks().get(var6);
               if (var7 != null) {
                  return var7;
               }
            }
         }

         for (Element var10 : var2.getChildren("Databank")) {
            String var12 = var10.getAttributeValue("name");
            if (var12.equals("Target")) {
               String var15 = var10.getAttributeValue("value");
               Databank var17 = ProjectEngine.get(var0).getDatabanks().get(var15);
               if (var17 != null) {
                  return var17;
               }
            }
         }
      }

      Databank var9 = ProjectEngine.get(var0).getDatabanks().get("Results");
      if (var9 == null) {
         throw new Exception(L.t("No output databank set.", new Object[0]));
      }

      if (var2 == null) {
         var2 = new Element("Databanks");
         var1.addContent(var2);
      }

      boolean var11 = false;

      for (Element var16 : var2.getChildren("Databank")) {
         String var18 = var16.getAttributeValue("name");
         if ("Output".equals(var18)) {
            var11 = true;
            break;
         }
      }

      if (!var11) {
         Element var14 = new Element("Databank");
         var14.setAttribute("label", "Output databank");
         var14.setAttribute("name", "Output");
         var14.setAttribute("value", "Results");
         var2.addContent(var14);
         Log.info("Project '{}' - Missing <Databanks> element in task settings, created default Output=Results", var0);
      }

      return var9;
   }

   public static Databank getInputDatabank(String var0, Element var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(var1, "Databanks");

      for (Element var4 : var2.getChildren("Databank")) {
         String var5 = var4.getAttributeValue("name");
         if (var5.equals("Input")) {
            String var6 = var4.getAttributeValue("value");
            Databank var7 = ProjectEngine.get(var0).getDatabanks().get(var6);
            if (var7 != null) {
               return var7;
            }
         }
      }

      for (Element var9 : var2.getChildren("Databank")) {
         String var10 = var9.getAttributeValue("name");
         if (var10.equals("Source")) {
            String var11 = var9.getAttributeValue("value");
            Databank var12 = ProjectEngine.get(var0).getDatabanks().get(var11);
            if (var12 != null) {
               return var12;
            }
         }
      }

      throw new Exception(L.t("No input databank set.", new Object[0]));
   }

   public static Databank getDatabankByType(String var0, String var1, Element var2) throws Exception {
      Element var3 = XMLUtil.getChildElem(var2, "Databanks");

      for (Element var5 : var3.getChildren("Databank")) {
         String var6 = var5.getAttributeValue("name");
         String var7 = var5.getAttributeValue("value");
         if (var6.equals(var1)) {
            return ProjectEngine.get(var0).getDatabanks().get(var7);
         }
      }

      throw new Exception(L.t("Unknown databank type '%s'", new Object[]{var1}));
   }

   public static ISQTask createTask(String var0, String var1, String var2) throws Exception {
      for (ISQTask var4 : SQPluginManager.getPlugins(ISQTask.class)) {
         if (var4.getType().equals(var0)) {
            ISQTask var5 = var4.clone(var2, null);
            var5.setCustomName(var1);
            return var5;
         }
      }

      throw new Exception(L.t("Cannot create task '%s'", new Object[]{var0}));
   }

   public static Element createTaskElement(String var0) throws Exception {
      return new Element("Task").setAttribute("type", var0);
   }

   public static String[] getTaskSettings(String var0) throws Exception {
      for (ISQTask var2 : SQPluginManager.getPlugins(ISQTask.class)) {
         if (var2.getType().equals(var0)) {
            return var2.getSettings();
         }
      }

      throw new Exception(L.t("Unknown task type '%s'", new Object[]{var0}));
   }

   public static ArrayList<RobustnessTestMethod> getRTMethods(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (Element var3 : var0.getChildren("Method")) {
         boolean var4 = XMLUtil.elementIs(var3, "use");
         if (var4) {
            String var5 = var3.getAttributeValue("type");
            RobustnessTestMethod var6 = (RobustnessTestMethod)RobustnessTestMethodsList.get().createNew(var5);
            var6.setFromXML(var3);
            var1.add(var6);
         }
      }

      return var1;
   }

   public static Element getTaskConfigByName(Element var0, String var1) throws Exception {
      Element var2 = var0.getChild("Tasks");

      for (Element var4 : var2.getChildren("Task")) {
         String var5 = var4.getAttributeValue("name");
         if (var5.equals(var1)) {
            return var4;
         }
      }

      throw new Exception(L.t("Project config doesn't contain task with name '%s'.", new Object[]{var1}));
   }

   public static Element getTaskConfigByXMLFile(Element var0, String var1) throws Exception {
      Element var2 = var0.getChild("Tasks");

      for (Element var4 : var2.getChildren("Task")) {
         String var5 = var4.getAttributeValue("taskXMLFile");
         if (var5.equals(var1)) {
            return var4;
         }
      }

      throw new Exception(L.t("Project config doesn't contain task with taskXMLFile '%s'.", new Object[]{var1}));
   }

   public static Databank getOutputDatabank(String var0, String var1) throws Exception {
      SQProject var2 = ProjectEngine.get(var0);
      Element var3 = var2.getTaskSettingsByName(var1);
      return getOutputDatabank(var0, var3);
   }

   public static void checkAdditionalChartsAndCIndysExpected(String var0, Databank var1, ChartSetup var2, ArrayList<String> var3) throws TaskException {
      if (var1 == null) {
         throw new TaskException(L.t("Additional charts check - Non-existing input Databank", new Object[0]));
      }

      int var4 = 0;
      Iterator var5 = var1.getRecords().iterator();

      try {
         while (var5.hasNext()) {
            ResultsGroup var6 = (ResultsGroup)var5.next();
            if (var3 == null || var3.contains(var6.getName())) {
               var4 = Math.max(var6.getChartCount(), var4);
               checkCustDataIndys(var6);
            }
         }
      } catch (ConcurrentModificationException var7) {
         Log.info("Skipped because of ConcurrentModificationException - not necessary to check now");
      }

      if (var4 > var2.getChartsCount() - 1) {
         throw new TaskException(
            9,
            AbstractTask.formatWSError(
               "Data",
               null,
               L.t("Not enough additional charts defined. Some strategies in databank expect at least %d additional chart(s).", new Object[]{var4})
            )
         );
      }
   }

   public static void checkAdditionalChartsAndCIndysExpected(String var0, ResultsGroup var1, ChartSetup var2) throws TaskException {
      int var3 = 0;
      var3 = Math.max(var1.getChartCount(), var3);
      checkCustDataIndys(var1);
      if (var3 > var2.getChartsCount() - 1) {
         throw new TaskException(
            9,
            AbstractTask.formatWSError(
               "Data", null, L.t("Not enough additional charts defined. Strategy expect at least %d additional chart(s).", new Object[]{var3})
            )
         );
      }
   }

   public static void checkCustDataIndys(ResultsGroup var0) throws TaskException {
      try {
         Element var1 = var0.portfolio().getStrategyXml();
         if (var1 == null) {
            return;
         }

         verifyCustDataIndyExists(var0.getName(), var1);
      } catch (TaskException var2) {
         throw var2;
      } catch (Exception var3) {
         Log.error("checkCustDataIndys failes. ", var3);
      }
   }

   public static void checkVolumeProfileBlocksAllowed(Databank var0, ArrayList<String> var1) throws TaskException {
      if (var0 != null) {
         if (!VolumeProfileSubscription.getInstance().isActive()) {
            Iterator var2 = var0.getRecords().iterator();

            try {
               while (var2.hasNext()) {
                  ResultsGroup var3 = (ResultsGroup)var2.next();
                  if (var1 == null || var1.contains(var3.getName())) {
                     checkVolumeProfileBlocksAllowed(var3);
                  }
               }
            } catch (ConcurrentModificationException var4) {
               Log.info("Skipped because of ConcurrentModificationException - not necessary to check now");
            }
         }
      }
   }

   public static void checkVolumeProfileBlocksAllowed(ResultsGroup var0) throws TaskException {
      if (var0 != null) {
         if (!VolumeProfileSubscription.getInstance().isActive()) {
            try {
               VolumeProfileBlocks.checkStrategyAllowed(var0.getStrategyXml(), var0.getName());
            } catch (TaskException var2) {
               throw var2;
            } catch (Exception var3) {
               throw new TaskException(9, AbstractTask.formatWSError("Data", null, var3.getMessage()));
            }
         }
      }
   }

   private static void verifyCustDataIndyExists(String var0, Element var1) throws TaskException {
      if (var1.getName().equals("Item")) {
         String var2 = var1.getAttributeValue("key");
         if (var2 != null && var2.startsWith("CDataIndy")) {
            String var3 = CustomDataManager.getCDataIndyId(var2);
            if (var3 != null) {
               CustomDataInfo var4 = CustomDataManager.getDataInfo(var3);
               if (var4 == null) {
                  Log.error("Cannot load data info for {}", var3);
                  throw new TaskException(
                     9,
                     AbstractTask.formatWSError(
                        "Data",
                        null,
                        "Strategy '" + var0 + "' uses custom data indicator '" + var3 + "', which doesn't exist in your StrategyQuant installation!"
                     )
                  );
               }
            }
         }
      }

      List var5 = var1.getChildren();
      if (var5 != null && var5.size() > 0) {
         for (int var6 = 0; var6 < var5.size(); var6++) {
            Element var7 = (Element)var5.get(var6);
            verifyCustDataIndyExists(var0, var7);
         }
      }
   }

   public static void delete(final SQProject var0, Databank var1, String[] var2, String var3) throws Exception {
      ArrayList var4 = getOptimizationStrategies(var0, var1);
      final ArrayList var5 = new ArrayList();
      boolean var6 = var2.length == var1.size();

      for (int var7 = 0; var7 < var2.length; var7++) {
         String var8 = var2[var7];
         boolean var9 = false;
         if (var4 != null) {
            for (int var10 = 0; var10 < var4.size(); var10++) {
               File var11 = (File)var4.get(var10);
               String var12 = SQUtils.stripExtension(var11.getName());
               if (var8.equals(var12)) {
                  var9 = true;
                  break;
               }
            }
         }

         if (!var9) {
            var5.add(var8);
         }
      }

      if (var2.length == 1 && var5.size() == 0) {
         throw new Exception(L.t("Cannot remove strategy that is selected to be optimized.", new Object[0]));
      }

      var0.publisher.resetLastData("progress-channel");
      var0.getProgress().update("delete", 0.0, null);
      IDatabankListener var22 = null;

      try {
         var22 = new IDatabankListener() {
            double count = 0.0;

            @Override
            public void databankChanged(Databank var1) {
               this.count++;
               int var2x = (int)(this.count / var5.size() * 100.0);
               var0.getProgress().update("delete", var2x, null);
            }
         };
         var1.addListener(var22);
         ArrayList var23 = new ArrayList();

         for (String var26 : var5) {
            try {
               var1.remove(var26, true, true, !var6, false, true, var3);
            } catch (Exception var20) {
               var23.add(var26);
            }
         }

         if (var23.size() <= 0) {
            var1.updateBestResults();
         } else {
            Thread.sleep(200L);
            boolean var25 = false;

            for (String var28 : var23) {
               if (var1.contains(var28)) {
                  try {
                     var1.remove(var28, true, !var6, !var6, false, true, var3);
                  } catch (RecordNotFoundException var18) {
                  } catch (Exception var19) {
                     var25 = true;
                  }
               } else {
                  File var29 = new File(var1.getDatabankFolder() + "/" + var28 + ".sqx");
                  if (var29.exists() && !var29.delete()) {
                     var25 = true;
                  }
               }
            }

            var1.updateBestResults();
            if (var25) {
               throw new Exception(L.t("Some of the strategies couldn't be deleted.", new Object[0]));
            }
         }

         if (var6) {
            var1.notifyListeners("cleared");
         }
      } finally {
         var1.refreshGrid();
         if (var22 != null) {
            var1.removeListener(var22);
            var0.getProgress().update("delete", 100.0, null);
         }

         if (var2.length > 1) {
            System.gc();
         }
      }
   }

   public static ArrayList<File> getOptimizationStrategies(SQProject var0, Databank var1) {
      ArrayList var2 = null;
      ArrayList var3 = var0.getTasks();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         ISQTask var5 = (ISQTask)var3.get(var4);
         String var6 = var5.getType();
         String var7 = var5.getName();
         Element var8 = var5.getConfig();
         if (var8 == null) {
            Log.warn("Warning! Task '" + var7 + "' from project '" + var0.getName() + "' doesn't contain Settings element in its config. ");
         } else {
            Element var9 = var8.getChild("Optimization");
            if (var6 != null && var8 != null && var9 != null && var6.equals("Optimize")) {
               Databank var10 = null;

               try {
                  var10 = getOutputDatabank(var0.getName(), var8);
               } catch (Exception var13) {
               }

               if (var10 != null && var10.getName().equals(var1.getName())) {
                  OptimizationSettings var11 = new OptimizationSettings(var0.getName());

                  try {
                     var11.setFromXML(var8, var9);
                  } catch (Exception var14) {
                     continue;
                  }

                  if (var11.singleStrategy && var11.strategyFile != null) {
                     if (var2 == null) {
                        var2 = new ArrayList();
                     }

                     var2.add(var11.strategyFile);
                  }
               }
            }
         }
      }

      return var2;
   }

   public static double getChartValue(String var0, Element var1) {
      Element var2 = (Element)var1.getChild("Setups").getChildren().get(0);
      return Double.parseDouble(var2.getAttributeValue(var0));
   }

   public static SettingsMap getBuildSettings(Element var0) {
      SettingsMap var1 = new SettingsMap();
      Element var2 = var0.getChild("SLPTOptions");
      if (var2 == null) {
         return null;
      }

      for (Element var4 : var2.getChildren()) {
         String var5 = var4.getValue();
         if (!var4.getName().endsWith("ValueType")) {
            if (!var5.equals("true") && !var5.equals("false")) {
               var1.set(var4.getName(), Double.parseDouble(var5));
            } else {
               var1.set(var4.getName(), Boolean.parseBoolean(var5));
            }
         }
      }

      Element var10 = var0.getChild("BuildTradingOptions");
      if (var10 == null) {
         return null;
      }

      for (Element var12 : var10.getChildren()) {
         String var6 = var12.getAttributeValue("key");
         String var7 = var12.getValue();

         try {
            if (var7.equals("true") || var7.equals("false")) {
               var1.set(var6, Boolean.parseBoolean(var7));
            } else if (var7.contains(".")) {
               var1.set(var6, Double.parseDouble(var7));
            } else {
               var1.set(var6, Integer.parseInt(var7));
            }
         } catch (NumberFormatException var9) {
            Log.error("Cannot parse Build Trading option key {}, value {}", var6, var7);
         }
      }

      return var1;
   }

   public static int getDismissBadStrategiesSettings(Element var0) {
      int var1 = 0;
      Element var2 = var0.getChild("AutomaticDismissal");
      List var3 = null;
      if (var2 != null) {
         var3 = var2.getChildren("Problem");

         for (int var11 = 0; var11 < var3.size(); var11++) {
            Element var12 = (Element)var3.get(var11);
            if (XMLUtil.elementIs(var12, "dismiss")) {
               String var6 = var12.getAttributeValue("code");
               int var7 = 0;

               try {
                  var7 = Integer.parseInt(var6);
               } catch (Exception var9) {
                  Log.warn("Automatic dismissal settings error - no reason code");
                  continue;
               }

               String var8 = BadStrategyException.getExplanation(var7);
               if (!var8.equals(BadStrategyException.ExplanationUnknown)) {
                  if (var7 != 0) {
                     var1 += var7;
                  }
               } else {
                  Log.warn("Automatic dismissal settings error - unknown reason code " + var7);
               }
            }
         }

         return var1;
      } else {
         ArrayList var4 = BadStrategyException.listProblems();

         for (int var5 = 0; var5 < var4.size(); var5++) {
            var1 += ((StrategyProblem)var4.get(var5)).code;
         }

         return var1;
      }
   }

   public static boolean getDismissBadStrategiesWarnings(Element var0) {
      Element var1 = var0.getChild("AutomaticDismissal");
      if (var1 == null) {
         return false;
      }

      String var2 = var1.getAttributeValue("warnings");
      return var2 != null && var2.equals("true");
   }

   public static File getStrategyFile(Databank var0, String var1, int var2, String var3) {
      if (var0 == null) {
         Log.error("Cannot get strategy file. Databank is null");
         return null;
      }

      if (var1 == null) {
         Log.error("Cannot get strategy file. Strategy name is null");
         return null;
      }

      ResultsGroup var4 = null;

      String var5;
      try {
         var4 = var0.getLocked(var1, var3);
         var5 = var4.getFilePath();
      } catch (Exception var11) {
         return null;
      } finally {
         if (var4 != null) {
            var4.releaseLock(var3);
         }
      }

      File var6 = new File(var5);
      long var7 = var6.length() / 1024L;
      if (var7 > var2) {
         Log.info("Strategy file not attached - too big (" + var7 + " kB)");
         return null;
      } else {
         return var6;
      }
   }

   public static CommissionsMethod getCommissionsMethod(Element var0) throws Exception {
      Element var1 = var0.getChild("Commissions");
      if (var1 == null) {
         try {
            String var8 = var0.getAttributeValue("commission");
            return CommissionsMethodsList.create("SizeBased", Double.parseDouble(var8));
         } catch (Exception var7) {
            throw new Exception(L.t("Neither commissions method nor commission price set.", new Object[0]));
         }
      } else {
         List var2 = var1.getChildren("Method");

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            if (var4.getChild("Params") == null) {
               var4.addContent(new Element("Params"));
            }

            if (var2.size() <= 1 || XMLUtil.elementIs(var4, "use")) {
               String var5 = var4.getAttributeValue("type");
               if (CommissionsMethodsList.get().checkClassExists(var5)) {
                  CommissionsMethod var6 = ((CommissionsMethod)CommissionsMethodsList.get().findClassByName(var5)).getClone();
                  var6.setFromXML(var1);
                  return var6;
               }
            }
         }

         throw new Exception(L.t("No commissions method set.", new Object[0]));
      }
   }

   public static SwapMethod getSwapMethod(Element var0) throws Exception {
      SwapMethod var1 = new SwapMethod();
      if (var0 != null) {
         Element var2 = var0.getChild("Swap");
         if (var2 != null) {
            var1.setFromXML(var2);
         }
      }

      return var1;
   }

   public static ArrayList<ICrossCheck> cloneCrossChecks(ArrayList<ICrossCheck> var0) {
      if (var0 == null) {
         return null;
      }

      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < var0.size(); var2++) {
         var1.add(((ICrossCheck)var0.get(var2)).clone(null));
      }

      return var1;
   }

   public static Element getElCommission(String var0, boolean var1) throws IOException, JDOMException {
      String var2;
      if (var1) {
         var2 = "<Setup><Commissions>\n                <Method type=\"PerTrade\" use=\"true\">\n                  <Params>\n                    <Param key=\"Commission\" className=\"PerTrade\">XXXXX</Param>\n                  </Params>\n                </Method>\n              </Commissions></Setup>";
      } else {
         var2 = "<Method type=\"PerTrade\" use=\"true\">\n                  <Params>\n                    <Param key=\"Commission\" className=\"PerTrade\">XXXXX</Param>\n                  </Params>\n                </Method>";
      }

      String var3 = var2.replace("XXXXX", var0);
      return XMLUtil.stringToElement(var3);
   }

   public static IFitnessFunction getFitnessFunction(Element var0) throws Exception {
      Element var1 = XMLUtil.getChildElem(var0, "FitnessCriteria");
      String var2 = var1.getAttributeValue("method");

      for (IFitnessFunction var4 : SQPluginManager.getPlugins(IFitnessFunction.class)) {
         if (var4.getFitnessKey().equals(var2)) {
            var4 = var4.clone();
            Element var5 = var1.getChild("Settings");
            if (var5 == null) {
               throw new Exception(L.t("FitnessCriteria - No Settings found for method '%s'", new Object[]{var2}));
            }

            var4.initFitnessFromXml(var5);
            return var4;
         }
      }

      throw new Exception(L.t("No fitness function plugin of type '%s' found.", new Object[]{var2}));
   }

   private static void addGoalElement(Element var0, String var1, double var2) {
      Element var4 = new Element("Goal");
      String var5 = getElValueTypeForGoal(var1);
      if (var5 != null) {
         var4.setAttribute("use", "true");
         var4.setAttribute("type", var1);
         var4.setAttribute("weight", Double.toString(var2));
         var4.setAttribute("valueType", var5);
         var4.setAttribute("target", "0");
         var0.addContent(var4);
      }
   }

   private static String getElValueTypeForGoal(String var0) {
      for (DatabankColumn var2 : DatabankColumns.get().getAvailableClasses()) {
         if (var2.getClassName().equals(var0)) {
            return Byte.toString(var2.valueType);
         }
      }

      return null;
   }

   public static String getEngineKey(Element var0) {
      String var1 = null;

      try {
         Element var2 = var0.getChild("Data");
         if (var2 == null) {
            var2 = var0.getChild("CustomData");
         }

         if (var2 != null) {
            Element var3 = var2.getChild("Setups").getChild("Setup");
            if (var3 != null) {
               var1 = var3.getAttributeValue("engine");
            }
         }
      } catch (Exception var4) {
         Log.error("Failed to parse engine.", var4);
      }

      return Engines.getEngineKey(var1);
   }
}
