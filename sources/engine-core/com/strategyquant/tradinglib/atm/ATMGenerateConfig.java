package com.strategyquant.tradinglib.atm;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.atm.exits.FixedProfit;
import com.strategyquant.tradinglib.atm.exits.MultipleOfOriginalPT;
import com.strategyquant.tradinglib.atm.exits.MultipleOfOriginalSL;
import com.strategyquant.tradinglib.atm.exits.None;
import com.strategyquant.tradinglib.atm.exits.TrailingStop;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.io.Serializable;
import org.jdom2.Element;

public class ATMGenerateConfig implements Serializable {
   private static final int Scenario2exits_5050 = 100;
   private static final int Scenario2exits_3366 = 101;
   private static final int Scenario2exits_6633 = 102;
   private static final int Scenario3exits_333333 = 200;
   private static final int Scenario3exits_502525 = 201;
   private static final int Scenario3exits_255025 = 202;
   private static final int Scenario3exits_252550 = 203;
   public boolean useSLMultiple;
   public double slMultipleMin = 1.0;
   public double slMultipleMax = 1.0;
   public boolean usePTMultiple;
   public double ptMultipleMin = 1.0;
   public double ptMultipleMax = 1.0;
   public boolean useFixedProfit;
   public boolean useFixedProfitFixedPips;
   public int fixedProfitFixedPipsMin = 30;
   public int fixedProfitFixedPipsMax = 80;
   public boolean useFixedProfitATR;
   public double fixedProfitATRMutliplierMin = 1.0;
   public double fixedProfitATRMutliplierMax = 1.0;
   public int fixedProfitATRPeriodMin = 20;
   public int fixedProfitATRPeriodMax = 20;
   public boolean useTS;
   public boolean useTSFixedPips;
   public int tsFixedPipsMin = 30;
   public int tsFixedPipsMax = 80;
   public boolean useTSATR;
   public double tsATRMutliplierMin = 1.0;
   public double tsATRMutliplierMax = 1.0;
   public int tsATRPeriodMin = 30;
   public int tsATRPeriodMax = 80;
   public boolean useNone;
   public boolean useNoneLimit;
   public int noneMinBars = 50;
   public int noneMaxBars = 50;
   public boolean scenario_2exits_5050 = false;
   public boolean scenario_2exits_3366 = false;
   public boolean scenario_2exits_6633 = false;
   public boolean scenario_3exits_333333 = false;
   public boolean scenario_3exits_502525 = false;
   public boolean scenario_3exits_255025 = false;
   public boolean scenario_3exits_252550 = false;
   private int configHash;
   private int sizeDecimals;
   private double minimalSize;
   private int totalEnabledScenarios;
   private Element elMoveSL2BE;
   private int totalEnabledExits;

