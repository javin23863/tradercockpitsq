package com.strategyquant.tradinglib.options;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.options.parameters.BrokerOption;
import com.strategyquant.tradinglib.options.parameters.LimitOver;
import com.strategyquant.tradinglib.options.parameters.MarketOpenSession;
import com.strategyquant.tradinglib.options.parameters.RealisticGapsHandling;
import com.strategyquant.tradinglib.options.parameters.ReservedBars;
import com.strategyquant.tradinglib.options.parameters.SessionOption;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.options.parameters.StoreChartData;
import java.util.List;
import org.jdom2.Element;

public class TradingOptionsList extends CustomClasses<TradingOption> {
   private static TradingOptionsList instance;

   public static TradingOptionsList getInstance() {
      if (instance == null) {
         instance = new TradingOptionsList();
      }

      return instance;
   }

   private TradingOptionsList() {
      this.setDirName("TradingOptions");
      this.setExpectedClassType(TradingOption.class);
      this.loadAvailableClasses();
   }

   private void loadBuiltinClass(String var1) {
      try {
         this.add(this.getNewBuiltInClass(var1));
      } catch (Exception var4) {
         String var3 = String.format("Error loading builtin class '%s': '%s'", var1, var4);
         this.addError(var1, var3);
      }
   }

   public void loadAvailableClasses() {
      super.loadAvailableClasses();
      this.loadBuiltinClass("SessionOption");
      this.loadBuiltinClass("ReservedBars");
      this.loadBuiltinClass("StoreChartData");
      this.loadBuiltinClass("RealisticGapsHandling");
      this.loadBuiltinClass("StockpickerOptions");
      this.loadBuiltinClass("BrokerOption");
      this.loadBuiltinClass("MarketOpenSession");
      this.loadBuiltinClass("LimitOver");
   }

   public TradingOptions cloneAvailableClasses() {
      TradingOptions var1 = new TradingOptions();

      for (TradingOption var3 : this.getAvailableClasses()) {
         String var4 = var3.getClass().getSimpleName();
         TradingOption var5 = this.getNewBuiltInClass(var4);
         if (var5 == null) {
            var5 = (TradingOption)this.createNew(var4);
         }

         var1.add(var5);
      }

      return var1;
   }

   public TradingOptions parseOptionsFromXml(Element var1) {
      TradingOptions var2 = this.cloneAvailableClasses();
      if (var1 == null) {
         return var2;
      }

      Element var3 = XMLUtil.tryAddElement(var1, "Params");
      List var4 = var3.getChildren("Param");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);

         try {
            String var7 = var6.getAttributeValue("className");
            if (!var7.equals("DontUsePriceRangeInConditions")
               && !var7.equals("ReplacePendingOrders")
               && !var7.equals("ComputeDailyStats")
               && !var7.equals("UseInitialSL")) {
               try {
                  TradingOption var8 = this.findClassByName(var2, var7);
                  var8.setFromParameterEl(var6);
               } catch (Exception var9) {
                  Log.debug(var7 + " is not exist. " + var7 + " will not be loaded.");
               }
            }
         } catch (Exception var10) {
            Log.error("Exc.", var10);
         }
      }

      return var2;
   }

   public TradingOption findClassByName(TradingOptions var1, String var2) throws NonexistingCustomClassException {
      for (TradingOption var4 : var1) {
         if (var4.getClass().getSimpleName().equals(var2)) {
            return var4;
         }
      }

      throw new NonexistingCustomClassException(var2);
   }

   public TradingOption getNewBuiltInClass(String var1) {
      switch (var1) {
         case "SessionOption":
            return new SessionOption();
         case "StoreChartData":
            return new StoreChartData();
         case "RealisticGapsHandling":
            return new RealisticGapsHandling();
         case "ReservedBars":
            return new ReservedBars();
         case "StockpickerOptions":
            return new StockpickerOptions();
         case "BrokerOption":
            return new BrokerOption();
         case "MarketOpenSession":
            return new MarketOpenSession();
         case "LimitOver":
            return new LimitOver();
         default:
            return null;
      }
   }
}
