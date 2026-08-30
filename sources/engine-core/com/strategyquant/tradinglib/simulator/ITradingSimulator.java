package com.strategyquant.tradinglib.simulator;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.ticksimulator.ITickSimulator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.strategy.MarketData;

public interface ITradingSimulator extends IExecutionEngine {
   void setConnection(String var1);

   void setTestPrecision(int var1);

   int getTestPrecision();

   ITradingSimulator clone();

   ITickSimulator getTickSimulator();

   ReservedBarsType getReservedBarsType();

   void eventNewTick(TickEvent var1, double var2, double var4);

   void eventNewTick(TickEvent var1, double var2, double var4, BarUpdateInfo var6) throws TradingException;

   void initialize(MarketData var1);

   void destroy();

   void eventEndOfTest();

   int getEngineId();

   void initSettings(SettingsMap var1) throws TradingException;

   int getBarEventType();

   boolean getApplyExitsAtTheEndOfRule();

   void eventNoTrade();
}
