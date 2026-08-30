package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.tradinglib.gp.IGPFinishedListener;

public interface IBuildEngine {
   void testRun() throws Exception;

   void start() throws Exception;

   void setFinishListener(IGPFinishedListener var1);

   void destroy();

   boolean getFinishSent();

   void setStrategyNamePrefix(String var1);
}
