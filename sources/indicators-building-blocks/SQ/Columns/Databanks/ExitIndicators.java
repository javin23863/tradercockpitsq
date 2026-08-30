package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.jdom2.Element;

public class ExitIndicators extends DatabankColumn {
   public ExitIndicators() {
      super(L.tsq("Exit indicators"), "Text", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Which indicators are used in exit conditions"));
      this.setWidth(100);
      this.printsSpecialValue(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      if (var1.subResult(var2).containsKey(SpecialValues.ExitIndicators)) {
         return var1.subResult(var2).getString(SpecialValues.ExitIndicators);
      }

      String var6 = this.recognizeExitIndicators(var1);
      var1.specialValues().setString(SpecialValues.ExitIndicators, var6);
      return var6;
   }

   private String recognizeExitIndicators(ResultsGroup var1) {
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
            String var6 = ((Element)var4.get(0)).getAttributeValue("type");
            if (var6 != null && var6.equals("Signal")) {
               Element var7 = ((Element)var4.get(0)).getChild("signals");
               if (var7 == null) {
                  return "N/A";
               }

               List var8 = var7.getChildren("signal");
               if (var8.size() >= 3) {
                  this.findIndysRecursive((Element)var8.get(2), var5);
               }

               if (var8.size() >= 4) {
                  this.findIndysRecursive((Element)var8.get(3), var5);
               }
            } else {
               if (var4.size() >= 3) {
                  this.findIndysRecursive((Element)var4.get(2), var5);
               }

               if (var4.size() >= 4) {
                  this.findIndysRecursive((Element)var4.get(3), var5);
               }
            }

            this.findExitAfterBars(var4, var5);
            return this.convertToString(var5);
         } else {
            return "N/A";
         }
      } catch (Exception var9) {
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

   private void findIndysRecursive(Element var1, ArrayList<String> var2) {
      ArrayList var3 = XMLUtil.getNestedElements(var1, "Item");
      if (var3 != null && var3.size() != 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            String var6 = var5.getAttributeValue("key");
            if (var6 != null && var6.startsWith("CBlock_")) {
               BlockDefinition var9 = CustomBlocks.getBlock(var6);
               if (var9 != null) {
                  var2.add(var9.name);
               } else {
                  var2.add("Unknown custom block");
               }
            } else {
               String var7 = var5.getAttributeValue("categoryType");
               if (var7 != null) {
                  String var8 = var5.getAttributeValue("mI");
                  if (var7.equals("simpleRules")) {
                     if (var8 != null && !var8.equals("") && !var8.equals("StrategyControl")) {
                        var2.add(var8);
                     }
                  } else if (var7.equals("indicator")) {
                     var2.add(var6);
                  } else if (var7.equals("priceValue")) {
                     var2.add(var6);
                  } else if (var7.equals("priceRange")) {
                     var2.add(var6);
                  } else if (var7.equals("other") && var8 != null && var8.equals("BarAndTime")) {
                     var2.add(var6);
                  }
               }
            }
         }
      }
   }

   private void findExitAfterBars(List<Element> var1, ArrayList<String> var2) {
      for (Element var4 : var1) {
         this.findExitAfterBarsRecursive(var4, var2);
      }
   }

   private void findExitAfterBarsRecursive(Element var1, ArrayList<String> var2) {
      ArrayList var3 = XMLUtil.getNestedElements(var1, "Item");
      if (var3 != null && var3.size() != 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            String var6 = var5.getAttributeValue("key");
            if (var6 != null && (var6.startsWith("EnterAt") || var6.startsWith("EnterReverseAt"))) {
               Element var7 = XMLUtil.getItemParameterNoException(var5, "#ExitAfterBars.ExitAfterBars#");
               if (var7 != null) {
                  String var8 = var7.getText();
                  if (var8 != null && !var8.equals("0")) {
                     var2.add("ExitAfterXBars");
                  }
               }
            }
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
