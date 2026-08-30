package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.StatsDontExistException;
import com.strategyquant.tradinglib.crosscheck.CrossCheckConditionValue;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankTableColumnEntry implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("TableColumnEntry");
   public DatabankColumn tableColumn;
   public String resultType = "main";
   public byte direction = 0;
   public byte sampleType = 127;
   public byte plType = 10;
   public int confidenceLevel = 50;
   public int market = 1;
   public byte subresult = 30;
   public boolean showMainResult = true;
   public Element elValue = null;

   public DatabankTableColumnEntry() {
   }

   public DatabankTableColumnEntry(DatabankColumn var1, String var2, byte var3, byte var4, byte var5) {
      this(var1, var2, var3, var4, var5, 50, 1, (byte)30, true);
   }

   public DatabankTableColumnEntry(DatabankColumn var1, String var2, byte var3, byte var4, byte var5, int var6, int var7, byte var8, boolean var9) {
      this.tableColumn = var1;
      this.resultType = var2;
      this.direction = var3;
      this.sampleType = var5;
      this.plType = var4;
      this.confidenceLevel = var6;
      this.market = var7;
      this.subresult = var8;
      this.showMainResult = var9;
   }

   public String getColumnEntryName() {
      ICrossCheck var1 = ResultTypes.findCrossCheckByType(this.resultType);
      if (var1 != null) {
         return var1.getColumnTitle(this.tableColumn.getName(), this.elValue);
      }

      String var2 = this.getResultType(this.resultType);
      String var3 = var2.isEmpty() ? "" : var2 + ", ";
      String var4 = this.direction == 0 ? "" : this.getDirection(this.direction);
      String var5 = var4.isEmpty() ? "" : var4 + ", ";
      String var6 = this.sampleType == 0 ? "" : this.getSampleType(this.sampleType);
      String var7 = var6.isEmpty() ? "" : var6 + ", ";
      String var8 = this.plType == 0 ? "" : this.getPlType(this.plType);
      String var9 = var8.isEmpty() ? "" : var8 + ", ";
      String var10 = "";
      var10 = var3 + var5 + var7 + var9;
      if (!var10.equals("")) {
         var10 = var10.trim();
         var10 = SQUtils.replaceLast(var10, ",", "");
         var10 = " (" + var10 + ")";
      }

      return this.tableColumn.getName() + var10;
   }

   private String getResultType(String var1) {
      return var1.equals("main") ? "" : ResultTypes.typeToString(var1);
   }

   private String getDirection(byte var1) {
      switch (var1) {
         case -1:
            return "Short";
         case 0:
            return "";
         case 1:
            return "Long";
         default:
            return "?";
      }
   }

   private String getSampleType(byte var1) {
      return var1 == 127 ? "" : SampleTypes.typeToString(var1);
   }

   private String getPlType(byte var1) {
      switch (var1) {
         case 10:
            return "";
         case 20:
            return "%";
         case 30:
            return "Pips";
         default:
            return "?";
      }
   }

   public String getValue(Object var1) {
      return this.getValue(var1, false);
   }

   public String exportValue(Object var1) {
      return this.getValue(var1, true);
   }

   private String getValue(Object var1, boolean var2) {
      try {
         ResultsGroup var3 = (ResultsGroup)var1;
         if (!ResultTypes.isCrossCheck(this.resultType)) {
            if (var2) {
               return this.tableColumn.exportValue(var3, this.getCorrectResultKey(var3), this.direction, this.plType, this.sampleType);
            }

            return this.tableColumn.getValue(var3, this.getCorrectResultKey(var3), this.direction, this.plType, this.sampleType);
         }

         String var4 = this.tryToReturnWFTextValue(var3, var2);
         if (var4 != null) {
            return var4;
         }

         CrossCheckConditionValue var5 = new CrossCheckConditionValue(this.tableColumn.getClassName(), this.resultType, 0, this.elValue);
         if (var5.hasValue(var3)) {
            if (this.tableColumn.isSpecialValue()) {
               return var5.printValue(var3);
            }

            double var6 = var5.getValue(var3);
            String var8 = this.tableColumn.getCrossCheckComputedValue(var3, var6);
            if (var8 != null) {
               return var8;
            }

            return Double.toString(var6);
         }

         if (this.showMainResult) {
            if (var2) {
               return this.tableColumn.exportValue(var3, this.getCorrectResultKey(var3), this.direction, this.plType, this.sampleType);
            }

            return this.tableColumn.getValue(var3, this.getCorrectResultKey(var3), this.direction, this.plType, this.sampleType);
         }
      } catch (StatsDontExistException var9) {
      } catch (CrossCheckDataNotExistException var10) {
      } catch (Exception var11) {
         Log.error("Cannot print value of databank column '" + this.tableColumn.getName() + "'. Exc.", var11);
      }

      return "N/A";
   }

   private String tryToReturnWFTextValue(ResultsGroup var1, boolean var2) {
      Object var3 = null;

      try {
         if ((this.resultType.equals("WalkForwardMatrix") || this.resultType.equals("WalkForwardOptimization")) && var1.getBestWFResultKey() != null) {
            if (this.tableColumn.getClassName().equals("Symbol") || this.tableColumn.getClassName().equals("TimeFrame")) {
               if (var2) {
                  return this.tableColumn.exportValue(var1, var1.getMainResultKey(), this.direction, this.plType, this.sampleType);
               }

               return this.tableColumn.getValue(var1, var1.getMainResultKey(), this.direction, this.plType, this.sampleType);
            }

            if (this.tableColumn.getClassName().equals("MiniEquityChart")) {
               if (var2) {
                  return this.tableColumn.exportValue(var1, var1.getBestWFResultKey(), this.direction, this.plType, this.sampleType);
               }

               return this.tableColumn.getValue(var1, var1.getBestWFResultKey(), this.direction, this.plType, this.sampleType);
            }

            if (this.tableColumn.getClassName().equals("Fitness")) {
               if (var2) {
                  return this.tableColumn.exportValue(var1, var1.getBestWFResultKey(), this.direction, this.plType, this.sampleType);
               }

               return this.tableColumn.getValue(var1, var1.getBestWFResultKey(), this.direction, this.plType, this.sampleType);
            }
         }
      } catch (Exception var5) {
         Log.error("Errow while trying to return CrossCheck value. Exc.", var5);
      }

      return (String)var3;
   }

   public double getNumericValue(ResultsGroup var1) throws Exception {
      if (ResultTypes.isCrossCheck(this.resultType)) {
         CrossCheckConditionValue var2 = new CrossCheckConditionValue(this.tableColumn.getClassName(), this.resultType, 0, this.elValue);
         return var2.getValue(var1);
      } else {
         return this.tableColumn.getNumericValue(var1, this.getCorrectResultKey(var1), this.direction, this.plType, this.sampleType);
      }
   }

   private String getCorrectResultKey(ResultsGroup var1) throws Exception {
      return this.resultType.equals("portfolio") ? "Portfolio" : var1.getMainResultKey();
   }

   public Element getXML() {
      Element var1 = new Element("Column");
      var1.setAttribute("class", this.tableColumn.getClass().getSimpleName());
      var1.setAttribute("name", this.tableColumn.getName());
      var1.setAttribute("resultType", String.valueOf(this.resultType));
      var1.setAttribute("sampleType", String.valueOf(this.sampleType));
      var1.setAttribute("direction", String.valueOf(this.direction));
      var1.setAttribute("plType", String.valueOf(this.plType));
      var1.setAttribute("confidenceLevel", String.valueOf(this.confidenceLevel));
      var1.setAttribute("market", String.valueOf(this.market));
      var1.setAttribute("subresult", String.valueOf(this.subresult));
      var1.setAttribute("showMainResult", String.valueOf(this.showMainResult));
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("class");
      if (var2 != null && !var2.equals("")) {
         this.tableColumn = (DatabankColumn)DatabankColumns.get().createNew(var2);
         if (this.tableColumn == null) {
            throw new Exception(L.t("Invalid databank column '%s'.", new Object[]{var2}));
         }

         if (var1.getAttributeValue("resultType") != null) {
            String var3 = var1.getAttributeValue("resultType");
            if (ResultTypes.isValid(var3)) {
               this.resultType = var3;
            }
         }

         if (var1.getAttributeValue("sampleType") != null) {
            this.sampleType = Byte.parseByte(var1.getAttributeValue("sampleType"));
         }

         if (var1.getAttributeValue("direction") != null) {
            this.direction = Byte.parseByte(var1.getAttributeValue("direction"));
         }

         if (var1.getAttributeValue("plType") != null) {
            Byte var4 = Byte.parseByte(var1.getAttributeValue("plType"));
            if (PlTypes.isValid(var4)) {
               this.plType = var4;
            }
         }

         if (var1.getAttributeValue("confidenceLevel") != null) {
            this.confidenceLevel = Integer.parseInt(var1.getAttributeValue("confidenceLevel"));
         }

         if (var1.getAttributeValue("market") != null) {
            this.market = Integer.parseInt(var1.getAttributeValue("market"));
         }

         if (var1.getAttributeValue("subresult") != null) {
            this.subresult = Byte.parseByte(var1.getAttributeValue("subresult"));
         }

         if (var1.getAttributeValue("showMainResult") != null) {
            this.showMainResult = Boolean.parseBoolean(var1.getAttributeValue("showMainResult"));
         }

         this.elValue = var1;
      } else {
         throw new Exception(L.t("Empty column definition.", new Object[0]));
      }
   }
}
