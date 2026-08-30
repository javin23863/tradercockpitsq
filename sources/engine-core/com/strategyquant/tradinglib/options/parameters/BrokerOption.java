package com.strategyquant.tradinglib.options.parameters;

import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import java.util.TreeMap;

public class BrokerOption extends TradingOption {
   @Parameter(name = "Broker", defaultValue = "No filter", category = "Stockpicker")
   @Editor(type = 40)
   @ForEngine("SP")
   @Help("Set broker")
   public String PickerBroker;

   public BrokerOption() {
      this.inbuild = true;
   }

   @Override
   public TreeMap<String, String> getCustomOptions() {
      TreeMap var1 = new TreeMap();

      for (BrokerDto var3 : BrokerManager.getInstance().getAllBrokers()) {
         String var4 = var3.getName();
         var1.put(var4, var4);
      }

      var1.put("No filter", "No filter");
      return var1;
   }

   @Override
   public boolean OnBarUpdate(StrategyBase var1) {
      return true;
   }

   public TradingOption clone() {
      BrokerOption var1 = new BrokerOption();
      var1.PickerBroker = this.PickerBroker;
      return var1;
   }

   @Override
   public TradingOption getClone() throws CloneNotSupportedException {
      return this.clone();
   }

   @Override
   public boolean isUsedInTrading() {
      return false;
   }
}
