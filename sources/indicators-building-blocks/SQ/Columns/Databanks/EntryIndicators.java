package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.jdom2.Element;

public class EntryIndicators extends DatabankColumn {
   public EntryIndicators() {
      super(L.tsq("Entry indicators"), "Text", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Which indicators are used in entry conditions"));
      this.setWidth(150);
      this.printsSpecialValue(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      if (var1.specialValues().containsKey(SpecialValues.EntryIndicators)) {
         return var1.specialValues().getString(SpecialValues.EntryIndicators);
      }

      String var6 = this.recognizeEntryIndicators(var1);
      var1.specialValues().setString(SpecialValues.EntryIndicators, var6);
      return var6;
   }

   private String recognizeEntryIndicators(ResultsGroup var1) {
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
               if (var8.size() >= 1) {
                  this.findIndysRecursive((Element)var8.get(0), var5, false);
               }

               if (var8.size() >= 2) {
                  this.findIndysRecursive((Element)var8.get(1), var5, false);
               }
            } else {
               this.findIndysRecursive((Element)var4.get(0), var5, false);
               if (var4.size() > 1) {
                  this.findIndysRecursive((Element)var4.get(1), var5, false);
               }
            }

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
      boolean var3 = false;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         String var5 = (String)var1.get(var4);
         if (var5 != null) {
            if (var5.equals("AlwaysTrue")) {
               var3 = true;
            } else {
               var2.add(var5);
            }
         }
      }

      if (var2.size() == 0 && var3) {
         var2.add("AlwaysTrue");
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
               String var12 = var6.getAttributeValue("key");
               if (var12 != null && var12.startsWith("CBlock_")) {
                  BlockDefinition var13 = CustomBlocks.getBlock(var12);
                  if (var13 != null) {
                     var2.add(var13.name);
                  } else {
                     var2.add("Unknown custom block");
                  }
                  continue;
               }

               String var9 = var6.getAttributeValue("categoryType");
               if (var9 != null) {
                  String var10 = var6.getAttributeValue("mI");
                  String var11 = var6.getAttributeValue("name");
                  if (var9.equals("simpleRules")) {
                     if (var10 != null && !var10.equals("") && !var10.equals("StrategyControl") && !var3) {
                        if (var10.equals("BarAndTime")) {
                           var2.add(var12);
                        } else {
                           var2.add(var11);
                        }
                     }
                  } else if (var9.equals("indicator")) {
                     if (!var3) {
                        var2.add(var12);
                     }
                  } else if (var9.equals("priceValue")) {
                     if (!var3) {
                        var2.add(var12);
                     }
                  } else if (var9.equals("priceRange")) {
                     if (!var3) {
                        var2.add(var12);
                     }
                  } else if (var9.equals("other") && var10 != null && var10.equals("BarAndTime") && !var3) {
                     var2.add(var12);
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
