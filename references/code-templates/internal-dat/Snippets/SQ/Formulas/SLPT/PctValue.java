package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=300, name="Percent value", formula="SLPT")
public class PctValue extends FormulaBlock {

	@Parameter(defaultValue="5", minValue=1, builderMinValue=5, builderMaxValue=50, maxValue=99, step=0.1, postfix="%")
	@SLPTValue(SLPTValues.ValueInPct)
	public double Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		
		// we have to compute SL or PT for this order
		double pctValue = (price / 100d) * Value;
				
		if(direction > 0) {
			return price + pctValue;

		} else {
			return price - pctValue;
		}
	}

}
