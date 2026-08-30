package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="HMA is rising", display="HMA(@Chart@#Period#)[#Shift#] is rising", returnType = ReturnTypes.Boolean)
@Help("Is triggered if HMA is rising 2 bars")
@OppositeBlock("HMAFalling")
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
public class HMARising extends ConditionBlock {
	
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
		double value1 = indicator.Value.getRounded(Shift + 1);
		double value2 = indicator.Value.getRounded(Shift);
		
		return value1 < value2;
	}

}