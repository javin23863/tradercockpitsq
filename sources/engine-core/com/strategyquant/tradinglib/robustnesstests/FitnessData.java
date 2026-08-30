package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Variable;
import org.json.JSONArray;
import org.json.JSONObject;

public class FitnessData {
   public static final double NOT_STABLE = -Double.MAX_VALUE;
   public Variable variable;
   public double originalValue;
   public double[] values;
   public double[] fitness;
   public double bestAreaStartValue;
   public double bestAreaEndValue;
   public double bestValue;
   public boolean stableAreaFound;

   protected FitnessData() {
   }

   public FitnessData(Variable var1, int var2) {
      this.variable = var1;
      this.originalValue = var1.getValueAsDouble();
      this.values = new double[var2];
      this.fitness = new double[var2];
   }

   public void set(int var1, double var2, double var4) throws Exception {
      if (var1 >= 0 && var1 < this.values.length) {
         this.values[var1] = var2;
         this.fitness[var1] = var4;
      } else {
         throw new Exception(L.t("Index %d out of range (%d)", new Object[]{var1, this.values.length}));
      }
   }

   public double analyze(int var1, int var2) {
      this.bestValue = -Double.MAX_VALUE;
      double var3 = -Double.MAX_VALUE;
      boolean var5 = var1 % 2 == 0;
      int var6 = var1 - 1;
      var6 = Math.min(var6, this.fitness.length - 1);

      for (int var7 = var6; var7 < this.fitness.length; var7++) {
         double var8 = -Double.MAX_VALUE;
         int var10 = var7 - var6;
         var10 = Math.max(var10, 0);

         for (int var11 = var10; var11 <= var7; var11++) {
            double var12 = this.fitness[var11];
            if (var12 > var8) {
               var8 = var12 / 100.0 * (100 - var2);
            }
         }

         boolean var17 = true;

         for (int var18 = var10; var18 <= var7; var18++) {
            if (this.fitness[var18] < var8) {
               var17 = false;
               break;
            }
         }

         if (var17) {
            int var19 = var7 - var1 / 2;
            if (var19 < 0) {
               var19 = 0;
            }

            double var13 = this.fitness[var19];
            if (var5 && this.fitness[var19 + 1] > this.fitness[var19]) {
               var13 = this.fitness[++var19];
            }

            if (var13 > var3) {
               var3 = var13;
               this.bestValue = this.values[var19];
               this.bestAreaStartValue = this.values[var10];
               this.bestAreaEndValue = this.values[var7];
               this.stableAreaFound = true;
            }
         }
      }

      return this.bestValue;
   }

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("parameterName", this.variable.getName());
      var1.put("originalValue", this.originalValue);
      var1.put("optimizedValue", this.stableAreaFound ? this.bestValue : this.originalValue);
      var1.put("stableAreaFound", this.stableAreaFound);
      var1.put("stableAreaStartValue", this.bestAreaStartValue);
      var1.put("stableAreaEndValue", this.bestAreaEndValue);
      JSONArray var2 = new JSONArray();
      JSONArray var3 = new JSONArray();

      for (int var4 = 0; var4 < this.values.length; var4++) {
         var2.put(this.values[var4]);
         var3.put(this.fitness[var4]);
      }

      var1.put("values", var2);
      var1.put("fitness", var3);
      return var1;
   }

   public FitnessData getClone() {
      FitnessData var1 = new FitnessData();
      var1.variable = this.variable != null ? this.variable.getClone() : null;
      var1.originalValue = this.originalValue;
      var1.values = new double[this.values.length];
      System.arraycopy(this.values, 0, var1.values, 0, this.values.length);
      var1.fitness = new double[this.fitness.length];
      System.arraycopy(this.fitness, 0, var1.fitness, 0, this.fitness.length);
      var1.bestAreaStartValue = this.bestAreaStartValue;
      var1.bestAreaEndValue = this.bestAreaEndValue;
      var1.bestValue = this.bestValue;
      var1.stableAreaFound = this.stableAreaFound;
      return var1;
   }
}
