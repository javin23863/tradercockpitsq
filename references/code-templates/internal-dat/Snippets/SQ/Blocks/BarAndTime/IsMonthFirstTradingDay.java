package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Is month first trading day", display="IsMonthFirstTradingDay", returnType = ReturnTypes.Boolean)
@SortOrder(100)
@NoShift
public class IsMonthFirstTradingDay extends ConditionBlock {
	
	private long monthFirstTradingDay = -1;
	
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
		
		long date = SQTime.getDateInMs(time);
		
		if(monthFirstTradingDay == -1) {
			monthFirstTradingDay = date;
		}

		if(SQTime.getMonth(monthFirstTradingDay)!=SQTime.getMonth(date)) {
			if(!IncludeWeekends) {
				if(SQTime.getDayOfWeek(date) != SQTime.SATURDAY && SQTime.getDayOfWeek(date) != SQTime.SUNDAY) {
					monthFirstTradingDay = date;
				}
			} else {
				monthFirstTradingDay = date;
			}
		}

		return monthFirstTradingDay==date;
	}
}
