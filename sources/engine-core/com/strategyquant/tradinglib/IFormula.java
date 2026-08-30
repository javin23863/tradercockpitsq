package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import org.jdom2.Element;

public interface IFormula {
   double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException;

   IFormula newFormulaInstance(StrategyBase var1, Element var2) throws BlockDefinitionException;

   boolean isNoneValue();

   boolean isBooleanValue();
}
