package SQ.Formulas.CloseSize;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=100, name="Full position", formula="CloseSize")
public class FullPosition extends FormulaBlock {

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		return 0;
	}

}