   public void init(Element var1, int var2, double var3) throws Exception {
      if (var1 != null) {
         this.sizeDecimals = var2;
         this.minimalSize = var3;
         String var5 = XMLUtil.xmlToStringRaw(var1);
         this.configHash = var5.hashCode() + Integer.hashCode(var2) + Double.hashCode(var3);
         Element var6 = XMLUtil.getChildElem(var1, "Types");
         Element var7 = XMLUtil.getChildElem(var6, "SLMultiple");
         this.useSLMultiple = XMLUtil.getBooleanAttr(var7, "use", false);
         this.slMultipleMin = XMLUtil.getDoubleAttr(var7, "minMultiplier", 1.0);
         this.slMultipleMax = XMLUtil.getDoubleAttr(var7, "maxMultiplier", 1.0);
         Element var8 = XMLUtil.getChildElem(var6, "PTMultiple");
         this.usePTMultiple = XMLUtil.getBooleanAttr(var8, "use", false);
         this.ptMultipleMin = XMLUtil.getDoubleAttr(var8, "minMultiplier", 1.0);
         this.ptMultipleMax = XMLUtil.getDoubleAttr(var8, "maxMultiplier", 1.0);
         Element var9 = XMLUtil.getChildElem(var6, "FixedProfit");
         this.useFixedProfit = XMLUtil.getBooleanAttr(var9, "use", false);
         this.useFixedProfitFixedPips = XMLUtil.getBooleanAttr(var9, "useFixedPips", false);
         this.fixedProfitFixedPipsMin = XMLUtil.getIntAttr(var9, "minPips", 30);
         this.fixedProfitFixedPipsMax = XMLUtil.getIntAttr(var9, "maxPips", 80);
         this.useFixedProfitATR = XMLUtil.getBooleanAttr(var9, "useATR", false);
         this.fixedProfitATRMutliplierMin = XMLUtil.getDoubleAttr(var9, "minMultiplier", 1.5);
         this.fixedProfitATRMutliplierMax = XMLUtil.getDoubleAttr(var9, "maxMultiplier", 1.5);
         this.fixedProfitATRPeriodMin = XMLUtil.getIntAttr(var9, "minATRPeriod", 20);
         this.fixedProfitATRPeriodMax = XMLUtil.getIntAttr(var9, "maxATRPeriod", 20);
         Element var10 = XMLUtil.getChildElem(var6, "TrailingStop");
         this.useTS = XMLUtil.getBooleanAttr(var10, "use", false);
         this.useTSFixedPips = XMLUtil.getBooleanAttr(var10, "useFixedPips", false);
         this.tsFixedPipsMin = XMLUtil.getIntAttr(var10, "minPips", 30);
         this.tsFixedPipsMax = XMLUtil.getIntAttr(var10, "maxPips", 80);
         this.useTSATR = XMLUtil.getBooleanAttr(var10, "useATR", false);
         this.tsATRMutliplierMin = XMLUtil.getDoubleAttr(var10, "minMultiplier", 1.5);
         this.tsATRMutliplierMax = XMLUtil.getDoubleAttr(var10, "maxMultiplier", 1.5);
         this.tsATRPeriodMin = XMLUtil.getIntAttr(var10, "minATRPeriod", 20);
         this.tsATRPeriodMax = XMLUtil.getIntAttr(var10, "maxATRPeriod", 20);
         Element var11 = XMLUtil.getChildElem(var6, "None");
         this.useNoneLimit = XMLUtil.getBooleanAttr(var11, "useLimit", false);
         this.noneMinBars = XMLUtil.getIntAttr(var11, "minBars", 50);
         this.noneMaxBars = XMLUtil.getIntAttr(var11, "maxBars", 50);
         Element var12 = XMLUtil.getChildElem(var1, "Scenarios");
         Element var13 = XMLUtil.getChildElem(var12, "TwoExits");
         this.scenario_2exits_5050 = XMLUtil.getBooleanAttr(var13, "exit5050", false);
         this.scenario_2exits_3366 = XMLUtil.getBooleanAttr(var13, "exit3366", false);
         this.scenario_2exits_6633 = XMLUtil.getBooleanAttr(var13, "exit6633", false);
         Element var14 = XMLUtil.getChildElem(var12, "ThreeExits");
         this.scenario_3exits_333333 = XMLUtil.getBooleanAttr(var14, "exit333333", false);
         this.scenario_3exits_502525 = XMLUtil.getBooleanAttr(var14, "exit502525", false);
         this.scenario_3exits_255025 = XMLUtil.getBooleanAttr(var14, "exit255025", false);
         this.scenario_3exits_252550 = XMLUtil.getBooleanAttr(var14, "exit252550", false);
         this.totalEnabledExits = this.countEnabledExits();
         this.totalEnabledScenarios = this.countEnabledScenarios();
         this.elMoveSL2BE = this.generateConstantMoveSL2BE();
      }
   }

   private int countEnabledExits() {
      int var1 = 0;
      if (this.useSLMultiple) {
         var1++;
      }

      if (this.usePTMultiple) {
         var1++;
      }

      if (this.useFixedProfit) {
         var1++;
      }

      if (this.useTS) {
         var1++;
      }

      if (this.useNoneLimit) {
         var1++;
      }

      return var1;
   }

