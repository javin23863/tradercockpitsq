package com.strategyquant.indicatorTester;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ChartData;

public class IndicatorParameters {
   private IndicatorTester tester;
   private int dataSource;

   public IndicatorParameters(IndicatorTester var1) {
      this.tester = var1;
   }

   public Object[] convertParams(String var1, String[] var2) throws Exception {
      IndicatorInfo var3 = IndicatorsLoader.getIndyByName(var1);
      if (var3 == null) {
         throw new Exception("Indicator '" + var1 + "' doesn't exist.");
      }

      if (var2 == null) {
         this.dataSource = 0;
         return null;
      }

      Object[] var4 = new Object[this.containsDataSeries(var3) ? var2.length - 1 : var2.length];
      int var5 = 0;
      int var6 = 0;

      for (int var7 = 0; var7 < var3.parameters.size(); var7++) {
         IndyParameter var8 = var3.parameters.get(var7);

         try {
            if (var8.className.equals(ChartData.class.getSimpleName())) {
               this.dataSource = 0;
            } else {
               if (var8.className.equals(DataSeries.class.getSimpleName())) {
                  this.dataSource = this.getDataSource(var2[var5]);
               } else {
                  var4[var6] = this.convertParam(var2[var5], var8.className);
                  var6++;
               }

               var5++;
            }
         } catch (Exception var11) {
            String var10 = "Error while converting parameter '" + var8.name + "': \n" + SQUtils.stackTraceToString(var11);
            this.tester.getDataErrors().add(var10);
            throw var11;
         }
      }

      return var4;
   }

   public int getDataSource() {
      return this.dataSource;
   }

   private Object convertParam(String var1, String var2) {
      if (var2.equals(Integer.class.getSimpleName()) || var2.equals(int.class.getSimpleName())) {
         return Integer.parseInt(var1);
      } else if (var2.equals(Double.class.getSimpleName()) || var2.equals(double.class.getSimpleName())) {
         return Double.parseDouble(var1);
      } else {
         return !var2.equals(Boolean.class.getSimpleName()) && !var2.equals(boolean.class.getSimpleName()) ? var1 : Boolean.parseBoolean(var1);
      }
   }

   private boolean containsDataSeries(IndicatorInfo var1) {
      for (IndyParameter var3 : var1.parameters) {
         if (var3.className.equals(DataSeries.class.getSimpleName())) {
            return true;
         }
      }

      return false;
   }

   private int getDataSource(String var1) throws Exception {
      switch (var1) {
         case "Open":
            return 1;
         case "High":
            return 2;
         case "Low":
            return 3;
         case "Close":
            return 4;
         case "Volume":
            return 5;
         case "Typical":
            return 6;
         case "Median":
            return 7;
         case "Weighted":
            return 8;
         default:
            throw new Exception("Unknown input price '" + var1 + "'");
      }
   }
}
