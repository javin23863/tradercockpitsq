package SQ.Blocks.Indicators.ROC;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ROCCAL) ROC crosses above Level", display="ROC(@Chart@#Period#)[#Shift#] crosses above #Level#", returnType = ReturnTypes.Boolean)
@Help("Is triggered if ROC is above Level")
@OppositeBlock(value="ROCCrossesBelowLevel",oscillator=true, middleValue=0, field="Level")
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
public class ROCCrossesAboveLevel extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter(defaultValue="0", minValue=-100, maxValue=100, step=0.001)
	public double Level;

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

		return (curVal > Level) && (prevVal < Level);
	}

}