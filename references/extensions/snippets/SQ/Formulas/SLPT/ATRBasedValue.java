package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.talib.TALibIndicator;

@Formula(order=400, name="ATR-based value", formula="SLPT")
public class ATRBasedValue extends FormulaBlock {

	@Parameter(defaultValue="1", minValue=0.01, builderMinValue=1, builderMaxValue=5, maxValue=9999, step=0.1, postfix="* ATR(")
	@SLPTValue(SLPTValues.ATRMultiple)
	public double Value;
		
	@Parameter(defaultValue="20", minValue=1, maxValue=9999, step=5, postfix=")")
	@SLPTValue(SLPTValues.ATRPeriod)
	public int AtrPeriod;
	
	private SettingsMap params = new SettingsMap();
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
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
			// we have to compute SL or PT for this order
			ChartData chartData = strategy.MarketData.Chart(symbol);
		
			atr = strategy.getATRValue(chartData, AtrPeriod, 1);		
		}
		
		double valueInRealPrice = Value * SQUtils.round(atr, 6);		
		
		if(direction > 0) {
			return price + valueInRealPrice;

		} else {
			return price - valueInRealPrice;
		}		
	}



}
