package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;

public interface DataModifierCallback {
   void modifyData(TickEvent var1);

   void modifyData(TickEvent var1, double var2);

   void modifyOHLCData(VersatileData var1);
}
