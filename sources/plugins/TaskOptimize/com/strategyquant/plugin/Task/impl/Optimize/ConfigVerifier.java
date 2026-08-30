package com.strategyquant.plugin.Task.impl.Optimize;

import com.strategyquant.lib.SQTime;
import java.text.ParseException;
import java.util.ArrayList;
import org.jdom2.Element;

public class ConfigVerifier {
   public ArrayList<String> verifyConfig(Element var1) {
      ArrayList var2 = new ArrayList();
      Element var3 = var1.getChild("Settings");
      if (var3 == null) {
         var2.add("Invalid XML configuration.");
      } else {
         this.checkDataConfig(var2, var3);
      }

      return var2;
   }

   private void checkDataConfig(ArrayList<String> var1, Element var2) {
      Element var3 = var2.getChild("Data");
      if (var3 == null) {
         var1.add("Cannot find Data settings.");
      } else {
         Element var4 = var3.getChild("Setups");
         if (var4 == null) {
            var1.add("Cannot find Data.Setups settings.");
         } else if (var4.getChildren("Setup").size() == 0) {
            var1.add("You have to choose at least one backtest!");
         } else {
            int var5 = 0;

            for (Element var7 : var4.getChildren("Setup")) {
               var5++;
               int var8 = 0;

               for (Element var10 : var7.getChildren("Chart")) {
                  this.checkChart(var10, var5, ++var8, var1);
               }

               if (this.checkDate(var7.getAttributeValue("dateFrom"), "Date From", var5, var1)
                  && this.checkDate(var7.getAttributeValue("dateTo"), "Date To", var5, var1)) {
                  if (var7.getAttributeValue("slippage") != null
                     && !var7.getAttributeValue("slippage").equals("")
                     && !var7.getAttributeValue("slippage").equals("undefined")) {
                     if (var7.getAttributeValue("minDist") != null
                        && !var7.getAttributeValue("minDist").equals("")
                        && !var7.getAttributeValue("minDist").equals("undefined")) {
                        if (var7.getAttributeValue("commission") != null
                           && !var7.getAttributeValue("commission").equals("")
                           && !var7.getAttributeValue("commission").equals("undefined")) {
                           continue;
                        }

                        var1.add(String.format("Commission in %s is undefined!", var5, this.getBacktestLabel(var5)));
                        return;
                     }

                     var1.add(String.format("Min Distance in %s is undefined!", var5, this.getBacktestLabel(var5)));
                     return;
                  }

                  var1.add(String.format("Slippage in %s is undefined!", var5, this.getBacktestLabel(var5)));
                  return;
               }
               break;
            }

            return;
         }
      }
   }

   private void checkChart(Element var1, int var2, int var3, ArrayList<String> var4) {
      if (var1.getAttributeValue("symbol") == null || var1.getAttributeValue("symbol").equals("") || var1.getAttributeValue("symbol").equals("undefined")) {
         var4.add(String.format("Symbol in Chart %d in %s is undefined!", var3, this.getBacktestLabel(var2)));
      } else if (var1.getAttributeValue("timeframe") == null
         || var1.getAttributeValue("timeframe").equals("")
         || var1.getAttributeValue("timeframe").equals("undefined")) {
         var4.add(String.format("Timeframe in Chart %d in %s is undefined!", var3, this.getBacktestLabel(var2)));
      } else if (var1.getAttributeValue("spread") == null
         || var1.getAttributeValue("spread").equals("")
         || var1.getAttributeValue("spread").equals("undefined")) {
         var4.add(String.format("Spread in Chart %d in %s is undefined!", var3, this.getBacktestLabel(var2)));
      }
   }

   private boolean checkDate(String var1, String var2, int var3, ArrayList<String> var4) {
      if (var1 != null && !var1.equals("") && !var1.equals("undefined")) {
         try {
            long var5 = SQTime.parseDateToMilis(var1);
            return true;
         } catch (ParseException var7) {
            var4.add(String.format("%s in %s has incorrect value'%s'!", var2, this.getBacktestLabel(var3), var1));
            return false;
         }
      } else {
         var4.add(String.format("%s in %s is undefined!", var2, this.getBacktestLabel(var3)));
         return false;
      }
   }

   private String getBacktestLabel(int var1) {
      return var1 == 1 ? "Main backtest" : String.format("Backtest %d", var1);
   }
}
