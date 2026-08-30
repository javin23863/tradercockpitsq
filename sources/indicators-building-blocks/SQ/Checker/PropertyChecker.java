package SQ.Checker;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Checker;
import org.jdom2.Element;

public class PropertyChecker extends Checker {
   public boolean check(Element var1) {
      String var2 = var1.getAttributeValue("returnType");
      if (var2 != null && !var2.equals("boolean")) {
         return true;
      }

      String var3 = var1.getAttributeValue("key");
      if (!var3.equalsIgnoreCase("IsRising") && !var3.equalsIgnoreCase("IsFalling")) {
         return true;
      }

      Element var4 = this.getParameter(var1, "Indicator");
      if (var4 == null) {
         return true;
      }

      String var5 = XMLUtil.xmlToStringRaw(var4);
      return !var5.contains("#Shift#") ? false : !var5.contains("Hour") && !var5.contains("Minute") && !var5.contains("DayOfWeek");
   }

   private Element getParameter(Element var1, String var2) {
      var2 = this.fixKey(var2);

      for (Element var4 : var1.getChildren()) {
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.equals(var2)) {
            return var4;
         }
      }

      return null;
   }

   private String fixKey(String var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append("#");
      var2.append(var1);
      var2.append("#");
      return var2.toString();
   }
}
