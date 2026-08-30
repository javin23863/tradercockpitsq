package SQ.Checker;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Checker;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComparisonChecker extends Checker {
   public static final Logger Log = LoggerFactory.getLogger("ComparisonChecker");

   public boolean check(Element var1) {
      String var2 = var1.getAttributeValue("returnType");
      if (var2 != null && !var2.equals("boolean")) {
         return true;
      } else {
         Element var3 = this.getParameter(var1, "#Left#");
         Element var4 = this.getParameter(var1, "#Right#");
         if (var3 == null || var4 == null) {
            return this.checkIfItIsntNotBlock(var1);
         } else {
            return this.checkIncorrectNumberIndicatorsComparison(var3, var4, var1) ? false : !this.checkLeftRightAreSame(var3, var4);
         }
      }
   }

   private boolean checkIfItIsntNotBlock(Element var1) {
      if (!var1.getAttributeValue("key").equals("Not")) {
         return true;
      }

      Element var2 = this.getParameter(var1, "#Value#");
      if (var2 == null) {
         return true;
      }

      String var3 = var2.getAttributeValue("returnType");
      return var3 == null || var3.equals("boolean");
   }

   private boolean checkIncorrectNumberIndicatorsComparison(Element var1, Element var2, Element var3) {
      String var4 = var1.getAttributeValue("returnType");
      String var5 = var2.getAttributeValue("returnType");
      if (var4 != null && var5 != null) {
         if (!var4.equals(var5)) {
            return true;
         }

         if (var4.equals("number") && var5.equals("number")) {
            String var10 = var1.getAttributeValue("categoryType");
            String var11 = var2.getAttributeValue("categoryType");
            if (var10 != null && var11 != null) {
               String var8 = var1.getAttributeValue("key");
               String var9 = var2.getAttributeValue("key");
               if (var10.equals("indicator") && var11.equals("indicator")) {
                  if (!var8.equals(var9)) {
                     return true;
                  }
               } else if (var10.equals("indicator") && var11.equals("other")) {
                  if (!var9.equals("Number")) {
                     return true;
                  }
               } else if (var10.equals("other") && var11.equals("indicator")) {
                  if (!var8.equals("Number")) {
                     return true;
                  }
               } else if (var10.equals("other") && var11.equals("other")) {
                  if (var8.equals("Number")) {
                     if (this.isBarTimeBlock(var9)) {
                        return true;
                     }
                  } else if (var9.equals("Number")) {
                     if (!this.isBarTimeBlock(var8)) {
                        return true;
                     }

                     if (this.checkBadTimeRange(var1, var2)) {
                        return true;
                     }

                     if (var3.getAttributeValue("key").contains("Crosses")) {
                        return true;
                     }
                  } else if (this.isBarTimeBlock(var8) && this.isBarTimeBlock(var9)) {
                     return true;
                  }
               }
            }
         } else {
            String var6 = var1.getAttributeValue("key");
            if ((var6.equals("HighestInRange") || var6.equals("LowestInRange")) && !this.checkCorrectHHMMTime(var1)) {
               return true;
            }

            String var7 = var2.getAttributeValue("key");
            if ((var7.equals("HighestInRange") || var7.equals("LowestInRange")) && !this.checkCorrectHHMMTime(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean checkCorrectHHMMTime(Element var1) {
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && (var5.equals("#TimeFrom#") || var5.equals("#TimeTo#"))) {
            String var6 = var4.getText();
            if (var6 != null) {
               try {
                  int var7 = Integer.parseInt(var6);
                  int var8 = var7 / 100;
                  if (var8 >= 0 && var8 <= 23) {
                     int var9 = var7 % 100;
                     if (var9 >= 0 && var9 <= 59) {
                        continue;
                     }

                     Log.info("Bad minute: {}", var7);
                     return false;
                  }

                  Log.info("Bad hour: {}", var7);
                  return false;
               } catch (NumberFormatException var10) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean checkBadTimeRange(Element var1, Element var2) {
      int var3 = 0;

      try {
         Element var4 = var2.getChild("Param");
         if (var4 == null) {
            return false;
         }

         String var5 = var4.getTextTrim();
         var3 = (int)Double.parseDouble(var5);
      } catch (Exception var6) {
         return false;
      }

      String var8 = var1.getAttributeValue("key");
      if (!var8.contains("Hour") || var3 >= 0 && var3 <= 23) {
         return !var8.contains("Minute") || var3 >= 0 && var3 <= 59 ? var8.contains("DayOfWeek") && (var3 < 0 || var3 > 6) : true;
      } else {
         return true;
      }
   }

   private boolean isBarTimeBlock(String var1) {
      return var1.contains("Hour") || var1.contains("Minute") || var1.contains("DayOfWeek");
   }

   private boolean checkLeftRightAreSame(Element var1, Element var2) {
      String var3 = XMLUtil.xmlToStringRaw(var1);
      String var4 = XMLUtil.xmlToStringRaw(var2);
      return var3.equals(var4);
   }

   private Element getParameter(Element var1, String var2) {
      for (Element var4 : var1.getChildren()) {
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.equals(var2)) {
            return var4.getChild("Item");
         }
      }

      return null;
   }
}
