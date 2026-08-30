package com.strategyquant.tradinglib.conditions;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import java.util.HashMap;

public class ColumnConditionValue implements IConditionValue {
   private String resultType;
   private byte direction;
   private byte plType;
   private byte sampleType;
   private byte columnType;
   private int pctRatio;
   DatabankColumn column;

   public ColumnConditionValue(DatabankColumn var1, String var2, byte var3, byte var4, byte var5, int var6) {
      this.column = var1;
      this.resultType = var2;
      this.direction = var3;
      this.plType = var4;
      this.sampleType = var5;
      this.pctRatio = var6;
   }

   @Override
   public double getStatsValue(Object... var1) throws Exception {
      ResultsGroup var2 = (ResultsGroup)var1[0];
      String var3 = this.resultType.equals("portfolio") ? "Portfolio" : var2.getMainResultKey();
      return this.column.getNumericValue(var2, var3, this.direction, this.plType, this.getSampleType(var1));
   }

   @Override
   public double getValue(Object... var1) throws Exception {
      double var2 = this.getStatsValue(var1);
      if (this.pctRatio > 0) {
         var2 *= this.pctRatio / 100.0;
      }

      return var2;
   }

   @Override
   public String getLabel(Object... var1) throws Exception {
      double var2 = this.getStatsValue(var1);
      String var4 = String.format("%s (%s)", this.getColumnName(var1), this.column.printPlValue(var2, this.plType));
      if (this.pctRatio > 0) {
         var4 = this.pctRatio + "% of " + var4;
      }

      return var4;
   }

   @Override
   public String toString() {
      String var1 = this.getColumnName();
      if (this.pctRatio > 0) {
         var1 = this.pctRatio + "% of " + var1;
      }

      return var1;
   }

   private String getColumnName(Object... var1) {
      String var2 = "";
      var2 = var2 + ResultTypes.typeToString(this.resultType) + ", ";
      var2 = var2 + (this.sampleType == 127 ? "" : SampleTypes.typeToString(this.getSampleType(var1)) + ", ");
      var2 = var2 + (this.direction == 0 ? "" : Directions.typeToString(this.direction) + ", ");
      var2 = var2 + (this.plType == 10 ? "" : PlTypes.typeToString(this.plType) + ", ");
      var2 = var2.trim();
      var2 = SQUtils.replaceLast(var2, ",", "");
      var2 = "[" + var2 + "]";
      return L.t(this.column.getName(), new Object[0]) + var2;
   }

   public IConditionValue getClone() {
      return new ColumnConditionValue(this.column, this.resultType, this.direction, this.plType, this.sampleType, this.pctRatio);
   }

   @Override
   public String getTitle() throws Exception {
      return this.getColumnName();
   }

   @Override
   public String getCrossCheckSettingName() {
      return null;
   }

   @Override
   public String getConditionParamValue(String var1, String var2) {
      return var1.equals("SampleType") ? this.sampleType + "" : var2;
   }

   @Override
   public boolean statsExists(Object... var1) {
      try {
         ResultsGroup var2 = (ResultsGroup)var1[0];
         String var3 = this.resultType.equals("portfolio") ? "Portfolio" : var2.getMainResultKey();
         return this.column.getStats(var2, var3, this.direction, this.plType, this.getSampleType(var1)) != null;
      } catch (Exception var4) {
         return false;
      }
   }

   private byte getSampleType(Object... var1) {
      byte var2 = this.sampleType;
      if ((this.sampleType == 66 || this.sampleType == 77) && var1.length > 1 && var1[1] instanceof HashMap) {
         HashMap var3 = (HashMap)var1[1];
         if (var3.containsKey("SampleType")) {
            var2 = (Byte)var3.get("SampleType");
         }
      }

      return var2;
   }

   @Override
   public String printFormatedValue(double var1) throws Exception {
      return this.column.printPlValue(var1, this.plType);
   }
}
