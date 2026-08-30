package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=200, name="Fixed value (in pips)", formula="Range")
public class FixedValue extends FormulaBlock {

	@Parameter(defaultValue="50", minValue=1, maxValue=9999999, step=1, builderMinValue=5, builderMaxValue=100, builderStep=1, postfix="pips")
	public double Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		if(Value == 0) return 0;
		
		return strategy.convertPipsToRealPrice(symbol, Value);
	}

}
