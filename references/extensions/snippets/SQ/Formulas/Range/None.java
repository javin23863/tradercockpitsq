package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=100, name="None", formula="Range", noneValue=true)
public class None extends FormulaBlock {

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		return Order.NOT_DEFINED;
	}

}
