package com.strategyquant.tradinglib.formulas;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.StrategyBase;
import org.jdom2.Element;

public class EmptyFormula implements IFormula {
   @Override
   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return -9.9999999E7;
   }

   @Override
   public IFormula newFormulaInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      return null;
   }

   @Override
   public boolean isNoneValue() {
      return true;
   }

   @Override
   public boolean isBooleanValue() {
      return false;
   }
}
