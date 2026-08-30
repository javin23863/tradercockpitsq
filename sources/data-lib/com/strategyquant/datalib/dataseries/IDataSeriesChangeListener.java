package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.TradingException;

public interface IDataSeriesChangeListener {
   void changed(int var1, int var2) throws TradingException;
}