   private Element generateConstantMoveSL2BE() {
      Element var1 = new Element("MoveSL2BE");
      XMLUtil.setItemBoolParam(var1, "MoveSL2BE", false);
      XMLUtil.setItemBoolParam(var1, "MoveSL2BEAddPips", false);
      XMLUtil.setItemIntParam(var1, "Type", 1);
      XMLUtil.setItemIntParam(var1, "AddPipsValue", 10);
      return var1;
   }

   private int countEnabledScenarios() {
      int var1 = 0;
      if (this.scenario_2exits_5050) {
         var1++;
      }

      if (this.scenario_2exits_3366) {
         var1++;
      }

      if (this.scenario_2exits_6633) {
         var1++;
      }

      if (this.scenario_3exits_333333) {
         var1++;
      }

      if (this.scenario_3exits_502525) {
         var1++;
      }

      if (this.scenario_3exits_255025) {
         var1++;
      }

      if (this.scenario_3exits_252550) {
         var1++;
      }

      return var1;
   }

   public int getHash() {
      return this.configHash;
   }

   public Element generate(IRandomGenerator var1) throws GenerateException {
      Element var2 = new Element("ATMs");
      var2.setAttribute("enable", "true");
      var2.setAttribute("source", "config");
      var2.setAttribute("generated", "true");
      var2.setAttribute("sizeDecimals", Integer.toString(this.sizeDecimals));
      var2.setAttribute("minSize", Double.toString(this.minimalSize));
      Element var3 = new Element("ATM");
      var2.addContent(var3);
      var3.setAttribute("id", "global");
      var3.setAttribute("sizeDecimals", Integer.toString(this.sizeDecimals));
      var3.setAttribute("minimalSize", Double.toString(this.minimalSize));
      Element var4 = new Element("Exits");
      var3.addContent(var4);
      int var5 = this.chooseScenario(var1);

      for (int var6 = 0; var6 < this.getScenarioExitCount(var5); var6++) {
         Element var7 = this.generateScenarioExit(var6, var5, var1);
         var4.addContent(var7);
      }

      return var2;
   }

   private Element generateScenarioExit(int var1, int var2, IRandomGenerator var3) throws GenerateException {
      Element var4 = new Element("Exit");
      Element var5 = this.generateExitPositionSize(var1, var2, var3);
      var4.addContent(var5);
      Element var6 = this.generateExitExitLevel(var1, var2, var3);
      var4.addContent(var6);
      Element var7 = this.generateExitMoveSL2BE(var1, var2, var3);
      var4.addContent(var7);
      return var4;
   }

   private Element generateExitExitLevel(int var1, int var2, IRandomGenerator var3) throws GenerateException {
      Element var4 = new Element("ExitLevels");
      String var5 = this.chooseExitLevel(var2, var3);
      Element var6 = this.generateExitLevelParams(var5, var3);
      var4.addContent(var6);
      return var4;
   }

   private Element generateExitLevelParams(String var1, IRandomGenerator var2) throws GenerateException {
      switch (var1) {
         case "MultipleOfOriginalSL":
            return MultipleOfOriginalSL.generate(var2, this);
         case "MultipleOfOriginalPT":
            return MultipleOfOriginalPT.generate(var2, this);
         case "FixedProfit":
            return FixedProfit.generate(var2, this);
         case "TrailingStop":
            return TrailingStop.generate(var2, this);
         case "None":
            return None.generate(var2, this);
         default:
            throw new GenerateException("Unknown exit level type '" + var1 + "'");
      }
   }

   private String chooseExitLevel(int var1, IRandomGenerator var2) throws GenerateException {
      if (this.totalEnabledExits == 0) {
         throw new GenerateException("No ATM exit chosen!");
      }

      int var3 = 0;
      if (this.totalEnabledScenarios > 1) {
         var3 = var2.nextInt(this.totalEnabledExits);
      }

      int var4 = 0;
      if (this.useSLMultiple) {
         if (var3 == var4) {
            return "MultipleOfOriginalSL";
         }

         var4++;
      }

      if (this.usePTMultiple) {
         if (var3 == var4) {
            return "MultipleOfOriginalPT";
         }

         var4++;
      }

      if (this.useFixedProfit) {
         if (var3 == var4) {
            return "FixedProfit";
         }

         var4++;
      }

      if (this.useTS) {
         if (var3 == var4) {
            return "TrailingStop";
         }

         var4++;
      }

      if (this.useNoneLimit) {
         if (var3 == var4) {
            return "None";
         }

         var4++;
      }

      throw new GenerateException("Non-existing ATM exit level chosen!");
   }

