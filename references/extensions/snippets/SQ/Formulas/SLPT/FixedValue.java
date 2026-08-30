package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=300, name="Fixed value (in pips)", formula="SLPT")
public class FixedValue extends FormulaBlock {

	@Parameter(defaultValue="50", minValue=1, builderMinValue=5, builderMaxValue=500, maxValue=9999999, step=1, postfix="pips")
	@SLPTValue(SLPTValues.ValueInPips)
	public double Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		
		// we have to compute SL or PT for this order
		double valueInRealPrice = strategy.convertPipsToRealPrice(symbol, Value);
		
		if(direction > 0) {
			return price + valueInRealPrice;

		} else {
			return price - valueInRealPrice;
		}
	}

}
