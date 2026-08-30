package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=700, name="Price level", formula="RangeLevel")
public class PriceLevel extends FormulaBlock {

	@Parameter
	public IBlock Value;

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		return Value.evaluateBlock();
	}

}
