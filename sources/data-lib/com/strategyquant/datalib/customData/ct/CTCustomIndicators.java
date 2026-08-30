package com.strategyquant.datalib.customData.ct;

import com.strategyquant.lib.XMLUtil;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CTCustomIndicators extends HashMap<String, CTCustomIndicator> {
   public static final Logger Log = LoggerFactory.getLogger(CTCustomIndicators.class);

   public CTCustomIndicators(Element var1) throws Exception {
      try {
         for (Element var4 : var1.getChildren("Item")) {
            CTCustomIndicator var5 = new CTCustomIndicator(var4);
            if (!this.containsKey(var5.fileName)) {
               this.put(var5.fileName, var5);
            }
         }
      } catch (Exception var6) {
         throw new Exception("Failed to load Custom Indicators - " + var6.getMessage(), var6);
      }
   }

   public void addSourceCodesToItem(Element var1, String var2) throws Exception {
      var1.setAttribute("cdataIndyName", var2);
      CTCustomIndicator var3 = this.get(var2);
      if (var3 == null) {
         Log.error("Cannot load data info for CustomIndicator {}", var2);
         var1.setAttribute("mt4", "");
         var1.setAttribute("mt5", "");
         var1.setAttribute("el", "");
         var1.setAttribute("jf", "");
      } else {
         String var4 = null;
         Element var6 = XMLUtil.findFirstWithKey(var1, "Param", "#Output#");
         if (var6 != null) {
            var4 = var6.getText();
         }

         CTCustomIndicatorOutput var7 = var3.outputs.get(var4);
         if (var7 == null) {
            throw new Exception(String.format("CustomIndicator[%s].Output '%s' not found.", var2, var4));
         }

         try {
            String var5 = var7.mt4;
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("mt4", var5);
            var5 = var7.mt5;
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("mt5", var5);
            var5 = var7.el;
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("el", var5);
            var5 = var7.jf;
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("jf", var5);
         } catch (Exception var9) {
            Log.error("Cannot load Src value", var9);
         }
      }
   }
}
