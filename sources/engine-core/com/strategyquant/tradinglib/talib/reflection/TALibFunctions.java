package com.strategyquant.tradinglib.talib.reflection;

import com.strategyquant.lib.L;
import com.tictactec.ta.lib.meta.annotation.FuncInfo;
import com.tictactec.ta.lib.meta.annotation.InputParameterInfo;
import com.tictactec.ta.lib.meta.annotation.IntegerList;
import com.tictactec.ta.lib.meta.annotation.IntegerRange;
import com.tictactec.ta.lib.meta.annotation.OptInputParameterInfo;
import com.tictactec.ta.lib.meta.annotation.OutputParameterInfo;
import com.tictactec.ta.lib.meta.annotation.RealRange;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class TALibFunctions {
   private static TALibFunction[] talibFunctions = loadFunctions();

   public static TALibFunction getFunction(String var0) {
      for (TALibFunction var4 : talibFunctions) {
         if (var4.getName().equals(var0)) {
            return var4;
         }
      }

      throw new RuntimeException(L.t("No such function %s", new Object[]{var0}));
   }

   public static TALibFunction[] getTALibFunctions() {
      return talibFunctions;
   }

   private static TALibFunction[] loadFunctions() {
      Method[] var0 = null;

      try {
         var0 = TALibFunctions.class.getClassLoader().loadClass("com.tictactec.ta.lib.CoreAnnotated").getMethods();
      } catch (SecurityException var18) {
         var18.printStackTrace();
      } catch (ClassNotFoundException var19) {
         var19.printStackTrace();
      }

      ArrayList var1 = new ArrayList();

      for (Method var5 : var0) {
         FuncInfo var6 = var5.getAnnotation(FuncInfo.class);
         if (var6 != null) {
            TALibFunction var7 = new TALibFunction(var5, var6.name(), var6.nbInput(), var6.nbOptInput(), var6.nbOutput());
            var1.add(var7);
            Annotation[][] var8 = var5.getParameterAnnotations();

            for (Annotation[] var12 : var8) {
               for (Annotation var16 : var12) {
                  if (var16 instanceof OutputParameterInfo) {
                     OutputParameterInfo var17 = (OutputParameterInfo)var16;
                     var7.addOutput(new TALibFunction.Param(var17.paramName(), var17.type().name(), var17.flags()));
                  } else if (var16 instanceof InputParameterInfo) {
                     InputParameterInfo var20 = (InputParameterInfo)var16;
                     var7.addInput(new TALibFunction.Param(var20.paramName(), var20.type().name(), var20.flags()));
                  } else if (var16 instanceof OptInputParameterInfo) {
                     OptInputParameterInfo var21 = (OptInputParameterInfo)var16;
                     var7.addOptInput(new TALibFunction.Param(var21.paramName(), var21.type().name(), var21.displayName(), var21.flags()));
                  } else if (var16 instanceof IntegerRange) {
                     IntegerRange var22 = (IntegerRange)var16;
                     var7.addIntegerRange(
                        var22.paramName(),
                        new TALibFunction.IntegerParams(
                           var22.min(), var22.max(), var22.defaultValue(), var22.suggested_start(), var22.suggested_end(), var22.suggested_increment()
                        )
                     );
                  } else if (var16 instanceof RealRange) {
                     RealRange var23 = (RealRange)var16;
                     var7.addDoubleRange(
                        var23.paramName(),
                        new TALibFunction.DoubleParams(
                           var23.min(), var23.max(), var23.defaultValue(), var23.suggested_start(), var23.suggested_end(), var23.suggested_increment()
                        )
                     );
                  } else if (var16 instanceof IntegerList) {
                     IntegerList var24 = (IntegerList)var16;
                     var7.addIntegerRange(var24.paramName(), new TALibFunction.IntegerParams(0, 0, var24.defaultValue(), 0, 0, 0));
                  }
               }
            }
         }
      }

      return var1.toArray(new TALibFunction[var1.size()]);
   }
}
