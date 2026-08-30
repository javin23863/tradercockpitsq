package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CAVWAP) Close is above VWAP ", display="Close is above VWAP(@Chart@#VWAPPeriod#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Close is above VWAP")
@OppositeBlock("CloseBelowVWAP")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=80")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=96")
@ParameterSet(set="Period=120")
public class CloseAboveVWAP extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="10", minValue=2, maxValue=480, step=1)
	public int VWAPPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		VWAP indicator = Strategy.Indicators.VWAP(Chart, VWAPPeriod);

		double curVal = indicator.Value.getRounded(Shift);
		double close = Chart.Close.get(Shift);
		
		return (curVal < close);
	}

}