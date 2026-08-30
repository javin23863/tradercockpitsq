package com.strategyquant.tradinglib.backtestrunner;

import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyChecker {
   private static final Logger Log = LoggerFactory.getLogger(StrategyChecker.class);

   public static boolean strategyValid(Element var0) {
      try {
         ArrayList var1 = XMLUtil.getNestedElements(var0, "Rule");
         if (var1.size() == 0) {
            return false;
         }

         boolean var2 = false;

         for (int var3 = 0; var3 < var1.size(); var3++) {
            Element var4 = (Element)var1.get(var3);
            String var5 = var4.getAttributeValue("type");
            if (var5 != null) {
               Element var6 = var4.getChild("Then");
               if (var6 != null) {
                  List var7 = var6.getChildren("Item");
                  if (var7 != null) {
                     for (int var8 = 0; var8 < var7.size(); var8++) {
                        Element var9 = (Element)var7.get(var8);
                        String var10 = var9.getAttributeValue("mI");
                        if (var10 != null && var10.equals("Open")) {
                           var2 = true;
                           break;
                        }
                     }
                  }
               }
            }
         }

         return var2;
      } catch (Exception var11) {
         Log.error("Strategy validation failed", var11);
         return false;
      }
   }
}
