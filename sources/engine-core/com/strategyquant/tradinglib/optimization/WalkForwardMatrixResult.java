package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import java.util.ArrayList;
import org.jdom2.Element;

public class WalkForwardMatrixResult implements IXMLAble {
   private ArrayList<WalkForwardResult> results = new ArrayList<>();
   public int start1;
   public int stop1;
   public int increment1;
   public int start2;
   public int stop2;
   public int increment2;
   public int activeParam1;
   public int activeParam2;
   public int periodType;
   public ArrayList<Condition> conditions = null;
   public Element conditionsElem;
   public int thresholdPct = -1;
   public int rows = -1;
   public int cols = -1;
   public int comb = -1;
   public boolean passed = false;
   public String dismissalMessage = null;
   public int combinationsTotal = 0;
   public int combinationsPassed = 0;
   public int condPassedInBestGroup = 0;
   public String bestGroupArroundCell = "N/A";
   public String recommendedCombination = "N/A";
   public WalkForwardResult bestGroupArroundWFCell = null;
   public int numberOfPassedCombinationInBestGroup = -1;

   public void addWFResult(int var1, int var2, WalkForwardResult var3) {
      var3.param1 = var1;
      var3.param2 = var2;
      this.results.add(var3);
   }

   public WalkForwardResult getWFResult(int var1, int var2) throws Exception {
      for (int var3 = 0; var3 < this.results.size(); var3++) {
         WalkForwardResult var4 = this.results.get(var3);
         if (var4.param1 == var1 && var4.param2 == var2) {
            return var4;
         }
      }

      throw new Exception("No WalkForwardResult exists for " + var1 + " / " + var2);
   }

   public WalkForwardResult getWFResult(String var1, boolean var2) throws Exception {
      if (var1 == null) {
         return null;
      }

      for (int var3 = 0; var3 < this.results.size(); var3++) {
         WalkForwardResult var4 = this.results.get(var3);
         if (var4.resultName.equals(var1)) {
            return var4;
         }
      }

      if (var2) {
         return null;
      } else {
         return this.getResultList().size() > 1 ? this.getWFResult(this.start1, this.start2) : this.getResultList().get(0);
      }
   }

   public ArrayList<WalkForwardResult> getResultList() {
      return this.results;
   }

   public boolean isMatrix() {
      return this.getResultList().size() > 1;
   }

   public Element getXML() {
      Element var1 = new Element("MatrixResult");
      XMLUtil.trySetAttr(var1, "start1", this.start1);
      XMLUtil.trySetAttr(var1, "stop1", this.stop1);
      XMLUtil.trySetAttr(var1, "increment1", this.increment1);
      XMLUtil.trySetAttr(var1, "start2", this.start2);
      XMLUtil.trySetAttr(var1, "stop2", this.stop2);
      XMLUtil.trySetAttr(var1, "increment2", this.increment2);
      XMLUtil.trySetAttr(var1, "activeParam1", this.activeParam1);
      XMLUtil.trySetAttr(var1, "activeParam2", this.activeParam2);
      XMLUtil.trySetAttr(var1, "periodType", this.periodType);

      for (int var2 = 0; var2 < this.results.size(); var2++) {
         Element var3 = this.results.get(var2).getXML();
         var1.addContent(var3);
      }

      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.start1 = XMLUtil.tryGetIntAttr(var1, "start1");
      this.stop1 = XMLUtil.tryGetIntAttr(var1, "stop1");
      this.increment1 = XMLUtil.tryGetIntAttr(var1, "increment1");
      this.start2 = XMLUtil.tryGetIntAttr(var1, "start2");
      this.stop2 = XMLUtil.tryGetIntAttr(var1, "stop2");
      this.increment2 = XMLUtil.tryGetIntAttr(var1, "increment2");
      this.activeParam1 = XMLUtil.tryGetIntAttr(var1, "activeParam1");
      this.activeParam2 = XMLUtil.tryGetIntAttr(var1, "activeParam2");
      this.periodType = XMLUtil.getIntAttr(var1, "periodType", 10);
      this.results.clear();

      for (Element var3 : var1.getChildren()) {
         WalkForwardResult var4 = new WalkForwardResult();
         var4.setFromXML(var3);
         this.results.add(var4);
      }
   }

   private WalkForwardResult[][] createMatrix() throws Exception {
      int var1 = 0;
      int var2 = 0;

      for (int var3 = this.start2; var3 <= this.stop2; var3 += this.increment2) {
         var1++;
      }

      for (int var9 = this.start1; var9 <= this.stop1; var9 += this.increment1) {
         var2++;
      }

      WalkForwardResult[][] var10 = new WalkForwardResult[var1][var2];
      int var4 = 0;
      int var5 = 0;

      for (int var6 = this.start2; var6 <= this.stop2; var6 += this.increment2) {
         for (int var7 = this.start1; var7 <= this.stop1; var7 += this.increment1) {
            WalkForwardResult var8 = this.getWFResult(var7, var6);
            var10[var5][var4] = var8;
            var4++;
         }

         var5++;
         var4 = 0;
      }

      return var10;
   }

