package SQ.Formulas.Price;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=400, name="Use formula", formula="Price", priceLevel=true)
public class UseFormula extends FormulaBlock {

	@Parameter
	public IBlock Value;
	
	private int isBoolean = -1;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		return Value.evaluateBlock();
	}

	//------------------------------------------------------------------------

	@Override
	public boolean isBooleanValue() {
		checkIsBoolean();
		
		return (isBoolean > 0 ? true : false);
	}

	//------------------------------------------------------------------------

	private void checkIsBoolean() {
		if(isBoolean != -1) {
			return; // already initialized
		}
		
		if(Value.getClass().getCanonicalName().contains("Boolean")) {
			isBoolean = 1;
		} 
		else {
			BuildingBlock bbAnnotation = Value.getClass().getAnnotation(BuildingBlock.class);
			if(bbAnnotation != null && bbAnnotation.returnType() == ReturnTypes.Boolean) {
				isBoolean = 1;
			}
			else {
				isBoolean = 0;
			}
		}
		
	}
	
}