   private Element generateExitPositionSize(int var1, int var2, IRandomGenerator var3) throws GenerateException {
      Element var4 = new Element("PositionSizes");
      Element var5 = new Element("PositionSize");
      var4.addContent(var5);
      var5.setAttribute("key", "SizePercents");
      var5.setAttribute("use", "true");
      byte var6;
      switch (var2) {
         case 100:
            var6 = 50;
            break;
         case 101:
            var6 = (byte)(var1 == 0 ? 33 : 66);
            break;
         case 102:
            var6 = (byte)(var1 == 0 ? 66 : 33);
            break;
         case 200:
            var6 = 33;
            break;
         case 201:
            var6 = (byte)(var1 == 0 ? 50 : 25);
            break;
         case 202:
            var6 = (byte)(var1 == 1 ? 50 : 25);
            break;
         case 203:
            var6 = (byte)(var1 == 2 ? 50 : 25);
            break;
         default:
            throw new GenerateException("Cannot generate position size for non-existing ATM scenario: " + var2 + "!");
      }

      XMLUtil.setItemIntParam(var5, "Percents", var6);
      return var4;
   }

   private Element generateExitMoveSL2BE(int var1, int var2, IRandomGenerator var3) {
      return this.elMoveSL2BE.clone();
   }

   private int getScenarioExitCount(int var1) throws GenerateException {
      switch (var1) {
         case 100:
         case 101:
         case 102:
            return 2;
         case 200:
         case 201:
         case 202:
         case 203:
            return 3;
         default:
            throw new GenerateException("Non'existing ATM scenario chosen: " + var1 + "!");
      }
   }

   private int chooseScenario(IRandomGenerator var1) throws GenerateException {
      if (this.totalEnabledScenarios == 0) {
         throw new GenerateException("No ATM scenario chosen!");
      }

      int var2 = 0;
      if (this.totalEnabledScenarios > 1) {
         var2 = var1.nextInt(this.totalEnabledScenarios);
      }

      int var3 = 0;
      if (this.scenario_2exits_5050) {
         if (var2 == var3) {
            return 100;
         }

         var3++;
      }

      if (this.scenario_2exits_3366) {
         if (var2 == var3) {
            return 101;
         }

         var3++;
      }

      if (this.scenario_2exits_6633) {
         if (var2 == var3) {
            return 102;
         }

         var3++;
      }

      if (this.scenario_3exits_333333) {
         if (var2 == var3) {
            return 200;
         }

         var3++;
      }

      if (this.scenario_3exits_502525) {
         if (var2 == var3) {
            return 201;
         }

         var3++;
      }

      if (this.scenario_3exits_255025) {
         if (var2 == var3) {
            return 202;
         }

         var3++;
      }

      if (this.scenario_3exits_252550) {
         if (var2 == var3) {
            return 203;
         }

         var3++;
      }

      throw new GenerateException("Non-existing ATM scenario chosen!");
   }

   public void verifyConfig(TaskSettingsData var1) {
      if (this.totalEnabledExits == 0) {
         var1.addError("ATM settings", null, "You have no exits enabled for ATR generation!");
      }

      if (this.totalEnabledScenarios == 0) {
         var1.addError("ATM settings", null, "You have no scenarios enabled for ATR generation!");
      }

      if (this.useFixedProfit && !this.useFixedProfitFixedPips && !this.useFixedProfitATR) {
         var1.addError("ATM settings", null, "You have enabled Use fixed profit but none of its option in ATM generation (fixed or ATR)!");
      }

      if (this.useTS && !this.useTSFixedPips && !this.useTSATR) {
         var1.addError("ATM settings", null, "You have enabled Use fixed profit but none of its option in ATM generation (fixed or ATR)!");
      }
   }
}
