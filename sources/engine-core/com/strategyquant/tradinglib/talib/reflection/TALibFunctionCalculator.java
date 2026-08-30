package com.strategyquant.tradinglib.talib.reflection;

import com.strategyquant.tradinglib.engine.stockpicker.data.BarData;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.meta.annotation.InputParameterType;
import com.tictactec.ta.lib.meta.annotation.OptInputParameterType;
import com.tictactec.ta.lib.meta.annotation.OutputParameterType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class TALibFunctionCalculator {
   private TALibFunction TALibFunction;
   private ObjectArrayList<BarData> bars;

   public TALibFunctionCalculator(TALibFunction var1, ObjectArrayList<BarData> var2) {
      this.TALibFunction = var1;
      this.bars = var2;
   }

   public void calculate(double... var1) {
      ArrayList var2 = new ArrayList();
      int var3 = 0;

      for (TALibFunction.Param var7 : this.TALibFunction.getInputs()) {
         if (var7.getType().equals(InputParameterType.TA_Input_Price.name())) {
            String var8 = var7.getName().substring(7, var7.getName().length());

            for (char var12 : var8.toCharArray()) {
               switch (var12) {
                  case 'C':
                     var2.add(TALibConst.CLOSE);
                     break;
                  case 'H':
                     var2.add(TALibConst.HIGH);
                     break;
                  case 'L':
                     var2.add(TALibConst.LOW);
                     break;
                  case 'O':
                     var2.add(TALibConst.OPEN);
                     break;
                  case 'V':
                     var2.add(TALibConst.VOLUME);
               }
            }
         } else if (var7.getType().equals(InputParameterType.TA_Input_Real.name())) {
            var2.add((int)var1[var3++]);
         }
      }

      double[][] var31 = new double[var2.size()][];
      Object[] var32 = new Object[this.TALibFunction.getOptInputs().length];
      Object[] var33 = new Object[this.TALibFunction.getOutputs().length];
      int var34 = 0;

      for (TALibFunction.Param var41 : this.TALibFunction.getOptInputs()) {
         if (var41.getType().equals(OptInputParameterType.TA_OptInput_IntegerRange.name())
            || var41.getType().equals(OptInputParameterType.TA_OptInput_IntegerList.name())) {
            var32[var34++] = (int)var1[var3++];
         } else if (var41.getType().equals(OptInputParameterType.TA_OptInput_RealRange.name())
            || var41.getType().equals(OptInputParameterType.TA_OptInput_RealList.name())) {
            var32[var34++] = var1[var3++];
         }
      }

      Integer[] var36 = var2.toArray(new Integer[var2.size()]);
      DoubleArray var38 = new DoubleArray(this.bars, var36);
      byte var40 = 0;
      int var42 = var38.get(var36[0]).length - 1;

      for (int var43 = 0; var43 < var31.length; var43++) {
         var31[var43] = var38.get(var36[var43]);
      }

      for (int var44 = 0; var44 < var33.length; var44++) {
         if (this.TALibFunction.getOutputs()[var44].getType().equals(OutputParameterType.TA_Output_Integer.toString())) {
            var33[var44] = new int[var42 + 1];
         } else {
            var33[var44] = new double[var42 + 1];
         }
      }

      MInteger var45 = new MInteger();
      MInteger var13 = new MInteger();
      Object[] var14 = new Object[var31.length + var33.length + var32.length + 4];
      int var15 = 0;
      var14[var15++] = Integer.valueOf(var40);
      var14[var15++] = var42;

      for (double[] var19 : var31) {
         var14[var15++] = var19;
      }

      int var50 = 0;

      for (Object var20 : var32) {
         TALibFunction.Param var21 = this.TALibFunction.getOptInputs()[var50];
         if (!var21.getType().equals(OptInputParameterType.TA_OptInput_IntegerList.toString())
            && !var21.getType().equals(OptInputParameterType.TA_OptInput_IntegerRange.toString())) {
            var14[var15++] = var20;
         } else {
            int var22 = (Integer)var20;
            if (var21.getName().equals("optInMAType")) {
               switch (var22) {
                  case 0:
                     var14[var15++] = MAType.Sma;
                     break;
                  case 1:
                     var14[var15++] = MAType.Ema;
                     break;
                  case 2:
                     var14[var15++] = MAType.Wma;
                     break;
                  case 3:
                     var14[var15++] = MAType.Dema;
                     break;
                  case 4:
                     var14[var15++] = MAType.Tema;
                     break;
                  case 5:
                     var14[var15++] = MAType.Trima;
                     break;
                  case 6:
                     var14[var15++] = MAType.Kama;
                     break;
                  case 7:
                     var14[var15++] = MAType.Mama;
                     break;
                  case 8:
                     var14[var15++] = MAType.T3;
               }
            } else {
               var14[var15++] = var22;
            }
         }

         var50++;
      }

      var14[var15++] = var45;
      var14[var15++] = var13;
      boolean var52 = false;

      for (Object var61 : var33) {
         var14[var15++] = var61;
      }

      try {
         this.TALibFunction.getTalibMethod().invoke(new CoreAnnotated(), var14);
      } catch (IllegalAccessException var28) {
         var28.printStackTrace();
      } catch (IllegalArgumentException var29) {
         var29.printStackTrace();
      } catch (InvocationTargetException var30) {
         var30.printStackTrace();
      }

      int var55 = 0;

      for (Object var63 : var33) {
         for (double var26 : (double[])var63) {
            System.out.println(var26);
         }

         var55++;
      }
   }
}
