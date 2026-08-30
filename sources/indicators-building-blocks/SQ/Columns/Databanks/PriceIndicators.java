package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.jdom2.Element;

public class PriceIndicators extends DatabankColumn {
   public PriceIndicators() {
      super(L.tsq("Price indicators"), "Text", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Which indicators are used in entry price levels (Enter at Stop or Limit"));
      this.setWidth(100);
      this.printsSpecialValue(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      if (var1.specialValues().containsKey(SpecialValues.PriceIndicators)) {
         return var1.specialValues().getString(SpecialValues.PriceIndicators);
      }

      String var6 = this.recognizePriceIndicators(var1);
      var1.specialValues().setString(SpecialValues.PriceIndicators, var6);
      return var6;
   }

   private String recognizePriceIndicators(ResultsGroup var1) {
      try {
         Element var2 = var1.getStrategyXml();
         if (var2 == null) {
            return "N/A";
         }

         Element var3 = this.getOnBarUpdateEvent(var2);
         if (var3 == null) {
            return "N/A";
         }

         List var4 = var3.getChildren("Rule");
         if (var4 != null && var4.size() != 0) {
            ArrayList var5 = new ArrayList();

            for (int var6 = 0; var6 < var4.size(); var6++) {
               this.findIndysRecursive((Element)var4.get(var6), var5, false);
            }

            return this.convertToString(var5);
         } else {
            return "N/A";
         }
      } catch (Exception var7) {
         return "N/A";
      }
   }

   private String convertToString(ArrayList<String> var1) {
      TreeSet var2 = new TreeSet();

      for (int var3 = 0; var3 < var1.size(); var3++) {
         var2.add((String)var1.get(var3));
      }

      return String.join(",", var2);
   }

   private void findIndysRecursive(Element var1, ArrayList<String> var2, boolean var3) {
      List var4 = var1.getChildren();
      if (var4 != null && var4.size() != 0) {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var6 = (Element)var4.get(var5);
            boolean var7 = var3;
            if (var6.getName().equals("Param")) {
               String var8 = var6.getAttributeValue("key");
               if (var8 != null && var8.equals("#Price#")) {
                  var7 = true;
               }
            } else if (var6.getName().equals("Item")) {
               String var10 = var6.getAttributeValue("categoryType");
               if (var10 != null) {
                  if (var10.equals("simpleRules")) {
                     String var9 = var6.getAttributeValue("mI");
                     if (var9 != null && !var9.equals("") && var3) {
                        var2.add(var9);
                     }
                  } else if (var10.equals("indicator") || var10.equals("priceValue") || var10.equals("priceRange")) {
                     String var11 = var6.getAttributeValue("key");
                     if (var3) {
                        var2.add(var11);
                     }
                  }
               }
            }

            this.findIndysRecursive(var6, var2, var7);
         }
      }
   }

   private Element getOnBarUpdateEvent(Element var1) {
      Element var2 = var1.getChild("Strategy");
      if (var2 == null) {
         return null;
      }

      Element var3 = var2.getChild("Rules");
      if (var3 == null) {
         return null;
      }

      Element var4 = var3.getChild("Events");
      if (var4 == null) {
         return null;
      }

      List var5 = var4.getChildren("Event");
      if (var5 != null && var5.size() != 0) {
         for (int var6 = 0; var6 < var5.size(); var6++) {
            Element var7 = (Element)var5.get(var6);
            String var8 = var7.getAttributeValue("key");
            if (var8.equals("OnBarUpdate")) {
               return var7;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
