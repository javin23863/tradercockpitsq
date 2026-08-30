package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.IConditionValue;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import com.strategyquant.tradinglib.table.TableColumn;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossCheckConditionValue implements IConditionValue {
   private static final Logger Log = LoggerFactory.getLogger(CrossCheckConditionValue.class);
   private String columnName;
   private int pctRatio;
   private String crossCheckSettingName;
   private Element elColumn;
   private byte plType;
   private String columnNameText = "NA";
   private ICrossCheck crossCheck = null;
   private TableColumn tableColumn;

   public CrossCheckConditionValue(String var1, String var2, int var3, Element var4) throws Exception {
      this.columnName = var1;
      this.pctRatio = var3;
      this.crossCheckSettingName = var2;
      this.elColumn = var4;
      this.plType = XMLUtil.getByteAttr(var4, "plType", (byte)10);

      try {
         if (DatabankColumns.get().checkClassExists(var1)) {
            this.tableColumn = (TableColumn)DatabankColumns.get().findClassByName(var1);
         } else {
            if (!WalkForwardColumns.get().checkClassExists(var1)) {
               throw new Exception(L.t("Column doesn't exist.", new Object[0]));
            }

            this.tableColumn = (TableColumn)WalkForwardColumns.get().findClassByName(var1);
         }
      } catch (Exception var6) {
         Log.error("Cannot find column wit name '{}'", var1);
         throw new Exception(L.t("Cannot find column wit name '%s'", new Object[]{var1}));
      }

      this.crossCheck = this.findPluginByName();
      this.columnNameText = this.crossCheck.getColumnTitle(this.tableColumn.getName(), var4);
   }

   @Override
   public double getStatsValue(Object... var1) throws Exception {
      ResultsGroup var2 = (ResultsGroup)var1[0];
      double var3 = 0.0;

      try {
         ICrossCheck var5 = this.findPluginByName();
         var3 = var5.getStatsValue(var2, this.columnName, this.elColumn, var1);
      } catch (CrossCheckDataNotExistException var6) {
         throw var6;
      } catch (Exception var7) {
         Log.error("Error while evaluating CrossCheck acceptance condition.", var7);
      }

      return var3;
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
      String var2 = this.crossCheck.getColumnTitle(this.columnName, this.elColumn, var1);
      double var3 = this.getStatsValue(var1);
      String var5 = String.format("%s[" + L.t("Cross Check  data", new Object[]{true}) + "] (%s)", var2, this.tableColumn.printPlValue(var3, this.plType));
      if (this.pctRatio > 0) {
         var5 = this.pctRatio + "% of " + var5;
      }

      return var5;
   }

   @Override
   public String toString() {
      String var1 = String.format("%s[" + L.t("Cross Check  data", new Object[]{true}) + "]", this.columnNameText);
      if (this.pctRatio > 0) {
         var1 = this.pctRatio + "% of " + var1;
      }

      return var1;
   }

   private ICrossCheck findPluginByName() throws Exception {
      for (ICrossCheck var2 : SQPluginManager.getPlugins(ICrossCheck.class)) {
         if (var2.getSettingName().equals(this.crossCheckSettingName)) {
            return var2;
         }
      }

      throw new Exception(L.t("CrossCheck method '%' doesn't exist.", new Object[]{this.crossCheckSettingName}));
   }

   public IConditionValue getClone() throws Exception {
      return new CrossCheckConditionValue(this.columnName, this.crossCheckSettingName, this.pctRatio, this.elColumn);
   }

   public String printValue(Object... var1) throws Exception {
      ResultsGroup var2 = (ResultsGroup)var1[0];
      ICrossCheck var3 = this.findPluginByName();
      return this.tableColumn != null && this.tableColumn.isSpecialValue()
         ? var3.printSpecialValue(var2, this.columnName, this.elColumn, var1)
         : Double.toString(var3.getStatsValue(var2, this.columnName, this.elColumn, var1));
   }

   @Override
   public String getTitle() throws Exception {
      return this.columnNameText;
   }

   @Override
   public String getCrossCheckSettingName() {
      return this.crossCheckSettingName;
   }

   public boolean hasValue(Object... var1) {
      try {
         ResultsGroup var2 = (ResultsGroup)var1[0];
         ICrossCheck var3 = this.findPluginByName();
         return var3.hasStatsValue(var2, this.columnName, this.elColumn, var1);
      } catch (Exception var4) {
         Log.error("Error while evaluating CrossCheck acceptance condition.", var4);
         return false;
      }
   }

   @Override
   public String getConditionParamValue(String var1, String var2) {
      if (var1.equals("AdditionalMarket")) {
         return XMLUtil.getIntAttr(this.elColumn, "market", 1) + "";
      } else {
         return var1.equals("SampleType") ? XMLUtil.getByteAttr(this.elColumn, "sampleType", (byte)127) + "" : var2;
      }
   }

   @Override
   public boolean statsExists(Object... var1) {
      try {
         ResultsGroup var2 = (ResultsGroup)var1[0];
         ICrossCheck var3 = this.findPluginByName();
         return var3.hasStatsValue(var2, this.columnName, this.elColumn, var1);
      } catch (Exception var4) {
         Log.error("Error while evaluating CrossCheck acceptance condition.", var4);
         return false;
      }
   }

   @Override
   public String printFormatedValue(double var1) throws Exception {
      return this.tableColumn.printPlValue(var1, this.plType);
   }
}
