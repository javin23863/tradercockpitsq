package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.indicator.IndicatorBase;
import java.lang.reflect.Field;
import org.jdom2.Attribute;

public class IndicatorDisplayFormatter {
   public static String format(IndicatorBase var0, BlockDefinition var1) {
      Attribute var2 = var1.getXml().getAttribute("display");
      if (var2 == null) {
         return var1.getName();
      }

      String var3 = var2.getValue();
      Field[] var4 = var0.getClass().getDeclaredFields();

      for (Field var8 : var4) {
         try {
            Object var9 = var8.get(var0);
            String var10 = var9 == null ? "" : var9.toString();
            String var11 = "#" + var8.getName() + "#";
            var3 = var3.replace(var11, var10);
         } catch (Exception var12) {
         }
      }

      while (true) {
         int var13 = var3.indexOf("#");
         if (var13 == -1) {
            break;
         }

         int var14 = var3.indexOf("#", var13 + 1);
         if (var14 == -1) {
            break;
         }

         String var15 = var3.substring(0, var13);
         String var16 = var14 > var3.length() ? "" : var3.substring(var14 + 1);
         var3 = var15 + var16;
      }

      return var3.replace(".[]", "").replace("[]", "");
   }
}