   public String computeBestWF(ResultsGroup var1) throws Exception {
      String var2 = null;
      if (this.isMatrix()) {
         Element var3 = (Element)var1.specialValues().get("WalkForwardConditions");
         ArrayList var4 = ProjectConfigHelper.getConditions(var3);
         int var5 = XMLUtil.getIntAttr(var3, "thresholdPct", 80);
         int var6 = XMLUtil.getIntAttr(var3, "robCombRows", 3);
         int var7 = XMLUtil.getIntAttr(var3, "robCombCols", 3);
         int var8 = XMLUtil.getIntAttr(var3, "robMinComb", 7);
         this.computeRobustnessResults(var1, var3, var4, var5, var6, var7, var8);
         var2 = this.bestGroupArroundWFCell.getResultKeyName();
      } else {
         WalkForwardResult var10 = this.getResultList().get(0);
         var10.computeStats(var1);
         var2 = var10.getResultKeyName();
      }

      return var2;
   }

   public void computeRobustnessResults(ResultsGroup var1, Element var2, ArrayList<Condition> var3, int var4, int var5, int var6, int var7) throws Exception {
      this.conditionsElem = var2;
      this.conditions = var3;
      this.thresholdPct = var4;
      this.rows = var5;
      this.cols = var6;
      this.comb = var7;
      this.combinationsTotal = 0;
      this.combinationsPassed = 0;
      this.dismissalMessage = null;
      WalkForwardResult var8 = null;
      if (this.results.size() > 1) {
         for (int var9 = this.start2; var9 <= this.stop2; var9 += this.increment2) {
            for (int var10 = this.start1; var10 <= this.stop1; var10 += this.increment1) {
               this.combinationsTotal++;
               var8 = this.getWFResult(var10, var9);
               var8.computeRobustnessScore(var1, var3, var4, this.periodType);
               if (var8.passed) {
                  this.combinationsPassed++;
               }
            }
         }

         this.passed = this.findBestGroupOfPassedCombinations(var5, var6, var7);
         if (!this.passed) {
            this.dismissalMessage = "Robustness score didn't pass.";
         }

         this.condPassedInBestGroup = this.numberOfPassedCombinationInBestGroup;
         this.bestGroupArroundCell = this.bestGroupArroundWFCell.toString(this.periodType);
         WalkForwardPeriod var13 = this.bestGroupArroundWFCell.wfPeriods.get(0);
         this.recommendedCombination = String.format(
            "Recommended combination: %s, which means:<br/><b>reoptimizing every %d days on history of %d days</b>",
            this.bestGroupArroundWFCell.toString(this.periodType),
            SQTimeOld.getDaysBetween(var13.runFrom, var13.runTo),
            SQTimeOld.getDaysBetween(var13.optimizeFrom, var13.optimizeTo)
         );
      } else {
         var8 = this.results.get(0);
         int var14 = var8.computeRobustnessScore(var1, var3, var4, this.periodType);
         this.passed = var8.passed;
         if (!this.passed) {
            this.dismissalMessage = String.format("Robustness score %d%% didn't pass %d%%.", var14, var4);
         }
      }
   }

   private boolean findBestGroupOfPassedCombinations(int var1, int var2, int var3) throws Exception {
      this.bestGroupArroundWFCell = null;
      this.numberOfPassedCombinationInBestGroup = -1;
      WalkForwardResult[][] var4 = this.createMatrix();
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;
      boolean var8 = false;
      int var9 = var4.length;
      int var10 = var4[0].length;
      int var11 = 0;
      if (var1 <= var9 && var2 <= var10) {
         for (int var14 = 0; var14 < var9 && var14 + (var1 - 1) < var9; var14++) {
            for (int var15 = 0; var15 < var10 && var15 + (var2 - 1) < var10; var15++) {
               var11 = 0;

               for (int var16 = var14; var16 < var14 + var1 && var16 < var9; var16++) {
                  for (int var17 = var15; var17 < var15 + var2 && var17 < var10; var17++) {
                     WalkForwardResult var19 = var4[var16][var17];
                     if (var19.passed) {
                        var11++;
                     }
                  }
               }

               WalkForwardResult var20 = var4[var14 + (var1 - 1) / 2][var15 + (var2 - 1) / 2];
               if (this.numberOfPassedCombinationInBestGroup < var11) {
                  this.numberOfPassedCombinationInBestGroup = var11;
                  this.bestGroupArroundWFCell = var20;
               }
            }
         }
      } else {
         for (WalkForwardResult var13 : this.results) {
            if (this.bestGroupArroundWFCell == null || this.bestGroupArroundWFCell.scorePerc < var13.scorePerc) {
               this.bestGroupArroundWFCell = var13;
               this.numberOfPassedCombinationInBestGroup = 1;
            }
         }
      }

      return this.numberOfPassedCombinationInBestGroup >= var3;
   }
}
