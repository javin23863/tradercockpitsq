package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;

public interface IActionEventListener {
   void OnActionEvent(StrategyBase var1) throws TradingException;
}
