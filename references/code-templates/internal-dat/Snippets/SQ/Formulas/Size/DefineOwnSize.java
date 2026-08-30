package SQ.Formulas.Size;

import SQ.Internal.MMFormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=100, name="Define own size", formula="Size")
@IgnoreInBuilder
public class DefineOwnSize extends MMFormulaBlock {

	@Parameter(defaultValue="0.1", minValue=0.01, maxValue=999999999)
	public double Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public double computeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl) throws TradingException {
		return Value;
	}

}
