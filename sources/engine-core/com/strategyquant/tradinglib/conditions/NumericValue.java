package com.strategyquant.tradinglib.conditions;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;

public class NumericValue implements IConditionValue {
   private double value;

   public NumericValue(double var1) {
      this.value = var1;
   }

   @Override
   public double getStatsValue(Object... var1) throws Exception {
      return this.value;
   }

   @Override
   public double getValue(Object... var1) {
      return this.value;
   }

   @Override
   public String getLabel(Object... var1) {
      return SQUtils.doubleToString(this.value);
   }

   @Override
   public String toString() {
      return SQUtils.d2(this.value);
   }

   public IConditionValue getClone() {
      return new NumericValue(this.value);
   }

   @Override
   public String getTitle() throws Exception {
      return L.t("Number", new Object[0]);
   }

   @Override
   public String getCrossCheckSettingName() {
      return null;
   }

   @Override
   public String getConditionParamValue(String var1, String var2) {
      return var2;
   }

   @Override
   public boolean statsExists(Object... var1) {
      return true;
   }

   @Override
   public String printFormatedValue(double var1) throws Exception {
      return SQUtils.d2(var1);
   }
}
