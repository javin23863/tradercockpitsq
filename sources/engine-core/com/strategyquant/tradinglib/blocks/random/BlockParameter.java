package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.tradinglib.blocks.annotations.Value;
import java.io.Serializable;
import java.math.BigDecimal;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockParameter implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("BlockParameter");
   public String key;
   public String defaultValue;
   public String type;
   public String controlType;
   private Element elParam;
   public double minValue;
   public double maxValue;
   public int decimals;
   public double step;
   public String values;
   public int iDefaultValue;
   public double dDefaultValue;
   public boolean bDefaultValue;
   boolean couldBeVariable = false;
   public String valueType = null;
   public String exitMethod = null;
   public String exitMethodType = null;
   public boolean customRangesSet;
   public RandomValueConfig rvConfig = null;
   public String paramType = null;

   public BlockParameter(Element var1) {
      this.elParam = var1;
      this.key = var1.getAttributeValue("key");
      this.type = var1.getAttributeValue("type");
      this.controlType = var1.getAttributeValue("controlType");
      this.valueType = var1.getAttributeValue("valueType");
      this.paramType = var1.getAttributeValue("paramType");
      this.initializeRanges(false, null);
      this.recognizeDecimals();
      this.exitMethod = var1.getAttributeValue("exitMethod");
      this.exitMethodType = var1.getAttributeValue("exitMethodType");
      this.values = this.getStringAttribute("values", null);
      this.setDefaultValue(var1.getAttributeValue("defaultValue"));
   }

   public void initializeRanges(boolean var1, String var2) {
      this.minValue = this.getDoubleAttribute("minValue", -1.0);
      this.maxValue = this.getDoubleAttribute("maxValue", -1.0);
      this.step = this.getDoubleAttribute("step", -1.0);
      if (var2 == null || !var2.contains("SQ.Formulas.SLPT")) {
         if (!var1 || Value.isPredefined(this.minValue)) {
            double var3 = this.getDoubleAttribute("builderMinValue", -9999999.0);
            if (var3 != -9999999.0 && var3 > this.minValue) {
               this.minValue = var3;
            }

            double var5 = this.getDoubleAttribute("genMinValue", -9999999.0);
            if (var5 != -9999999.0 && (var5 > this.minValue || Value.isPredefined(var5))) {
               this.minValue = var5;
            }
         }

         if (!var1 || Value.isPredefined(this.maxValue)) {
            double var7 = this.getDoubleAttribute("builderMaxValue", -9999999.0);
            if (var7 != -9999999.0 && var7 < this.maxValue) {
               this.maxValue = var7;
            }

            double var10 = this.getDoubleAttribute("genMaxValue", -9999999.0);
            if (var10 != -9999999.0 && (var10 < this.maxValue || Value.isPredefined(var10))) {
               this.maxValue = var10;
            }
         }

         if (!var1 || Value.isPredefined(this.minValue) && Value.isPredefined(this.maxValue)) {
            double var8 = this.getDoubleAttribute("builderStep", -9999999.0);
            this.step = var8;
            double var11 = this.getDoubleAttribute("genStep", -9999999.0);
            if (var11 != -9999999.0) {
               this.step = var11;
            }
         }
      }

      String var9 = this.elParam.getAttributeValue("randomValue");
      if (var9 != null && !var9.equals("") && !var9.equals("default")) {
         this.rvConfig = RandomValueConfig.parseRandomValue(var9, false);
      }

      this.customRangesSet = false;
      if (this.key.startsWith("#Period") && (this.minValue != -1000003.0 || this.maxValue != -1000004.0)) {
         this.customRangesSet = true;
      }
   }

   public void recognizeDecimals() {
      if (this.minValue == 0.0) {
         this.decimals = 2;
      }

      double var1 = Math.min(this.minValue, this.step);
      BigDecimal var3 = BigDecimal.valueOf(var1);
      this.decimals = var3.scale();
      if (this.decimals > 6) {
         this.decimals = 6;
      }
   }

   private double getDoubleAttribute(String var1, double var2) {
      String var4 = this.elParam.getAttributeValue(var1);
      if (var4 != null && !var4.isBlank()) {
         try {
            return Double.parseDouble(var4);
         } catch (NumberFormatException var6) {
            throw var6;
         }
      } else {
         return var2;
      }
   }

   private String getStringAttribute(String var1, String var2) {
      String var3 = this.elParam.getAttributeValue(var1);
      return var3 == null ? var2 : var3;
   }

   public boolean isValueParameter() {
      return this.type.equals("value");
   }

   public BlockParameter clone() {
      return new BlockParameter(this.elParam.clone());
   }

   public void setDefaultValue(String var1) {
      this.defaultValue = var1;
      if (this.defaultValue != null) {
         try {
            switch (this.type) {
               case "int":
                  this.iDefaultValue = Integer.parseInt(this.defaultValue);
                  break;
               case "double":
                  this.dDefaultValue = Double.parseDouble(this.defaultValue);
                  break;
               case "boolean":
                  this.bDefaultValue = Boolean.parseBoolean(this.defaultValue);
            }
         } catch (NumberFormatException var4) {
            this.couldBeVariable = true;
         }
      }
   }

   public void setParamAttribute(String var1, String var2) {
      this.elParam.setAttribute(var1, var2);
      switch (var1) {
         case "type":
            this.type = this.elParam.getAttributeValue("type");
            break;
         case "controlType":
            this.controlType = this.elParam.getAttributeValue("controlType");
            break;
         case "minValue":
            this.minValue = this.getDoubleAttribute("minValue", -1.0);
            break;
         case "maxValue":
            this.maxValue = this.getDoubleAttribute("maxValue", -1.0);
            break;
         case "step":
            this.step = this.getDoubleAttribute("step", -1.0);
            break;
         case "builderMinValue":
            double var5 = this.getDoubleAttribute("builderMinValue", -9999999.0);
            if (var5 != -9999999.0 && var5 > this.minValue && this.notPreset(this.minValue)) {
               this.minValue = var5;
            }
            break;
         case "builderMaxValue":
            double var7 = this.getDoubleAttribute("builderMaxValue", -9999999.0);
            if (var7 != -9999999.0 && var7 < this.maxValue && this.notPreset(this.minValue)) {
               this.maxValue = var7;
            }
            break;
         case "builderStep":
            double var9 = this.getDoubleAttribute("builderStep", -9999999.0);
            if (var9 != -9999999.0) {
               this.step = var9;
            }
            break;
         case "values":
            this.values = this.getStringAttribute("values", null);
            break;
         case "defaultValue":
            this.setDefaultValue(this.elParam.getAttributeValue("defaultValue"));
      }
   }

   private boolean notPreset(double var1) {
      return var1 <= -1000001.0 && var1 >= 1000056.0;
   }

   public String getParamAttribute(String var1) {
      return this.elParam.getAttributeValue(var1);
   }

   public Element getParamElement() {
      return this.elParam.clone();
   }
}
