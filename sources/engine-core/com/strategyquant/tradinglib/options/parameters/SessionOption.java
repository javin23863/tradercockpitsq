package com.strategyquant.tradinglib.options.parameters;

import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import java.util.TreeMap;

public class SessionOption extends TradingOption {
   @Parameter(name = "Session", defaultValue = "No Session", category = "Session")
   @Editor(type = 40)
   @Help("Set trading session. Only for stocks & futures, it has no effect in MetaTrader4/5 engine!!!")
   @ForEngine("MC,TS")
   public String Session;

   public SessionOption() {
      this.inbuild = true;
   }

   @Override
   public TreeMap<String, String> getCustomOptions() {
      TreeMap var1 = new TreeMap();

      for (Session var3 : SessionManager.getSessions()) {
         String var4 = var3.getSessionName();
         var1.put(var4, var4);
      }

      return var1;
   }

   @Override
   public boolean OnBarUpdate(StrategyBase var1) {
      return true;
   }

   public TradingOption clone() {
      SessionOption var1 = new SessionOption();
      var1.Session = this.Session;
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
