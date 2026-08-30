package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock( returnType=ReturnTypes.Number, display="DayOfMonth[#Shift#]")
@Help("Returns day of the month (1-31)")
@SortOrder(400)
public class BarDayOfMonth extends ValueBlock {

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
		
		return SQTime.getDay(time);
	}

}