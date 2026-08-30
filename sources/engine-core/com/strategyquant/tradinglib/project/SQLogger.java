package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLogger {
   public static final Logger Log = LoggerFactory.getLogger(SQLogger.class);

   public static void log(String var0, String var1) {
      if (!var0.equals("WalkForwardOptimization_CrossCheck") && !var0.equals("WalkForwardMatrix_CrossCheck")) {
         if (!MainApp.runInConsoleWithoutActiveWebserver()) {
            try {
               String var2 = SQTime.getLogTime(false, false);
               StringBuilder var3 = new StringBuilder(var2);
               var3.append(" ");
               var3.append(var1);
               var3.append("\n");
               ProjectEngine.get(var0).log.add(var3.toString());
            } catch (Exception var4) {
               Log.error("Error while writing log. Exc.", var4);
            }
         }
      }
   }
}
