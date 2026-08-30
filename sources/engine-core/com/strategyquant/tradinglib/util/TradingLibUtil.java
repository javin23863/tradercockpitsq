package com.strategyquant.tradinglib.util;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.SQStats;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import org.jdom2.Content;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingLibUtil {
   private static final Logger Log = LoggerFactory.getLogger(TradingLibUtil.class);

   public static Object elementToValue(Element var0) {
      Content var1 = var0.getContent(0);
      String var2 = var0.getAttributeValue("type");
      Object var3 = null;
      if (var2 == null) {
         return null;
      }

      switch (var2) {
         case "String":
            var3 = var1.getValue().intern();
            break;
         case "Byte":
            var3 = Byte.parseByte(var1.getValue());
            break;
         case "Short":
            var3 = Short.parseShort(var1.getValue());
            break;
         case "Integer":
            var3 = Integer.parseInt(var1.getValue());
            break;
         case "Long":
            var3 = Long.parseLong(var1.getValue());
            break;
         case "Float":
            var3 = Float.parseFloat(var1.getValue());
            break;
         case "Double":
            var3 = Double.parseDouble(var1.getValue());
            break;
         case "Element":
            var3 = (Element)var0.getChildren().get(0);
            break;
         default:
            Object var6 = null;

            try {
               return createInstance(var2, (Element)var0.getChildren().get(0));
            } catch (Exception var9) {
               var6 = null;

               try {
                  return createInstance(var2, var0);
               } catch (Exception var8) {
                  Log.error("Error while instantiating object of type using child: '" + var2 + "'. Exc.", (Throwable)var6);
                  Log.error("Error while instantiating object of type using element: '" + var2 + "'. Exc.", var8);
               }
            }
      }

      return var3;
   }

   private static Object createInstance(String var0, Element var1) throws Exception {
      Class var2 = null;
      if (!var0.startsWith("SQ.")) {
         if (var0.endsWith("SQStats")) {
            SQStats var3 = new SQStats();
            var3.setFromXML(var1);
            return var3;
         }

         if (var0.contains("com.strategyquant.lib.trading.")) {
            var0 = var0.replace("com.strategyquant.lib.trading.", "com.strategyquant.tradinglib.");
         } else if (var0.contains("com.strategyquant.lib.optimization.")) {
            var0 = var0.replace("com.strategyquant.lib.optimization.", "com.strategyquant.tradinglib.optimization.");
         }

         var2 = Class.forName(var0);
      } else {
         URLClassLoader var6 = MainApp.getSnippetsClassLoader();
         var2 = var6.loadClass(var0);
      }

      if (var2 != null) {
         Object var7 = var2.newInstance();
         Method var4 = var2.getMethod("setFromXML", Element.class);
         var4.invoke(var7, var1);
         return var7;
      } else {
         return null;
      }
   }
}
