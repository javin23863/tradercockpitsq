package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.robustnesstests.RobustnessOrdersValues;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResultComparator;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MonteCarloCrossCheckMethod extends CrossCheckMethod implements IFitnessFunction {
   public static final Logger Log = LoggerFactory.getLogger("MonteCarloCrossCheckMethod");
   public static int[] ConfidenceLevels = new int[]{50, 60, 70, 80, 90, 92, 95, 97, 98, 99, 100};
   public static double[] InitialCapitalVariants = new double[]{0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0, 6.0};
   private String fitnessDatabankColumnName = null;
   private int fitnessConfidenceLevel = 50;

   protected void computeConfidenceLevels(Result var1, ArrayList<RobustnessResults> var2) {
      List var3 = DatabankColumns.get().getAvailableClasses();
      HashMap var4 = this.createStatsForConfidenceLevels();

      for (DatabankColumn var6 : var3) {
         byte var7 = var6.getValueType();
         if (var7 == 3) {
            boolean var12 = true;
         }

         Collections.sort(var2, new RobustnessResultComparator(var6));

         for (int var9 : var4.keySet()) {
            int var10 = this.getConfidenceLevelIndex(var9, var2.size());
            if (var10 >= 0) {
               RobustnessResults var11 = (RobustnessResults)var2.get(var10);
               ((SQStats)var4.get(var9)).copyValueFrom(var11.getOriginalStats(), var6.getStatsValueName());
            }
         }
      }

      var1.set(this.getSettingName() + "_ConfidenceLevelsStats", var4);
   }

   private int getConfidenceLevelIndex(int var1, int var2) {
      int var3 = (int)Math.ceil(var2 * (var1 / 100.0));
      return var3 - 1;
   }

   private HashMap<Integer, SQStats> createStatsForConfidenceLevels() {
      HashMap var1 = new HashMap();
      var1.put(50, new SQStats());
      var1.put(60, new SQStats());
      var1.put(70, new SQStats());
      var1.put(80, new SQStats());
      var1.put(90, new SQStats());
      var1.put(92, new SQStats());
      var1.put(95, new SQStats());
      var1.put(97, new SQStats());
      var1.put(98, new SQStats());
      var1.put(99, new SQStats());
      var1.put(100, new SQStats());
      return var1;
   }

   private boolean isRuined(double var1, RobustnessOrdersValues var3) {
      double var4 = var1;
      int[] var6 = var3.arr();

      for (int var7 = 0; var7 < var6.length; var7++) {
         var4 += var6[var7] / 100.0;
         if (var4 <= 0.0) {
            return true;
         }
      }

      return false;
   }

   protected void getOrCreateOriginalOrders(Result var1, OrdersList var2, String var3, SettingsMap var4, SymbolsMap var5) throws Exception {
      var1.set("RobustnessOriginalOrders", var2);
      RobustnessResults var6 = MCResultsComputer.computeResults(var2, var4, var5, null);
      var1.set("RobustnessOriginalResults", var6);
   }

   @Override
   public void saveDataToFile(ResultsGroup var1, JarOutputStream var2) throws Exception {
      String var3 = var1.getMainResultKey();
      Result var4 = var1.mainResult();
      int var6 = var4.getInt(this.getSettingName() + "_NumberOfSimulations");
      if (var6 != 0) {
         var3 = SQUtils.correctResultKeyToDirname(var3);
         Element var7 = new Element("RobustnessResults");
         var7.addContent(new Element("NumberOfSimulations").addContent("" + var6));
         String var8 = var4.getString(this.getSettingName() + "_Symbol");
         if (var8 != null) {
            String var9 = var4.getString(this.getSettingName() + "_TimeFrame");
            String var10 = var4.getString(this.getSettingName() + "_DateRange");
            var7.addContent(new Element("Symbol").addContent(var8));
            var7.addContent(new Element("TimeFrame").addContent(var9));
            var7.addContent(new Element("DateRange").addContent(var10));
            String[] var11 = (String[])var4.get(this.getSettingName() + "_Methods");
            Element var12 = new Element("Methods");

            for (int var13 = 0; var13 < var11.length; var13++) {
               var12.addContent(new Element("Method").setText(var11[var13]));
            }

            var7.addContent(var12);
         }

         RobustnessResults var28 = (RobustnessResults)var4.get("RobustnessOriginalResults");
         if (var28 != null) {
            Element var30 = var28.getXml("RobustnessOriginalResults");
            var7.addContent(var30);

            try {
               JarEntry var5 = new JarEntry("Results/" + var3 + "/RobustnessOriginalOrders.bin");
               var2.putNextEntry(var5);
               var28.saveOrderValuesToFile(var2);
            } catch (Exception var24) {
            }

            for (int var32 = 0; var32 < var6; var32++) {
               var28 = var4.getMCSimulation(this.getSettingName(), var32);
               if (var28 != null) {
                  JarEntry var26 = new JarEntry("Results/" + var3 + "/" + String.format(this.getSettingName() + "_Simulation%dOrders", var32) + ".bin");
                  var2.putNextEntry(var26);
                  var28.saveOrderValuesToFile(var2);
               }
            }
         }

         HashMap var31 = (HashMap)var4.get(this.getSettingName() + "_ConfidenceLevelsStats");
         if (var31 != null) {
            Element var33 = new Element("ConfidenceLevelStats");

            for (int var39 : var31.keySet()) {
               SQStats var14 = (SQStats)var31.get(var39);
               Element var15 = new Element("LevelStat");
               var15.setAttribute("level", "" + var39);
               var15.addContent(var14.getXML());
               var33.addContent(var15);
            }

            var7.addContent(var33);
         }

         HashMap var34 = (HashMap)var4.get(String.format(this.getSettingName() + "_RiskOfRuins%d", 90));
         if (var34 != null) {
            Element var37 = new Element("RiskOfRuins");

            for (int var16 : ConfidenceLevels) {
               Element var17 = new Element("ConfidenceLevelStats");
               var17.setAttribute("level", "" + var16);
               var34 = (HashMap)var4.get(String.format(this.getSettingName() + "_RiskOfRuins%d", var16));

               for (double var21 : InitialCapitalVariants) {
                  Element var23 = new Element("InitialCapitalVariantStats");
                  var23.setAttribute("variant", "" + var21);
                  var23.addContent(((SQStats)var34.get(var21)).getXML());
                  var17.addContent(var23);
               }

               var37.addContent(var17);
            }

            var7.addContent(var37);
         }

         JarEntry var27 = new JarEntry("Results/" + var3 + "/" + this.getSettingName() + "_Results.xml");
         var2.putNextEntry(var27);
         Document var38 = new Document(var7);
         BufferedWriter var41 = new BufferedWriter(new OutputStreamWriter(var2, "UTF8"));
         XMLOutputter var43 = new XMLOutputter(Format.getPrettyFormat());
         var43.output(var38, var41);
      }
   }

   @Override
   public void loadDataFromFile(ResultsGroup var1, JarFile var2) throws Exception {
      String var3 = var1.getMainResultKey();
      Result var4 = var1.mainResult();
      var3 = SQUtils.correctResultKeyToDirname(var3);
      JarEntry var5 = var2.getJarEntry("Results/" + var3 + "/" + this.getSettingName() + "_Results.xml");
      if (var5 != null) {
         Element var6 = XMLUtil.readXmlFile(var2.getInputStream(var5));
         Element var7 = var6.getChild("NumberOfSimulations");
         int var8 = Integer.parseInt(var7.getValue());
         if (var6.getChild("Symbol") != null) {
            var4.set(this.getSettingName() + "_Symbol", var6.getChild("Symbol").getText());
            var4.set(this.getSettingName() + "_TimeFrame", var6.getChild("TimeFrame").getText());
            var4.set(this.getSettingName() + "_DateRange", var6.getChild("DateRange").getText());
            List var9 = var6.getChild("Methods").getChildren("Method");
            String[] var10 = new String[var9.size()];

            for (int var11 = 0; var11 < var9.size(); var11++) {
               var10[var11] = ((Element)var9.get(var11)).getText();
            }

            var4.set(this.getSettingName() + "_Methods", var10);
         }

         Element var23 = var6.getChild("RobustnessOriginalResults");
         if (var23 != null) {
            RobustnessResults var24 = new RobustnessResults(var23.getChild("SQStats"));
            var4.set("RobustnessOriginalResults", var24);
            JarEntry var27 = var2.getJarEntry("Results/" + var3 + "/RobustnessOriginalOrders.bin");
            RobustnessOrdersValues var12 = new RobustnessOrdersValues(var2.getInputStream(var27));
            var24.setOrdersValues(var12);

            for (int var13 = 0; var13 < var8; var13++) {
               JarEntry var14 = var2.getJarEntry("Results/" + var3 + "/" + String.format(this.getSettingName() + "_Simulation%dOrders", var13) + ".bin");
               if (var14 != null) {
                  var12 = new RobustnessOrdersValues(var2.getInputStream(var14));
                  var24 = new RobustnessResults();
                  var24.setOrdersValues(var12);
                  var4.addMCSimulation(this.getSettingName(), var24);
               }
            }
         }

         HashMap var26 = new HashMap();

         for (Element var31 : var6.getChild("ConfidenceLevelStats").getChildren("LevelStat")) {
            int var33 = Integer.parseInt(var31.getAttributeValue("level"));
            Element var35 = var31.getChild("SQStats");
            SQStats var15 = new SQStats();
            var15.setFromXML(var35);
            var26.put(var33, var15);
         }

         var4.set(this.getSettingName() + "_ConfidenceLevelsStats", var26);
         Element var29 = var6.getChild("RiskOfRuins");
         if (var29 != null) {
            for (Element var34 : var29.getChildren("ConfidenceLevelStats")) {
               int var36 = Integer.parseInt(var34.getAttributeValue("level"));
               HashMap var37 = new HashMap();

               for (Element var17 : var34.getChildren("InitialCapitalVariantStats")) {
                  double var18 = Double.parseDouble(var17.getAttributeValue("variant"));
                  Element var20 = var17.getChild("SQStats");
                  SQStats var21 = new SQStats();
                  var21.setFromXML(var20);
                  var37.put(var18, var21);
               }

               var4.set(String.format(this.getSettingName() + "_RiskOfRuins%d", var36), var37);
            }
         }
      }
   }

   @Override
   public double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      return this.computeFitness(var1, var2, var3, true);
   }

   @Override
   public double computeFitness(ResultsGroup var1, byte var2, byte var3, boolean var4) throws Exception {
      Result var5 = var1.mainResult();
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(this.fitnessDatabankColumnName);
      HashMap var9 = (HashMap)var5.get(this.getSettingName() + "_ConfidenceLevelsStats");
      double var6;
      if (var9 == null) {
         var6 = var8.getNumericValue(var1, "Portfolio", var2, (byte)10, var3);
      } else {
         SQStats var10 = (SQStats)var9.get(this.fitnessConfidenceLevel);
         var6 = var8.getNumericValue(var10);
      }

      double var12 = var8.transformToFitnessRange(var6, 0.0, (byte)0);
      if (var12 < 0.0) {
         var12 = 0.0;
      }

      if (var12 > 1.0) {
         var12 = 1.0;
      }

      return var12;
   }

   @Override
   public String getFitnessKey() {
      return this.getSettingName();
   }

   @Override
   public String getFitnessName() {
      return "Cross check - " + this.getName();
   }

   @Override
   public void initFitnessFromXml(Element var1) {
      Element var2 = var1.getChild("Ranking");
      this.fitnessDatabankColumnName = var2.getAttributeValue("type");
      if (this.fitnessDatabankColumnName.equals("Other")) {
         this.fitnessDatabankColumnName = var2.getAttributeValue("value");
      }

      this.fitnessConfidenceLevel = XMLUtil.getInt(var1, "ConfidenceLevel", 50);
   }

   @Override
   public byte getFitnessType() throws Exception {
      return this.fitnessDatabankColumnName.equals("Weighted")
         ? 1
         : ((DatabankColumn)DatabankColumns.get().findClassByName(this.fitnessDatabankColumnName)).valueType;
   }

   @Override
   public String getFitnessDatabankColumnName() {
      return this.fitnessDatabankColumnName;
   }

   @Override
   public ArrayList<DatabankColumn> getUsedStatValues() throws Exception {
      throw new Exception(L.t("This fitness method cannot be used in Walk-Forward!", new Object[0]));
   }

   @Override
   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      HashMap var5 = (HashMap)var1.mainResult().get(this.getSettingName() + "_ConfidenceLevelsStats");
      if (var5 == null) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      int var6 = XMLUtil.getIntAttr(var3, "confidenceLevel", 60);
      SQStats var7 = (SQStats)var5.get(var6);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getNumericValue(var7);
   }

   @Override
   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      HashMap var5 = (HashMap)var1.mainResult().get(this.getSettingName() + "_ConfidenceLevelsStats");
      return var5 != null;
   }

   @Override
   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      HashMap var5 = (HashMap)var1.mainResult().get(this.getSettingName() + "_ConfidenceLevelsStats");
      if (var5 == null) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      DatabankColumn var6 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var6.getValue(var1, var1.getMainResultKey(), (byte)0, (byte)10, (byte)127);
   }

   @Override
   public String getColumnTitle(String var1, Element var2, Object... var3) {
      int var4 = XMLUtil.getIntAttr(var2, "confidenceLevel", 60);
      String var5 = this.getColumnTitleTemplate();
      var5 = var5.replace("#columnName#", var1);
      return var5.replace("#confidenceLevel#", var4 + "");
   }

   @Override
   public String getColumnTitleTemplate() {
      return "#columnName# (" + this.getShortName() + ", Conf. level #confidenceLevel#%)";
   }

   @Override
   public boolean doesCreateSubjobs() {
      return false;
   }

   @Override
   public IFitnessFunction clone() {
      return this;
   }
}
