package com.strategyquant.datalib.ticksimulator;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;

public interface ITickSimulator {
   void init(VersatileData var1);

   boolean getNextTick(TickEvent var1);

   void setSymbolsSpread(SpreadsMap var1);
}
