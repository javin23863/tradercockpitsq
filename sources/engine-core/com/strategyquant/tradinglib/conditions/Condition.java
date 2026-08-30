package com.strategyquant.tradinglib.conditions;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.crosscheck.CrossCheckConditionValue;

public class Condition implements ISQCloneable<Condition> {
   private boolean use;
   private IConditionValue leftSide;
   private IConditionValue rightSide;
   private String comparator;
   private double leftStatsValue;
   private double rightStatsValue;

   public Condition(boolean var1, IConditionValue var2, IConditionValue var3, String var4) {
      this.use = var1;
      this.leftSide = var2;
      this.rightSide = var3;
      this.comparator = var4;
   }

   public Condition getClone() throws Exception {
      return new Condition(this.use, (IConditionValue)this.leftSide.getClone(), (IConditionValue)this.rightSide.getClone(), this.comparator);
   }

   public boolean isMet(Object... var1) throws Exception {
      if (this.leftSide == null || this.rightSide == null) {
         return true;
      } else if (this.leftSide.statsExists(var1) && this.rightSide.statsExists(var1)) {
         double var2 = SQUtils.round2(this.leftSide.getValue(var1));
         double var4 = SQUtils.round2(this.rightSide.getValue(var1));
         this.leftStatsValue = this.leftSide.getStatsValue(var1);
         this.rightStatsValue = this.rightSide.getStatsValue(var1);
         return !this.use ? false : validate(var2 - var4, this.comparator);
      } else {
         return true;
      }
   }

   public boolean isMet(ResultsGroup var1, Object var2, Object var3) throws Exception {
      if (this.leftSide != null && this.rightSide != null) {
         if (this.leftSide.statsExists(var1, var2) && this.rightSide.statsExists(var1, var3)) {
            double var4 = SQUtils.round2(this.leftSide.getValue(var1, var2));
            double var6 = SQUtils.round2(this.rightSide.getValue(var1, var3));
            this.leftStatsValue = this.leftSide.getStatsValue(var1, var2);
            this.rightStatsValue = this.rightSide.getStatsValue(var1, var3);
            return !this.use ? false : validate(var4 - var6, this.comparator);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean validate(double var0, String var2) throws Exception {
      switch (var2) {
         case ">":
            if (var0 > 0.0) {
               return true;
            }

            return false;
         case "<":
            if (var0 < 0.0) {
               return true;
            }

            return false;
         case "=":
            if (var0 == 0.0) {
               return true;
            }

            return false;
         case ">=":
            if (var0 >= 0.0) {
               return true;
            }

            return false;
         case "<=":
            if (var0 <= 0.0) {
               return true;
            }

            return false;
         case "<>":
            if (var0 != 0.0) {
               return true;
            }

            return false;
         default:
            throw new Exception(L.t("Unknown comparator '%s'", new Object[]{var2}));
      }
   }

   public String getAsText(Object... var1) throws Exception {
      StringBuilder var2 = new StringBuilder();
      var2.append(this.leftSide.getLabel(var1));
      var2.append(" ");
      var2.append(this.comparator);
      var2.append(" ");
      var2.append(this.rightSide.getLabel(var1));
      return var2.toString();
   }

   public String getAsText(ResultsGroup var1, Object var2, Object var3) throws Exception {
      StringBuilder var4 = new StringBuilder();
      var4.append(this.leftSide.getLabel(var1, var2));
      var4.append(" ");
      var4.append(this.comparator);
      var4.append(" ");
      var4.append(this.rightSide.getLabel(var1, var3));
      return var4.toString();
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.leftSide.toString());
      var1.append(" ");
      var1.append(this.comparator);
      var1.append(" ");
      var1.append(this.rightSide.toString());
      return var1.toString();
   }

   public double getLeftStatsValue() {
      return this.leftStatsValue;
   }

   public double getRightStatsValue() {
      return this.rightStatsValue;
   }

   public IConditionValue getLeftSideConditionValue() {
      return this.leftSide;
   }

   public IConditionValue getRightSideConditionValue() {
      return this.rightSide;
   }

   public boolean isUsed() {
      return this.use;
   }

   public boolean isFitnessCondition() {
      return this.leftSide.toString().contains("Fitness") || this.rightSide.toString().contains("Fitness");
   }

   public boolean isCrossCheckCondition() {
      return CrossCheckConditionValue.class.isAssignableFrom(this.leftSide.getClass())
         || CrossCheckConditionValue.class.isAssignableFrom(this.rightSide.getClass());
   }

   public boolean isCrossCheckCondition(String var1) {
      return this.leftSide.getCrossCheckSettingName() != null && this.leftSide.getCrossCheckSettingName().contains(var1)
         || this.rightSide.getCrossCheckSettingName() != null && this.rightSide.getCrossCheckSettingName().contains(var1);
   }

   public boolean isPortfolioCondition() {
      return this.leftSide.toString().contains("Portfolio") || this.rightSide.toString().contains("Portfolio");
   }
}
