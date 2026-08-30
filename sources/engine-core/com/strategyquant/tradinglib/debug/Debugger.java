package com.strategyquant.tradinglib.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Debugger {
   private static final Logger Log = LoggerFactory.getLogger(Debugger.class);

   public void debug(String var1, String var2) {
      DebugConsole.log(var1, var2);
   }

   public void fdebug(String var1, String var2) {
      Log.info(var1 + " - " + var2);
   }
}
