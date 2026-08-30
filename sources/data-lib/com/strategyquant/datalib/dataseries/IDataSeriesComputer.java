package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.TradingException;

public interface IDataSeriesComputer {
   void compute(int var1, int var2) throws TradingException;
}
