package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

abstract public class IsOneComparisonBlockAbstractPercentil extends ComparisonBlock {
	@Parameter
	public IBlock Indicator;

	@Parameter(name="Bars", defaultValue="10", minValue=2, builderMaxValue=1000, maxValue=1000, category="Properties")
	@Help("Number of bars for calculating the Percentile")
	public int Bars;
	
	@Parameter(category="Properties", builderMinValue=1, builderMaxValue=1)
	public int Shift;
	

	@Parameter(name="Percentile", defaultValue="50", minValue=0.1d, builderMaxValue=99.9d, maxValue=99.9d,category="Properties",step = 0.1d)
	@Help("Percentile Value")
	public double Percentile;


	


}



