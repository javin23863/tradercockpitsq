package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Last week of month", display="LastWeekOfMonth", returnType = ReturnTypes.Boolean)
@SortOrder(100)
@NoShift
@ForEngine("SP,SA")
public class LastWeekOfMonth extends ConditionBlock {
		
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
		
		long lastMonthDay = SQTime.setDayOfMonth(time, SQTime.getDaysInMonth(time));
		
		return SQTime.getWeekOfMonth(time) == SQTime.getWeekOfMonth(lastMonthDay);
	}
}
