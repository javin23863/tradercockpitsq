package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="First week of month", display="FirstWeekOfMonth", returnType = ReturnTypes.Boolean)
@SortOrder(100)
@NoShift
@ForEngine("SP,SA")
public class FirstWeekOfMonth extends ConditionBlock {
		
	@Parameter
	public ChartData Chart;

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		long time;
		
		if(Strategy.isStockpicker()) {
			time = Strategy.Stockpicker.TimeCurrent();
		} else {
			time = Chart.Time();
		}
		
		return SQTime.getWeekOfMonth(time) == 1;
	}
}
