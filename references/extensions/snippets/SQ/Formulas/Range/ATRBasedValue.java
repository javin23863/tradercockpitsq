package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;
import SQ.Internal.Strategy;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.talib.TALibIndicator;

@Formula(order=300, name="ATR-based value", formula="Range")
public class ATRBasedValue extends FormulaBlock {

	@Parameter(defaultValue="1", minValue=0.01, builderMinValue=1, builderMaxValue=5, maxValue=9999, step=0.1, postfix="* ATR(")
	public double Value;
		
	@Parameter(defaultValue="20", minValue=1, maxValue=9999, step=5, postfix=")")
	public int AtrPeriod;	

	private int cachedBars = -1;
	private StrategyBase cachedStrategy = null;
	private double cachedATR;
	
	private SettingsMap params = new SettingsMap();
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		if(Value == 0) return 0;
		
		double atr = 0;
		
		if(Strategy.isStockpicker()) {			
			params.set("TimePeriod", AtrPeriod);
			
			int line = 0;
			
			float[][] outputs = Strategy.Stockpicker.TALibIndicators.calculate("ATR", params, Strategy, 1, line, null);
			if(outputs != null) {
				int index = Strategy.Stockpicker.getCurrentBar(0);
				atr = outputs[line][index>0 ? (index - 1) : index];
			}
		} else {
			ChartData chartData = strategy.MarketData.Chart(symbol);
			
			atr = getCachedATR(chartData, strategy);
		}
		
		double value = Value * atr;
		
		return SQUtils.round(value, 5);
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
