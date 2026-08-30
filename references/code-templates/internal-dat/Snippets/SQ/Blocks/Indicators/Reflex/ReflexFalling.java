package SQ.Blocks.Indicators.Reflex;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(RFF) Reflex is Falling", display="Reflex(@Chart@#Period#)[#Shift#] is falling", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Reflex is falling 2 bars")
@OppositeBlock("ReflexRising")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=36")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=240")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=200")
public class ReflexFalling extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="24", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Reflex indicator = Strategy.Indicators.Reflex(Chart, Period);

		double prevValue = indicator.Value.getRounded(Shift + 1);
		double curValue = indicator.Value.getRounded(Shift);
		
		return (curValue < prevValue);
	}

}