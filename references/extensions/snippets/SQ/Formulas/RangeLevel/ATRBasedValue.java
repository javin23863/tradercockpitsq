package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;
import SQ.Internal.Strategy;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=300, name="ATR-based value", formula="RangeLevel")
public class ATRBasedValue extends FormulaBlock {

	@Parameter(defaultValue="1", minValue=0.01, builderMinValue=1, builderMaxValue=5, maxValue=9999, step=0.1, postfix="* ATR(")
	@SLPTValue(SLPTValues.ATRMultiple)
	public double Value;
		
	@Parameter(defaultValue="20", minValue=1, maxValue=9999, step=5, postfix=")")
	@SLPTValue(SLPTValues.ATRPeriod)
	public int AtrPeriod;

	private int cachedBars = -1;
	private StrategyBase cachedStrategy = null;
	private double cachedATR;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		if(Value == 0) return Order.NOT_DEFINED;
		
		ChartData chartData = strategy.MarketData.Chart(symbol);
		
		double atr = SQUtils.round(getCachedATR(chartData, strategy), 6);
		double valueInRealPrice = Value * atr;

		if(direction == Directions.Long) {
			// long order
			return SQUtils.round((price - valueInRealPrice), 5);

		} else {
			// short order
			return SQUtils.round((price + valueInRealPrice), 5);
		}
	}

	//------------------------------------------------------------------------

	private double getCachedATR(ChartData chartData, StrategyBase strategy) throws TradingException {
		int currentBar = chartData.Bars();
		
		if(currentBar != cachedBars || cachedStrategy != strategy) {
			cachedBars = currentBar;
			cachedStrategy = strategy;
			
			cachedATR = SQUtils.round6(strategy.getATRValue(chartData, AtrPeriod, 1));
		}
		
		return cachedATR;
	}
	

}
