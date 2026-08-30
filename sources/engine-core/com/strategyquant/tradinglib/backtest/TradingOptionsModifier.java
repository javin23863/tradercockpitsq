package com.strategyquant.tradinglib.backtest;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;

public class TradingOptionsModifier {
   public static void modifyTradingOptions(TradingOptions var0, Element var1, Element var2) throws Exception {
      Element var3 = null;
      if (var2 != null) {
         Element var4 = var2.getChild("Options");
         Element var5 = XMLUtil.tryAddElement(var4, "BuildTradingOptions");
         var3 = XMLUtil.tryAddElement(var5, "Params");
      }

      Variables var22 = new Variables(var1);
      HashMap var23 = new HashMap();

      for (int var6 = 0; var6 < var22.size(); var6++) {
         Variable var7 = var22.get(var6);
         var23.put(var7.getId(), var7);
      }

      for (int var24 = 0; var24 < var0.size(); var24++) {
         TradingOption var25 = var0.get(var24);
         String var8 = var25.getClass().getSimpleName();
         ArrayList var9 = var25.getParams();

         for (int var10 = 0; var10 < var9.size(); var10++) {
            IPGParameter var11 = (IPGParameter)var9.get(var10);
            String var12 = var11.getKey();
            String var13 = String.format("%s.%s", var8, var12);
            if (var23.containsKey(var13)) {
               Variable var14 = (Variable)var23.get(var13);
               String var15 = var14.getValue();
               if (var14.getInternalType() == 5) {
                  var15 = "" + SQTime.HHMMToMinutes(Integer.parseInt(var15)) * 60;
               }

               var25.setParameterValue(var11.getKey(), var15);
               if (var3 != null) {
                  List var16 = var3.getChildren("Param");
                  boolean var17 = false;

                  for (int var18 = var16.size() - 1; var18 >= 0; var18--) {
                     Element var19 = (Element)var16.get(var18);
                     String var20 = var19.getAttributeValue("className");
                     String var21 = var19.getAttributeValue("key");
                     if (var20 != null && var21 != null && var20.equals(var8) && var21.equals(var12)) {
                        var19.setText(var15);
                        var17 = true;
                        break;
                     }
                  }

                  if (!var17) {
                     var3.addContent(new Element("Param").setAttribute("className", var8).setAttribute("key", var12).setText(var15));
                  }
               }
            }
         }
      }
   }
}
