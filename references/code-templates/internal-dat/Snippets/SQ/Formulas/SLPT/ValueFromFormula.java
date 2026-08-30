package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=600, name="Value from formula (in pips)", formula="SLPT")
@IgnoreInBuilder
public class ValueFromFormula extends FormulaBlock {

	@Parameter
	public IBlock Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		double valueInPips = Value.evaluateBlock();
		
		double valueInRealPrice = strategy.convertPipsToRealPrice(symbol, valueInPips);

		return getLevelByDirection(direction, direction, price, valueInRealPrice);
	}

	//------------------------------------------------------------------------

	private double getLevelByDirection(int slptType, int direction, double openPrice, double valueInRealPrice) {
		return direction == Directions.Long ? openPrice + valueInRealPrice : openPrice - valueInRealPrice;
		/*
		if(direction == Directions.Long) {
			// long order
			return (slptType == 1 ? openPrice + valueInRealPrice : openPrice - valueInRealPrice);
		} else {
			// short order
			return (slptType == 1 ? openPrice - valueInRealPrice : openPrice + valueInRealPrice);
		}
		*/
	}	
}
