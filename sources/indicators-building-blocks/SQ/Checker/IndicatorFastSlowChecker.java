package SQ.Checker;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Checker;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorFastSlowChecker extends Checker {
   public static final Logger Log = LoggerFactory.getLogger("IndicatorFastSlowChecker");

   public boolean check(Element var1) {
      if (!this.checkFastSlow(var1)) {
         return false;
      }

      ArrayList var2 = new ArrayList();
      XMLUtil.findAll(var1, "Item", var2);
      if (var2 != null && var2.size() > 0) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            if (!this.checkFastSlow(var4)) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean checkFastSlow(Element var1) {
      List var2 = var1.getChildren("Param");
      Element var3 = this.findInParams(var2, "#Fast");
      if (var3 != null) {
         Element var4 = this.findInParams(var2, "#Slow");
         if (var4 != null) {
            return this.checkFastSlow(var3, var4);
         }
      }

      Element var6 = this.findInParams(var2, "#MinPeriod");
      if (var6 != null) {
         Element var5 = this.findInParams(var2, "#MaxPeriod");
         if (var5 != null) {
            return this.checkMinMax(var6, var5);
         }
      }

      return true;
   }

   private boolean checkFastSlow(Element var1, Element var2) {
      String var3 = var1.getText();
      String var4 = var2.getText();

      try {
         int var5 = Integer.parseInt(var3);
         int var6 = Integer.parseInt(var4);
         return var5 < var6;
      } catch (NumberFormatException var7) {
         return true;
      }
   }

   private boolean checkMinMax(Element var1, Element var2) {
      String var3 = var1.getText();
      String var4 = var2.getText();

      try {
         int var5 = Integer.parseInt(var3);
         int var6 = Integer.parseInt(var4);
         return var5 < var6;
      } catch (NumberFormatException var7) {
         return true;
      }
   }

   private Element findInParams(List<Element> var1, String var2) {
      for (Element var4 : var1) {
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.startsWith(var2)) {
            String var6 = var4.getAttributeValue("type");
            if (var6 != null && var6.equals("int")) {
               return var4;
            }
         }
      }

      return null;
   }
}
