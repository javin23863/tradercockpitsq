package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=500, name="Range in pips", formula="Range")
@IgnoreInBuilder
public class PriceRange extends FormulaBlock {

	@Parameter(postfix="pips")
	public IBlock Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		return Value.evaluateBlock();
	}

}
