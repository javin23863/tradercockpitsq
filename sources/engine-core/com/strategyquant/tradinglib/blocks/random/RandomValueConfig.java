package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import java.math.BigDecimal;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomValueConfig {
   public static final Logger Log = LoggerFactory.getLogger("RandomValueConfig");
   private boolean valid = true;
   private double val1;
   private double val2;
   private double step;
   private boolean isDouble;

   public RandomValueConfig(boolean var1) {
      this.valid = var1;
   }

   public RandomValueConfig(double var1, double var3, double var5, boolean var7) {
      this.val1 = var1;
      this.val2 = var3;
      this.step = var5;
      this.isDouble = var7;
   }

   public double generateRandomValue(IRandomGenerator var1, boolean var2) {
      return var2 ? this.generateFromDoubleRange(var1) : this.generateFromIntRange(var1);
   }

   private double generateFromIntRange(IRandomGenerator var1) {
      if (this.val1 == this.val2) {
         return (int)this.val1;
      }

      if ((this.val1 >= 0.0 || this.val2 >= 0.0) && this.val1 > this.val2) {
         return 2.147483647E9;
      }

      int var2 = (int)this.val2 - (int)this.val1;
      int var3;
      if (this.step == 0.0) {
         var3 = var2 / 100;
         if (var3 == 0) {
            var3 = 1;
         }
      } else {
         var3 = (int)this.step;
      }

      int var4 = var2 / var3;
      int var5 = var1.nextInt(var4 + 1);
      int var6 = (int)this.val1 + var5 * var3;
      return var6;
   }

   private double generateFromDoubleRange(IRandomGenerator var1) {
      if (this.val1 == this.val2) {
         return this.val1;
      }

      if ((this.val1 >= 0.0 || this.val2 >= 0.0) && this.val1 > this.val2) {
         return Double.MAX_VALUE;
      }

      double var2 = this.val2 - this.val1;
      double var4;
      if (this.step == 0.0) {
         var4 = SQUtils.round2(var2 / 100.0);
         if (var4 == 0.0) {
            var4 = SQUtils.round2(var2 / 10.0);
         }

         if (var4 == 0.0) {
            var4 = SQUtils.round2(var2 / 2.0);
         }

         if (var4 == 0.0) {
            return this.val1;
         }
      } else {
         var4 = this.step;
      }

      int var6 = (int)(var2 / var4);
      int var7 = var1.nextInt(var6 + 1);
      return this.val1 + var7 * var4;
   }

   public boolean isValid() {
      return this.valid;
   }

   public static RandomValueConfig parseRandomValue(String var0, boolean var1) {
      return !var0.contains(":") ? parseOldFormat(var0, var1) : parseNewFormat(var0, var1);
   }

   private static RandomValueConfig parseNewFormat(String var0, boolean var1) {
      if (var0.contains(":")) {
         String[] var11 = var0.split(":");
         if (var11 == null) {
            return null;
         }

         try {
            if (var11.length == 2) {
               double var12 = Double.parseDouble(var11[0]);
               double var13 = Double.parseDouble(var11[1]);
               return new RandomValueConfig(var12, var13, 1.0, var1);
            } else if (var11.length == 3) {
               double var3 = Double.parseDouble(var11[0]);
               double var5 = Double.parseDouble(var11[1]);
               double var7 = Double.parseDouble(var11[2]);
               return new RandomValueConfig(var3, var5, var7, var1);
            } else {
               return null;
            }
         } catch (NumberFormatException var9) {
            return null;
         }
      } else {
         try {
            double var2 = Double.parseDouble(var0);
            return new RandomValueConfig(var2, var2, 0.0, var1);
         } catch (NumberFormatException var10) {
            return null;
         }
      }
   }

   private static RandomValueConfig parseOldFormat(String var0, boolean var1) {
      if (!var0.contains("-")) {
         try {
            double var13 = Double.parseDouble(var0);
            return new RandomValueConfig(var13, var13, 0.0, var1);
         } catch (NumberFormatException var12) {
            return null;
         }
      } else {
         String var2 = null;
         String var3;
         if (var0.contains(";")) {
            String[] var4 = var0.split(";");
            if (var4 == null || var4.length != 2) {
               return null;
            }

            var3 = var4[0];
            var2 = var4[1];
         } else {
            var3 = var0;
         }

         String[] var14 = var3.split("-");
         if (var14 != null && var14.length == 2) {
            try {
               double var5 = Double.parseDouble(var14[0]);
               double var7 = Double.parseDouble(var14[1]);
               double var9 = var2 == null ? 0.0 : Double.parseDouble(var2);
               return new RandomValueConfig(var5, var7, var9, var1);
            } catch (NumberFormatException var11) {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public int getDecimals(int var1) {
      BigDecimal var2 = BigDecimal.valueOf(this.val1);
      BigDecimal var3 = BigDecimal.valueOf(this.val2);
      BigDecimal var4 = BigDecimal.valueOf(this.step);
      int var5 = var2.scale();
      int var6 = var3.scale();
      int var7 = var4.scale();
      int var8 = var1;
      if (var5 > var8) {
         var8 = var5;
      }

      if (var6 > var8) {
         var8 = var6;
      }

      if (var7 > var8) {
         var8 = var7;
      }

      if (var8 > 6) {
         var8 = 6;
      }

      return var8;
   }

   public boolean isInValidRange(double var1) {
      return !(var1 < this.val1) && !(var1 > this.val2);
   }

   public void setMinMaxStepAttributes(Element var1) {
      if (this.isDouble) {
         var1.setAttribute("minValue", Double.toString(this.val1));
         var1.setAttribute("maxValue", Double.toString(this.val2));
         var1.setAttribute("step", Double.toString(this.step));
      } else {
         var1.setAttribute("minValue", Integer.toString((int)this.val1));
         var1.setAttribute("maxValue", Integer.toString((int)this.val2));
         var1.setAttribute("step", Integer.toString((int)this.step));
      }
   }
}
