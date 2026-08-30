package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(DVWAPU) Faster VWAP is above Slower VWAP", display="Faster VWAP(@Chart@#FastPeriod#)[#Shift#] is above Slower VWAP(@Chart@#SlowPeriod#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Faster VWAP is above Slower VWAP")
@MT5ExtendedTemplate
@OppositeBlock("FastVWAPBelowSlowVWAP")
@ParameterSet(set="FastPeriod=10,SlowPeriod=20")
@ParameterSet(set="FastPeriod=20,SlowPeriod=40")
@ParameterSet(set="FastPeriod=40,SlowPeriod=80")
@ParameterSet(set="FastPeriod=100,SlowPeriod=200")
@ParameterSet(set="FastPeriod=12,SlowPeriod=24")
@ParameterSet(set="FastPeriod=24,SlowPeriod=48")
@ParameterSet(set="FastPeriod=48,SlowPeriod=96")
@ParameterSet(set="FastPeriod=120,SlowPeriod=240")
public class FastVWAPAboveSlowVWAP extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="10", minValue=2, maxValue=480, step=1)
	public int FastPeriod;

	@Parameter(defaultValue="20", minValue=2, maxValue=480, step=1)
	public int SlowPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		VWAP fastIndicator = Strategy.Indicators.VWAP(Chart, FastPeriod);
		VWAP slowIndicator = Strategy.Indicators.VWAP(Chart, SlowPeriod);

		double fastVal = fastIndicator.Value.getRounded(Shift);
		double slowVal = slowIndicator.Value.getRounded(Shift);
		
		return (fastVal > slowVal);
	}

}