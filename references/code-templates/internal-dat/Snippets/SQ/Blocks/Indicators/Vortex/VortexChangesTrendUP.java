package SQ.Blocks.Indicators.Vortex;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(VCU) Vortex Changes Trend UP", display="Vortex(@Chart@#Period#)[#Shift#] Changes Trend UP", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Vortex changes direction upwards")
@OppositeBlock("VortexChangesTrendDown")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=120")
public class VortexChangesTrendUP extends ConditionBlock {
	
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
		Vortex indicator = Strategy.Indicators.Vortex(Chart, Period);
		double valuePlusCurrent = indicator.VIPlusSumRge.getRounded(Shift);
		double valuePlusPrev = indicator.VIPlusSumRge.getRounded(Shift+1);
		double valueMinusCurrent = indicator.VIMinusSumRge.getRounded(Shift);
		double valueMinusPrev = indicator.VIMinusSumRge.getRounded(Shift+1);

		return (valuePlusCurrent>valueMinusCurrent &&valuePlusPrev<valueMinusPrev);
	}

}