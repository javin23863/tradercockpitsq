package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.OptimizationConst;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import com.strategyquant.tradinglib.optimization.WalkForwardScore;
import com.strategyquant.tradinglib.optimization.WalkForwardStability;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForwardResult implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger(WalkForwardResult.class);
   public ArrayList<WalkForwardPeriod> wfPeriods = null;
   public String resultName = null;
   public int param1;
   public int param2;
   public String testParams;
   public SQStats stats = null;
   public SQStats statsOOS = null;
   public SQStats statsStability = null;
   public SQStats statsScore = null;
   public SQStats statsSpecial = null;
   public int testedConditions = 0;
   public int passedConditions = 0;
   public int scorePerc = 0;
   public boolean passed = false;

   public Element getXML() {
      Element var1 = new Element("RunResult");
      XMLUtil.trySetAttr(var1, "param1", this.param1);
      XMLUtil.trySetAttr(var1, "param2", this.param2);
      XMLUtil.trySetAttr(var1, "testParams", this.testParams);
      XMLUtil.trySetAttr(var1, "resultName", this.resultName);
      Element var2 = XMLUtil.valueToElement("stats", this.stats);
      if (var2 != null) {
         var1.addContent(var2);
      }

      XMLUtil.trySetAttr(var1, "stats", this.stats);
      var2 = XMLUtil.valueToElement("statsOOS", this.statsOOS);
      if (var2 != null) {
         var1.addContent(var2);
      }

      XMLUtil.trySetAttr(var1, "statsOOS", this.statsOOS);
      Element var3 = new Element("Periods");
      var1.addContent(var3);

      for (int var4 = 0; var4 < this.wfPeriods.size(); var4++) {
         Element var5 = this.wfPeriods.get(var4).getXML();
         var3.addContent(var5);
      }

      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.param1 = XMLUtil.tryGetIntAttr(var1, "param1");
      this.param2 = XMLUtil.tryGetIntAttr(var1, "param2");
      this.testParams = var1.getAttributeValue("testParams");
      this.resultName = var1.getAttributeValue("resultName");
      Element var2 = var1.getChild("stats").getChild("SQStats");
      if (this.stats == null) {
         this.stats = new SQStats();
      }

      this.stats.setFromXML(var2);
      if (this.statsOOS == null) {
         this.statsOOS = new SQStats();
      }

      var2 = var1.getChild("statsOOS");
      if (var2 != null) {
         var2 = var2.getChild("SQStats");
         this.statsOOS.setFromXML(var2);
      }

      if (this.wfPeriods == null) {
         this.wfPeriods = new ArrayList<>();
      } else {
         this.wfPeriods.clear();
      }

      Element var3 = var1.getChild("Periods");

      for (Element var5 : var3.getChildren()) {
         WalkForwardPeriod var6 = new WalkForwardPeriod();
         var6.setFromXML(var5);
         this.wfPeriods.add(var6);
      }
   }

   public String toString(int var1) {
      return OptimizationConst.printWFByPeriodType(var1, this.param2, this.param1);
   }

   public String getResultKeyName() {
      return this.resultName;
   }

   public int computeRobustnessScore(ResultsGroup var1, ArrayList<Condition> var2, int var3, int var4) throws Exception {
      this.testedConditions = 0;
      this.passedConditions = 0;
      this.scorePerc = 100;
      this.passed = true;
      this.computeStats(var1);

      try {
         if (var2 == null) {
            return this.scorePerc;
         }

         for (int var6 = 0; var6 < var2.size(); var6++) {
            Condition var5 = (Condition)var2.get(var6);
            if (var5.isUsed()) {
               try {
                  boolean var7 = var5.isMet(var1, this);
                  this.testedConditions++;
                  if (var7) {
                     this.passedConditions++;
                  }
               } catch (Exception var8) {
                  Log.error(String.format("Cannot evaluate condition %s. Exc.", var5.toString()), var8);
               }
            }
         }

         if (this.testedConditions > 0) {
            this.scorePerc = SQUtils.round(SQUtils.safeDivide(this.passedConditions, this.testedConditions) * 100.0);
            this.passed = this.scorePerc >= var3;
         }
      } catch (Exception var9) {
         throw new Exception(L.t("Error while computing Robustness Result of combination '%s'.", new Object[]{this.toString(var4)}), var9);
      }

      this.statsSpecial.set("WFScore", (double)this.scorePerc);
      return this.scorePerc;
   }

   public void computeStats(ResultsGroup var1) {
      if (this.statsStability == null) {
         this.statsStability = new SQStats();
         this.statsScore = new SQStats();
         this.statsSpecial = new SQStats();

         for (DatabankColumn var3 : DatabankColumns.get().getAvailableClasses()) {
            WalkForwardStability.compute(var3, this.statsStability, this);
            WalkForwardScore.compute(var3, var1, this.statsScore, this);
         }

         for (WalkForwardColumn var5 : WalkForwardColumns.get().getAvailableClasses()) {
            var5.compute(this, this.statsSpecial);
         }
      }
   }
}
