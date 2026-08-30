package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="HMA changes direction upwards", display="HMA(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = ReturnTypes.Boolean)
@Help("Is triggered if HMA changes direction upwards")
@OppositeBlock("HMAChangesDown")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=240")
@ParameterSet(set="Period=12,ComputedFrom=0")
@ParameterSet(set="Period=24,ComputedFrom=0")
@ParameterSet(set="Period=48,ComputedFrom=0")
@ParameterSet(set="Period=120,ComputedFrom=0")
@ParameterSet(set="Period=240,ComputedFrom=0")

public class HMAChangesUP extends ConditionBlock {
	
	@Parameter
    public DataSeries Input;

    @Parameter(defaultValue="10", isPeriod=true, minValue=5, maxValue=252, step=1)
    public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		HullMovingAverage indicator = Strategy.Indicators.HullMovingAverage(Input, Period);
		double value1 = indicator.Value.getRounded(Shift + 2);
		double value2 = indicator.Value.getRounded(Shift + 1);
		double value3 = indicator.Value.getRounded(Shift);
		
		return (value1 > value2) && (value2 < value3);
	}

}