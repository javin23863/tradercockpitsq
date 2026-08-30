package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=500, name="Range in pips", formula="RangeLevel")
@IgnoreInBuilder
public class PriceRange extends FormulaBlock {

	@Parameter(postfix="pips")
	public IBlock Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		double valueInPips = Value.evaluateBlock();
		
		double valueInRealPrice = strategy.convertPipsToRealPrice(symbol, valueInPips);

		return getLevelByDirection(direction, price, valueInRealPrice);
	}

	//------------------------------------------------------------------------

	private double getLevelByDirection(int direction, double openPrice, double valueInRealPrice) {
		if(direction == Directions.Long) {
			// long order
			return openPrice - valueInRealPrice;

		} else {
			// short order
			return openPrice + valueInRealPrice;
		}
	}	
}
