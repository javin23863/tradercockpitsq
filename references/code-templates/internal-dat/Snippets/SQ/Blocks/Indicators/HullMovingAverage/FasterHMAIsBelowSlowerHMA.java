package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(FBS) Fast HMA Is Below Slow HMA", display="Fast HMA(@Chart@#FasterPeriod#)[#Shift#] Is Below Slow HMA(@Chart@#SlowerPeriod#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Fast HMA Is below Slow HMA")
@OppositeBlock("FasterHMAIsAboveSlowerHMA")
@ParameterSet(set="FasterPeriod=12,SlowerPeriod=24,ComputedFrom=0")
@ParameterSet(set="FasterPeriod=24,SlowerPeriod=48,ComputedFrom=0")
@ParameterSet(set="FasterPeriod=12,SlowerPeriod=120,ComputedFrom=0")
@ParameterSet(set="FasterPeriod=10,SlowerPeriod=50,ComputedFrom=0")
@ParameterSet(set="FasterPeriod=5,SlowerPeriod=20,ComputedFrom=0")
@MT5ExtendedTemplate
public class FasterHMAIsBelowSlowerHMA extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="12", minValue=5, maxValue=10000, step=1)
	public int FasterPeriod;

	@Parameter(defaultValue="24", minValue=10, maxValue=10000, step=1)
	public int SlowerPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		HullMovingAverage fasterHMA = Strategy.Indicators.HullMovingAverage(Input, FasterPeriod);
		HullMovingAverage slowerHMA = Strategy.Indicators.HullMovingAverage(Input, SlowerPeriod);
		double value1 = fasterHMA.Value.getRounded(Shift);
		double value2 = slowerHMA.Value.getRounded(Shift);
		
		return (value1 < value2);
	}

}