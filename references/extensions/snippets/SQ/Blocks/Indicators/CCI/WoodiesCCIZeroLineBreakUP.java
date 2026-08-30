package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ZBU) CCI Zero Line Break UP", display="CCI(@Chart@#Period#)[#Shift#] Zero Line Break UP", returnType = ReturnTypes.Boolean)
@Help("Is triggered if CCI cross zero line UP")
@OppositeBlock("WoodiesCCIZeroLineBreakDown")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
public class WoodiesCCIZeroLineBreakUP extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		CCI indicator = Strategy.Indicators.CCI(Input, Period);
		
		double cci1 = indicator.Value.getRounded(Shift + 1);
		double currCCI = indicator.Value.getRounded(Shift);

		
		return (cci1<0 && currCCI>0);
	}

}