package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Is month last trading day", display="IsMonthLastTradingDay", returnType = ReturnTypes.Boolean)
@SortOrder(100)
@NoShift
public class IsMonthLastTradingDay extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="false", showIfDefault=false)
	public boolean IncludeWeekends;
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		long time;
		
		if(Strategy.isStockpicker()) {
			time = Strategy.Stockpicker.TimeCurrent();
		} else {
			time = Chart.Time();
		}
		
		int daysInMonth = SQTime.getDaysInMonth(time);
		long lastTradingDate = SQTime.setDayOfMonth(time, daysInMonth);

		if(!IncludeWeekends) {
			if(SQTime.getDayOfWeek(lastTradingDate) == SQTime.SATURDAY) {
				lastTradingDate = SQTime.addDays(lastTradingDate, -1);
			
			} else if(SQTime.getDayOfWeek(lastTradingDate) == SQTime.SUNDAY) {
				lastTradingDate = SQTime.addDays(lastTradingDate, -2);
			}
		}
		
		return SQTime.getDay(time) == SQTime.getDay(lastTradingDate);
	}
}
