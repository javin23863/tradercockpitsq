package com.strategyquant.indicatorTester;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorCalibrator {
   public static final Logger Log = LoggerFactory.getLogger("IndicatorCalibrator");

   public ArrayList<CalibrationInfo> calibrate(String[] var1, String[] var2, int var3) throws Exception {
      if (var3 != 1316847364 && var3 != -1816889229) {
         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();

         for (int var6 = 0; var6 < var1.length; var6++) {
            var5.add(this.loadChartData(var1[var6], var2[var6]));
         }

         IndicatorsObj var25 = StrategyBase.createIndicatorsObj(var3);
         ArrayList var7 = IndicatorsLoader.getIndicators();
         ArrayList var8 = new ArrayList();

         for (int var9 = 0; var9 < var7.size(); var9++) {
            IndicatorInfo var10 = (IndicatorInfo)var7.get(var9);
            if (!var10.ignored && this.checkReturnType(var10.returnType)) {
               String var11 = IndicatorsLoader.correctIndyName(var10.simpleName);
               if (!var11.equals("DataLoggingIndy")) {
                  Log.debug("Calibrating indicator " + var11 + "...");

                  try {
                     CalibrationInfo var12 = new CalibrationInfo(var11);
                     var12.name = var10.title;

                     for (int var13 = 0; var13 < var5.size(); var13++) {
                        ChartData var14 = (ChartData)var5.get(var13);
                        ArrayList var15 = this.getParametersVariations(var10, var14);
                        CalibrationRangesInfo var16 = new CalibrationRangesInfo(var14.Symbol, var14.Timeframe);
                        var16.minValue = 1.0E9;
                        var16.maxValue = -1.0E9;
                        Method var17 = IndicatorTester.getMethod(var25, var11, (Object[])var15.get(0));
                        var8.clear();

                        for (int var18 = 0; var18 < var15.size(); var18++) {
                           Object var19 = var17.invoke(var25, (Object[])var15.get(var18));

                           for (int var20 = 0; var20 < var10.outputBuffers.size(); var20++) {
                              String var21 = var10.outputBuffers.get(var20);
                              DataSeries var22 = (DataSeries)var19.getClass().getField(var21).get(var19);
                              var22.setShift(0);

                              for (int var23 = 0; var23 < var22.size(); var23++) {
                                 var8.add(var22.get(var23));
                              }
                           }
                        }

                        double var26 = this.getMean(var8);
                        double var27 = this.getStdDev(var8, var26);
                        var16.minValue = var26 - var27;
                        var16.maxValue = var26 + var27;
                        var16.fixValues();
                        var12.ranges.add(var16);
                     }

                     var4.add(var12);
                  } catch (Exception var24) {
                     Log.error("Cannot calibrate indicator '" + var11 + "'", var24);
                  }
               }
            }
         }

         return var4;
      } else {
         throw new Exception(L.t("Indicator calibration is not supported for AlgoCloud Stockpicker / Single-asset engine.", new Object[0]));
      }
   }

   private double getMean(ArrayList<Double> var1) throws TradingException {
      double var2 = 0.0;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         var2 += var1.get(var4);
      }

      return var2 / var1.size();
   }

   private double getStdDev(ArrayList<Double> var1, double var2) throws TradingException {
      double var4 = 0.0;

      for (int var6 = 0; var6 < var1.size(); var6++) {
         var4 += Math.pow((Double)var1.get(var6) - var2, 2.0);
      }

      return Math.sqrt(var4 / var1.size());
   }

   private ChartData loadChartData(String var1, String var2) throws Exception {
      if (!DataManager.checkDataExists("History", var1)) {
         throw new Exception(L.t("Symbol '%s' doesn't exist", new Object[]{var1}));
      }

      DataInfo var3 = DataManager.getDataInfo("History", var1);
      ChartDef var4 = new ChartDef("History", var1, var2, var3.dateFrom, var3.dateTo, var3.symbolInfo.defaultSpread, "No Session");
      IDataLoader var5 = null;

      try {
         var5 = DataManager.getDataLoader(var4, 1);
         var5.open();
         TimeDataSeries var6 = new TimeDataSeries();
         DataSeries var7 = new DataSeries();
         DataSeries var8 = new DataSeries();
         DataSeries var9 = new DataSeries();
         DataSeries var10 = new DataSeries();
         DataSeries var11 = new DataSeries();
         VersatileData var12 = new VersatileData();

         while (var5.hasNextTick()) {
            var5.getNextTick(var12);
            var6.add(var12.time);
            var7.add(var12.open);
            var8.add(var12.high);
            var9.add(var12.low);
            var10.add(var12.close);
            var11.add(var12.volume);
         }

         var5.close();
         ChartData var13 = new ChartData(
            var2, var6, var7, var8, var9, var10, var11, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
         );
         var13.Symbol = var1;
         return var13;
      } catch (Exception var18) {
         throw new Exception(L.t("Cannot load chart data of symbol '%s'", new Object[]{var1}), var18);
      } finally {
         if (var5 != null) {
            var5.close();
         }
      }
   }

   private ArrayList<Object[]> getParametersVariations(IndicatorInfo var1, ChartData var2) {
      ArrayList var3 = new ArrayList();
      Object[] var4 = this.getDefaultParameters(var1, var2);

      for (int var5 = 0; var5 < var1.parameters.size(); var5++) {
         IndyParameter var6 = var1.parameters.get(var5);
         if (!var6.className.equals(Integer.class.getSimpleName()) && !var6.className.equals(int.class.getSimpleName())) {
            if ((var6.className.equals(Double.class.getSimpleName()) || var6.className.equals(double.class.getSimpleName())) && var6.calibrationValues != null) {
               String[] var12 = var6.calibrationValues.split(",");

               for (int var13 = 0; var13 < var12.length; var13++) {
                  double var14 = Double.parseDouble(var12[var13]);
                  if (var3.size() > var13) {
                     Object[] var11 = (Object[])var3.get(var13);
                     var11[var5] = var14;
                  } else {
                     Object[] var16 = this.cloneDefaultParameters(var4);
                     var16[var5] = var14;
                     var3.add(var16);
                  }
               }
            }
         } else if (var6.calibrationValues != null) {
            String[] var7 = var6.calibrationValues.split(",");

            for (int var8 = 0; var8 < var7.length; var8++) {
               int var9 = Integer.parseInt(var7[var8]);
               if (var3.size() > var8) {
                  Object[] var10 = (Object[])var3.get(var8);
                  var10[var5] = var9;
               } else {
                  Object[] var15 = this.cloneDefaultParameters(var4);
                  var15[var5] = var9;
                  var3.add(var15);
               }
            }
         }
      }

      if (var3.isEmpty()) {
         var3.add(var4);
      }

      return var3;
   }

   private Object[] getDefaultParameters(IndicatorInfo var1, ChartData var2) {
      Object[] var3 = new Object[var1.parameters.size()];

      for (int var4 = 0; var4 < var1.parameters.size(); var4++) {
         IndyParameter var5 = var1.parameters.get(var4);
         if (var5.className.equals(ChartData.class.getSimpleName())) {
            var3[var4] = var2;
         } else if (var5.className.equals(DataSeries.class.getSimpleName())) {
            var3[var4] = var2.Open;
         } else if (var5.className.equals(Integer.class.getSimpleName()) || var5.className.equals(int.class.getSimpleName())) {
            var3[var4] = Integer.parseInt(var5.defaultValue);
         } else if (!var5.className.equals(Double.class.getSimpleName()) && !var5.className.equals(double.class.getSimpleName())) {
            var3[var4] = var5.defaultValue;
         } else {
            var3[var4] = Double.parseDouble(var5.defaultValue);
         }
      }

      return var3;
   }

   private Object[] cloneDefaultParameters(Object[] var1) {
      Object[] var2 = new Object[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3];
      }

      return var2;
   }

   private boolean checkReturnType(int var1) {
      switch (var1) {
         case 0:
         case 1:
         case 7:
            return true;
         default:
            return false;
      }
   }
}
