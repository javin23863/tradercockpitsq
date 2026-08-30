package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.table.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankColumn extends TableColumn implements IFitnessValue {
   protected static final Logger Log = LoggerFactory.getLogger(DatabankColumn.class);
   public byte valueType = -1;
   private double target;
   private double avgMin;
   private double avgMax;
   private String[] dependsOn = null;
   private byte[] plTypeRestrictions;
   private byte[] directionRestrictions;
   private String className = null;
   private boolean dependentOnTradingPeriod = false;

   public DatabankColumn(String var1, String var2, byte var3, double var4, double var6, double var8) {
      super(var1, var2);
      this.valueType = var3;
      this.statsValueName = this.getClass().getSimpleName();
      this.target = var4;
      this.avgMin = var6;
      this.avgMax = var8;
   }

   public double getTarget() {
      return this.target;
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SQStats var6 = this.getStats(var1, var2, var3, var4, var5);
      if (var6 == null) {
      }

      return this.printValue(var6);
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return this.getValue(var1, var2, var3, var4, var5);
   }

   public CustomCellFormat getCustomFormat(byte var1, byte var2, byte var3, String var4, ResultsGroup var5) {
      return this.customCellFormat;
   }

   public double getNumericValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SQStats var6 = this.getStats(var1, var2, var3, var4, var5);
      return var6 == null ? 0.0 : this.getNumericValue(var6);
   }

   public SQStats getStats(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      if (this.statsValueName == null) {
         throw new Exception(
            L.t(
               "There is no StatsValue defined for databank column: '%s'!\nYou have to define it or override getValue() method!",
               new Object[]{this.getClass().getSimpleName()}
            )
         );
      } else {
         return var1.subResult(var2).statsOrNull(var3, var4, var5);
      }
   }

   public boolean setValue(Object var1, Object var2) throws Exception {
      if (!this.isEditable()) {
         throw new Exception(L.t("Column '%s' is not editable, you cannot call setValue()!", new Object[]{this.getClass().getSimpleName()}));
      } else {
         throw new Exception(L.t("Column '%s' is editable, it has to implement setValue() method!", new Object[]{this.getClass().getSimpleName()}));
      }
   }

   public byte getValueType() {
      return this.valueType;
   }

   public double transformToFitnessRange(double var1, double var3, byte var5) {
      if (var5 == 0) {
         var5 = this.getValueType();
      }

      return this.transformToFitnessRange(var1, var3, this.avgMin, this.avgMax, var5);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7, SettingsMap var8) throws Exception {
      return this.compute(var1, var2, var3, var4, var5, var6, var7);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7) throws Exception {
      return this.compute(var1, var2, var3, var4, var5, var6);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return 0.0;
   }

   protected void setDependencies(String... var1) {
      this.dependsOn = var1;
   }

   public String[] getDependencies() {
      return this.dependsOn;
   }

   protected void setPLTypeRestrictions(byte... var1) {
      this.plTypeRestrictions = var1;
   }

   public byte[] getPLTypeRestrictions() {
      return this.plTypeRestrictions;
   }

   protected void setDirectionRestrictions(byte... var1) {
      this.directionRestrictions = var1;
   }

   public byte[] getDirectionRestrictions() {
      return this.directionRestrictions;
   }

   protected double getPLByStatsType(Order var1, StatsTypeCombination var2) {
      if (var2.getPLType() == 10) {
         return var1.PL;
      } else if (var2.getPLType() == 20) {
         return var1.PctPL_TWR;
      } else {
         return var2.getPLType() == 30 ? var1.PipsPL : 0.0;
      }
   }

   public String getClassName() {
      if (this.className == null) {
         this.className = this.getClass().getSimpleName();
      }

      return this.className;
   }

   public String printHtmlFormatedValue(SQStats var1) throws Exception {
      double var2 = this.getNumericValue(var1);
      CustomCellFormat var4 = this.getCustomFormat((byte)0, (byte)10, (byte)127, Double.toString(var2), null);
      return var4.getHtmlContent(var2);
   }

   public double convertToDoubleSafe(String var1) {
      double var2 = 0.0;

      try {
         var2 = Double.parseDouble(SQUtils.extractNumber(var1));
      } catch (Exception var5) {
      }

      return var2;
   }

   public boolean isDependentOnTradingPeriod() {
      return this.dependentOnTradingPeriod;
   }

   public void setDependentOnTradingPeriod(boolean var1) {
      this.dependentOnTradingPeriod = var1;
   }

   public String getCrossCheckComputedValue(ResultsGroup var1, double var2) throws Exception {
      return null;
   }

   public enum Color {
      RED,
      GREEN,
      BLUE;
   }
}
