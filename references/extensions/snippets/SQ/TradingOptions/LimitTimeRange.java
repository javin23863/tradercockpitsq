package SQ.TradingOptions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;

public class LimitTimeRange extends TradingOption {
	
	@Parameter(name="Limit Time Range", defaultValue="false", category="Trading options")
	@Help("Limit trading to given time range?")
	@ForEngine("*,-SP,-SA")
	public boolean LimitTimeRange;

	@Parameter(name="Time Range From", defaultValue="0800", category="Trading options")
	@Help("Time range for taking signal - from time.\nTime is in timezone of the data.")
	@Editor(type=Editors.Time)	
	@Activator(param="LimitTimeRange")
	@ForEngine("*,-SP,-SA")
	public int SignalTimeRangeFrom;
	
	@Parameter(name="Time Range To", defaultValue="1600", category="Trading options")
	@Help("Time range for taking signal - to time.\nTime is in timezone of the data.")
	@Editor(type=Editors.Time)	
	@Activator(param="LimitTimeRange")
	@ForEngine("*,-SP,-SA")
	public int SignalTimeRangeTo;

	@Parameter(name="Exit At End Of Range", defaultValue="false", category="Trading options")
	@Help("Close all positions at end of range?")
	@Activator(param="LimitTimeRange")
	@ForEngine("*,-SP,-SA")
	public boolean ExitAtEndOfRange;

	@Parameter(name="Order Types To Close", defaultValue="0", category="Trading options")
	@Help("Type of orders to be closed at end of range - by default all (live & pending) are closed. Works only if Exit At End Of Range = true.")
	@Editor(type=Editors.Selection, values="All=0,Live only=1,Pending only=2")
	@Activator(param="LimitTimeRange")
	@ForEngine("*,-SP,-SA")
	public int OrderTypeToExit;

	private long dailySignalTimeRangeFrom;
	private long dailySignalTimeRangeTo;
	
	private boolean closedThisDay = false;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return LimitTimeRange;
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws TradingException {
		if(!LimitTimeRange) {
			return true;
		}
		long currentTime = Strategy.MarketData.TimeCurrent();

		long millisGap = 0;

		if(currentTime > dailySignalTimeRangeTo) {
			// it is new day
			initTimesForCurrentDay(currentTime);
		}

		if((currentTime + millisGap) < dailySignalTimeRangeFrom || (currentTime + millisGap) >= dailySignalTimeRangeTo) {
			// time is outside given range
			// returning false means there will be no more processing on this tick
			// this is what we want because we don't want to be trading outside of this time range
			return false; 
		}

		return true;
	}

	//------------------------------------------------------------------------

	@Override
	public void OnTick(StrategyBase Strategy, TickEvent tick, boolean includingPendingOrders) throws Exception {
		if(!LimitTimeRange) {
			return;
		}

		long currentTime = tick.getTime();

		if(ExitAtEndOfRange) {
			if(!Strategy.isTradestationEngine() && !closedThisDay && currentTime >= dailySignalTimeRangeTo) { 
				closeAllPositions(Strategy, tick);
				closedThisDay = true;
			}
			if(Strategy.isTradestationEngine() && !closedThisDay && tick.isBarClose() && currentTime >= dailySignalTimeRangeTo) { 
				closeAllPositions(Strategy, tick);
				closedThisDay = true;
			}
		}
	}	

	//------------------------------------------------------------------------

	private void initTimesForCurrentDay(long currentTime) {
		// set time of range open 
		dailySignalTimeRangeFrom = SQTime.setHHMM(currentTime, SignalTimeRangeFrom);
		dailySignalTimeRangeFrom = SQTime.setSecond(dailySignalTimeRangeFrom, 0);
		dailySignalTimeRangeFrom = SQTime.setMiliSeconds(dailySignalTimeRangeFrom, 0);
		
		dailySignalTimeRangeTo = SQTime.setHHMM(currentTime, SignalTimeRangeTo);
		dailySignalTimeRangeTo = SQTime.setSecond(dailySignalTimeRangeTo, 0);
		dailySignalTimeRangeTo = SQTime.setMiliSeconds(dailySignalTimeRangeTo, 0);
		
		if(SignalTimeRangeFrom >= SignalTimeRangeTo) {
			if(SQTime.getHHMM(currentTime) < SignalTimeRangeTo) {
				dailySignalTimeRangeFrom = SQTime.addDays(dailySignalTimeRangeFrom, -1);
			}
			else {
				dailySignalTimeRangeTo = SQTime.addDays(dailySignalTimeRangeTo, 1);
			}
		}
		else {
			if(currentTime > dailySignalTimeRangeTo) {
				dailySignalTimeRangeFrom = SQTime.addDays(dailySignalTimeRangeFrom, 1);
				dailySignalTimeRangeTo = SQTime.addDays(dailySignalTimeRangeTo, 1);
			}
		}
		
		closedThisDay = false;
	}

	//------------------------------------------------------------------------

	private void closeAllPositions(StrategyBase Strategy, TickEvent tick) throws TradingException {
		int total = Strategy.Trader.getOpenOrdersCount(false);
		for(int i=total-1; i>=0; i--) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
			
			if(OrderTypeToExit == 1) {
				// live only
				if(order.isPendingOrder()) continue;
			}
			if(OrderTypeToExit == 2) {
				// pending only
				if(!order.isPendingOrder()) continue;
			}

			if(Engines.isTradestationEngine(Strategy.getEngine())) {
				if(tick.isBarClose()) {
					if(order.getStrategyName().equals(Strategy.getStrategyName())) {
						order.Close(OrderCloseTypes.EOR);
					}
				}
			} else {
				if(order.getStrategyName().equals(Strategy.getStrategyName())) {
					order.Close(OrderCloseTypes.EOR);
				}
			}
		}

	}
	
	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		LimitTimeRange option = new LimitTimeRange();
		option.LimitTimeRange = this.LimitTimeRange;
		option.SignalTimeRangeFrom = this.SignalTimeRangeFrom;
		option.SignalTimeRangeTo = this.SignalTimeRangeTo;
		option.ExitAtEndOfRange = this.ExitAtEndOfRange;
		option.OrderTypeToExit = this.OrderTypeToExit;
		
		return option;
	}	
	
}
