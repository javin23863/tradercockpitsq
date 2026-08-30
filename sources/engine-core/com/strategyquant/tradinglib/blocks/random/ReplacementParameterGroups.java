package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementParameterGroups extends ArrayList<ReplacementParameters> {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementParameter");
   private int cummulativeWeight = 0;
   private int minShift = Integer.MIN_VALUE;
   private int maxShift = Integer.MIN_VALUE;
   private int minPeriod = Integer.MIN_VALUE;
   private int maxPeriod = Integer.MIN_VALUE;

   public boolean add(ReplacementParameters var1) {
      this.cummulativeWeight = this.cummulativeWeight + var1.weight;
      if (var1.generationType == 1) {
         this.recognizeMinMaxShiftPeriod(var1);
      }

      return super.add(var1);
   }

   private void recognizeMinMaxShiftPeriod(ReplacementParameters var1) {
      for (ReplacementParameter var3 : var1.parameters) {
         if (var3.blockParam.key.contains("#Period") && var3.generation == 2) {
            this.minPeriod = (int)var3.blockParam.minValue;
            this.maxPeriod = (int)var3.blockParam.maxValue;
         } else if (var3.blockParam.key.contains("#Shift") && var3.generation == 2) {
            this.minShift = (int)var3.blockParam.minValue;
            this.maxShift = (int)var3.blockParam.maxValue;
         }
      }
   }

   public ReplacementParameters chooseRandomly(IRandomGenerator var1, ReplacementChartConfig var2, ItemRandomValueRanges var3) throws GenerateException {
      if (this.size() == 1) {
         return this.get(0);
      }

      int var4 = 100;

      while (var4 > 0) {
         var4--;
         int var5 = var1.nextInt(this.cummulativeWeight);
         int var6 = 0;

         for (ReplacementParameters var8 : this) {
            var6 += var8.weight;
            if (var5 < var6 && (var4 <= 1 || this.checkParamPeriodShiftRangesMatch(var8, var2) && this.checkRandomValueAndGeneration(var8, var3))) {
               return var8;
            }
         }
      }

      throw new GenerateException("No replacement parameters group found, cummWeight: " + this.cummulativeWeight);
   }

   private boolean checkRandomValueAndGeneration(ReplacementParameters var1, ItemRandomValueRanges var2) {
      if (var2 != null && var1.parameters != null) {
         for (ReplacementParameter var4 : var1.parameters) {
            String var5 = var2.getForParam(var4.blockParam.key);
            if (var5 != null && var4.generation == 1) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private boolean checkParamPeriodShiftRangesMatch(ReplacementParameters var1, ReplacementChartConfig var2) {
      if (this.minPeriod == Integer.MIN_VALUE
         && this.maxPeriod == Integer.MIN_VALUE
         && this.minShift == Integer.MIN_VALUE
         && this.maxShift == Integer.MIN_VALUE) {
         return true;
      }

      for (ReplacementParameter var4 : var1.parameters) {
         if (var4.blockParam.key.contains("#Period") && var4.generation == 1) {
            if (this.minPeriod != Integer.MIN_VALUE) {
               int var7 = this.minPeriod;
               if (this.minPeriod == -1000003) {
                  var7 = var2.minPeriod;
               }

               if (var4.blockParam.iDefaultValue < var7) {
                  return false;
               }
            }

            if (this.maxPeriod != Integer.MIN_VALUE) {
               int var8 = this.maxPeriod;
               if (this.maxPeriod == -1000004) {
                  var8 = var2.maxPeriod;
               }

               if (var4.blockParam.iDefaultValue > var8) {
                  return false;
               }
            }
         } else if (var4.blockParam.key.contains("#Shift") && var4.generation == 2) {
            if (this.minShift != Integer.MIN_VALUE) {
               int var5 = this.minShift;
               if (this.minShift == -1000001) {
                  var5 = var2.minShift;
               }

               if (var4.blockParam.iDefaultValue < var5) {
                  return false;
               }
            }

            if (this.maxShift != Integer.MIN_VALUE) {
               int var6 = this.maxShift;
               if (this.maxShift == -1000002) {
                  var6 = var2.maxShift;
               }

               if (var4.blockParam.iDefaultValue > var6) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public ReplacementParameter getChartParam() {
      ReplacementParameters var1 = this.get(0);
      return var1 != null ? var1.getChartParam() : null;
   }
}
