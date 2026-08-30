package com.strategyquant.tradinglib.table;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.CustomCellFormat;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.debug.Debugger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TableColumn extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger(TableColumn.class);
   public static final int LEFT = 2;
   public static final int RIGHT = 4;
   public static final int CENTER = 0;
   public static final int HORIZONTAL_ALIGNMENT_LEFT = 2;
   public static final int HORIZONTAL_ALIGNMENT_RIGHT = 4;
   public static final int HORIZONTAL_ALIGNMENT_CENTER = 0;
   public static final String Decimal2 = "Decimal2";
   public static final String Decimal4 = "Decimal4";
   public static final String Decimal5 = "Decimal5";
   public static final String Text = "Text";
   public static final String Decimal2Pct = "Decimal2Pct";
   public static final String Decimal2PL = "Decimal2PL";
   public static final String Decimal2Pips = "Decimal2Pips";
   public static final String Integer = "Integer";
   public static final String Time = "Time";
   public static final String currencySymbol = "$";
   public static final String NOT_AVAILABLE = "N/A";
   private String formatType = "Decimal2";
   private int width;
   private String tooltip;
   private String name;
   private boolean editable;
   private boolean fontBold = false;
   private int horizontalAlignment = 2;
   protected CustomCellFormat customCellFormat = new CustomCellFormat();
   protected String statsValueName = null;
   private boolean specialValue = false;

   public TableColumn(String var1, String var2) {
      this(var1, var2, false);
   }

   public TableColumn(String var1, String var2, boolean var3) {
      this.name = var1;
      this.formatType = var2;
      this.width = 80;
      this.editable = false;
      this.tooltip = null;
      this.fontBold = var3;
   }

   public boolean isFontBold() {
      return this.fontBold;
   }

   public String getType() {
      return this.formatType;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int var1) {
      this.width = var1;
   }

   public void setTooltip(String var1) {
      this.tooltip = var1;
   }

   public String getTooltip() {
      return this.tooltip;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public String getStatsValueName() {
      return this.statsValueName;
   }

   public void setEditable(boolean var1) {
      this.editable = var1;
   }

   public boolean isEditable() {
      return this.editable;
   }

   protected double round2(double var1) {
      return SQUtils.round2(var1);
   }

   protected double round4(double var1) {
      return SQUtils.round(var1, 4);
   }

   protected double safeDivide(double var1, double var3) {
      return SQUtils.safeDivide(var1, var3);
   }

   public void printsSpecialValue(boolean var1) {
      this.specialValue = var1;
   }

   public boolean isSpecialValue() {
      return this.specialValue;
   }

   public double getNumericValue(SQStats var1) throws Exception {
      try {
         switch (this.formatType) {
            case "Decimal2":
            case "Decimal4":
            case "Decimal5":
            case "Decimal2PL":
            case "Decimal2Pips":
            case "Decimal2Pct":
            case "Time":
            case "Integer":
               return var1.getDouble(this.statsValueName, 0.0);
            case "Text":
               return 0.0;
            default:
               Log.error("Unknown type: '" + this.formatType + "'");
               throw new Exception("Unknown type: " + this.formatType);
         }
      } catch (Exception var4) {
         Log.debug("Error while obtaining stats value '" + this.statsValueName + "'. Exc.", var4);
         return 0.0;
      }
   }

   public String printValue(SQStats var1) throws Exception {
      double var2 = 0.0;

      try {
         if (var1 == null) {
            return "N/A";
         }

         var2 = var1.getDouble(this.statsValueName, 0.0);
      } catch (Exception var7) {
         Log.warn("Cannot parse double value", var7);
         return "N/A";
      }

      try {
         switch (this.formatType) {
            case "Decimal4":
               return Double.toString(SQUtils.round(var2, 4));
            case "Decimal5":
               return Double.toString(SQUtils.round(var2, 5));
            case "Decimal2":
            case "Decimal2PL":
            case "Decimal2Pips":
            case "Decimal2Pct":
               return Double.toString(SQUtils.round(var2, 2));
            case "Time":
               return Long.toString((long)var2);
            case "Integer":
               return java.lang.Integer.toString((int)var2);
            case "Text":
               return "N/A";
         }
      } catch (ClassCastException var6) {
         Log.debug("Error while obtaining stats value '" + this.statsValueName + "'. Exc.", var6);
      }

      return "N/A";
   }

   public String printPlValue(double var1, byte var3) throws Exception {
      switch (this.formatType) {
         case "Decimal2":
            return Double.toString(SQUtils.round(var1, 2));
         case "Decimal4":
            return Double.toString(SQUtils.round(var1, 4));
         case "Decimal5":
            return Double.toString(SQUtils.round(var1, 5));
         case "Decimal2PL":
            return PlTypes.printPL(var1, var3);
         case "Decimal2Pips":
            return SQUtils.round(var1, 2) + " pips";
         case "Decimal2Pct":
            return SQUtils.round(var1, 2) + " %";
         case "Integer":
            return (int)var1 + "";
         case "Time":
            return (long)var1 + "";
         default:
            return var1 + "";
      }
   }

   public String printPlValue(SQStats var1, byte var2) throws Exception {
      double var3 = this.getNumericValue(var1);
      return this.printPlValue(var3, var2);
   }
}
