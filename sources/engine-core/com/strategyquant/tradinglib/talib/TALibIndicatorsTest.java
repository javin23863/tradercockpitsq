package com.strategyquant.tradinglib.talib;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.engine.stockpicker.data.BarData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;
import java.util.Scanner;

public class TALibIndicatorsTest {
   TALibIndicators indicators = new TALibIndicators();

   public static void main(String[] var0) {
      TALibIndicatorsTest var1 = new TALibIndicatorsTest();
      var1.run();
   }

   private void run() {
      try {
         this.test("CustomIndy_PercentRank_D1_14_Close.csv", "PercentRank", "TimePeriod", 14);
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   private void test(String var1, String var2, Object... var3) throws Exception {
      System.out.println("***************** " + var2 + " **************");
      ObjectArrayList var4 = this.loadData(var1);
      SettingsMap var5 = new SettingsMap();

      for (byte var6 = 0; var6 < var3.length; var6 += 2) {
         var5.set((String)var3[var6], var3[var6 + 1]);
      }

      int var7 = this.compare(var2, var5, var4);
      if (var7 > 0) {
         System.out.println(String.format("Test '%s' FAILED: %d differences found", var1, var7));
      } else {
         System.out.println(String.format("Test '%s' SUCCESS: No differences found", var1));
      }
   }

   private ObjectArrayList<BarData> loadData(String var1) throws Exception {
      File var5 = new File("tests\\stockpicker\\Indicators\\" + var1);
      String var6 = SQUtils.fileToString(var5);
      ObjectArrayList var7 = new ObjectArrayList();
      int var8 = 0;

      try {
         Scanner var9 = new Scanner(var6);

         while (var9.hasNextLine()) {
            var8++;
            String var2 = var9.nextLine();
            String[] var3 = var2.split("\t", -1);
            BarData var4 = new BarData();
            var4.time = SQTime.parseToMilis(var3[0], "MM/dd/yyyy");
            var4.open = Float.valueOf(var3[1]);
            var4.high = Float.valueOf(var3[2]);
            var4.low = Float.valueOf(var3[3]);
            var4.close = Float.valueOf(var3[4]);
            var4.volume = Float.valueOf(var3[5]);
            var4.value = Float.valueOf(var3[6]).floatValue();
            var7.add(var4);
         }
      } catch (Exception var11) {
         throw new Exception(String.format("Cannot load file. Error on line %d", var8), var11);
      }

      int var13 = var7.size();
      this.indicators.o = new float[var13];
      this.indicators.h = new float[var13];
      this.indicators.l = new float[var13];
      this.indicators.c = new float[var13];

      for (int var10 = 0; var10 < var13; var10++) {
         BarData var12 = (BarData)var7.get(var10);
         this.indicators.o[var10] = var12.open;
         this.indicators.h[var10] = var12.high;
         this.indicators.l[var10] = var12.low;
         this.indicators.c[var10] = var12.close;
      }

      return var7;
   }

   private int compare(String var1, SettingsMap var2, ObjectArrayList<BarData> var3) throws Exception {
      int var4 = 0;
      float[][] var5 = this.indicators.calculate(var1, var2, null, var2.getInt("Shift"), 0, "TEST");
      if (var5 == null) {
         throw new Exception("outputs is null");
      }

      for (int var6 = 150; var6 < var5[0].length; var6++) {
         double var7 = ((BarData)var3.get(var6)).value;
         double var9 = var5[0][var6];
         if (SQUtils.round(var7, 4) != SQUtils.round(var9, 4) && Math.abs(var7 - var9) > 2.0E-4) {
            System.out.println(String.format("%s values don't match - (AMB) %f <> (SQX) %f", SQTime.toDateString(((BarData)var3.get(var6)).time), var7, var9));
            var4++;
         }
      }

      return var4;
   }
}
