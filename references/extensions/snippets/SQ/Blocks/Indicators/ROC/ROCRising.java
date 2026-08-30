package SQ.Blocks.Indicators.ROC;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ROCR) ROC is rising", display="ROC(@Chart@#Period#)[#Shift#] is rising ", returnType = ReturnTypes.Boolean)
@Help("Is triggered if ROC is rising")
@OppositeBlock("ROCFalling")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
@ParameterSet(set="Period=60")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=96")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=100")
public class ROCRising extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)

	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		ROC indicator = Strategy.Indicators.ROC(Chart, Period);
		double curVal = indicator.Value.getRounded(Shift);
		double prevVal = indicator.Value.getRounded(Shift + 1);
		
		
		return (curVal > prevVal);
	}

}