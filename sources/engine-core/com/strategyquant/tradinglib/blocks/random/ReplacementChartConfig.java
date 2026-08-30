package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.tradinglib.generator.GenerateException;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementChartConfig {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementChartConfig");
   public int minConditions;
   public int maxConditions;
   public int minExitConditions;
   public int maxExitConditions;
   public int minExitTypes;
   public int maxExitTypes;
   public int minShift;
   public int maxShift;
   public int minPeriod;
   public int maxPeriod;
   public int chartIndex;
   private int conditionsToGenerate;
   private int exitConditionsToGenerate;

   public ReplacementChartConfig(int var1, Element var2, double var3) throws GenerateException {
      this.chartIndex = this.loadIntAttribute(var2, "chartIndex", var1);
      this.minConditions = this.loadIntAttribute(var2, "minConditions", 0);
      this.maxConditions = this.loadIntAttribute(var2, "maxConditions", 0);
      if (this.minConditions != 0 && this.maxConditions != 0 && this.minConditions > this.maxConditions) {
         this.maxConditions = this.minConditions;
      }

      this.minExitConditions = this.loadIntAttribute(var2, "minExitConditions", 0);
      this.maxExitConditions = this.loadIntAttribute(var2, "maxExitConditions", 0);
      if (this.minExitConditions != 0 && this.maxExitConditions != 0 && this.minExitConditions > this.maxExitConditions) {
         this.maxExitConditions = this.minExitConditions;
      }

      if (this.minExitConditions == 0 && var3 == 1.0 && this.minExitConditions > 0 && this.maxExitConditions == 0) {
         this.maxExitConditions = 1;
      }

      this.minShift = this.loadIntAttribute(var2, "minShift", -1);
      this.maxShift = this.loadIntAttribute(var2, "maxShift", -1);
      if (this.minShift >= 0 && this.maxShift >= 0 && this.minShift > this.maxShift) {
         this.minShift = this.maxShift;
      }

      this.minPeriod = this.loadIntAttribute(var2, "minPeriod", -1);
      this.maxPeriod = this.loadIntAttribute(var2, "maxPeriod", -1);
      if (this.minPeriod >= 0 && this.maxPeriod >= 0 && this.minPeriod > this.maxPeriod) {
         this.maxPeriod = this.minPeriod;
      }

      this.minExitTypes = this.loadIntAttribute(var2, "minExitTypes", 1);
      this.maxExitTypes = this.loadIntAttribute(var2, "maxExitTypes", 5);
      if (this.minExitTypes != 0 && this.maxExitTypes != 0 && this.minExitTypes > this.maxExitTypes) {
         this.maxExitTypes = this.minExitTypes;
      }
   }

   private int loadIntAttribute(Element var1, String var2, int var3) throws GenerateException {
      String var4 = var1.getAttributeValue(var2);
      if (var3 < 0 && var4 == null) {
         throw new GenerateException("Attribute '" + var2 + "' must be set!");
      } else {
         return var4 != null ? Integer.parseInt(var4) : var3;
      }
   }

   public void setConditionsToGenerate(int var1, int var2) {
      if (var1 == 2) {
         this.exitConditionsToGenerate = var2;
         if (this.exitConditionsToGenerate < this.minExitConditions) {
            this.exitConditionsToGenerate = this.minExitConditions;
         }

         if (this.exitConditionsToGenerate > this.maxExitConditions) {
            this.exitConditionsToGenerate = this.maxExitConditions;
         }
      } else {
         this.conditionsToGenerate = var2;
         if (this.conditionsToGenerate < this.minConditions) {
            this.conditionsToGenerate = this.minConditions;
         }

         if (this.conditionsToGenerate > this.maxConditions) {
            this.conditionsToGenerate = this.maxConditions;
         }
      }
   }

   public int getConditionsToGenerate(int var1) {
      return var1 == 2 ? this.exitConditionsToGenerate : this.conditionsToGenerate;
   }

   public void decreaseConditionsToGenerate(int var1) {
      if (var1 == 2) {
         this.exitConditionsToGenerate--;
      } else {
         this.conditionsToGenerate--;
      }
   }
}
