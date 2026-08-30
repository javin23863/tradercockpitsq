package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.strategy.StrategiesList;

public class TradingUtils {
   public static StrategyBase getStrategy(SettingsMap var0) throws TradingException {
      if (var0.containsKey("StrategyObject")) {
         if (!(var0.get("StrategyObject") instanceof StrategyBase)) {
            throw new TradingException(
               "Setting 'TradingSetup.StrategyObject was set, but it has incorrect value! It must be an instance of StrategyBase object."
            );
         } else {
            return (StrategyBase)var0.get("StrategyObject");
         }
      } else if (var0.containsKey("StrategyClass")) {
         if (!(var0.get("StrategyClass") instanceof String)) {
            throw new TradingException(
               "Setting 'TradingSetup.StrategyClass was set, but it has incorrect value! It must be a string with name of existing strategy."
            );
         }

         String var1 = (String)var0.get("StrategyClass");

         try {
            return StrategiesList.create(var1);
         } catch (NonexistingCustomClassException var3) {
            throw new TradingException("Cannot create an instance of '" + var1 + "' class.");
         }
      } else {
         throw new TradingException("Setting 'TradingSetup.StrategyClass' is not set.");
      }
   }
}
