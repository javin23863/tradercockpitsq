package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock( returnType=ReturnTypes.Number, display="WeekOfMonth[#Shift#]")
@Help("Returns week of the month (1-6)")
@SortOrder(400)
@ForEngine("SP,SA,MT4,MT5,TS,MC")
public class BarWeekOfMonth extends ValueBlock {

	@Parameter
	public ChartData Chart;
	
	@Parameter
	public int Shift;
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		long time;
		
		if(Strategy.isStockpicker()) {
			time = Strategy.Stockpicker.data.TimeD(relativeShift + Shift);
		} else {
			time = Chart.Time(relativeShift+Shift);
		}
		
		return SQTime.getWeekOfMonth(time);
	}

}