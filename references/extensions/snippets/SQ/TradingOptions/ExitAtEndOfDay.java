package SQ.TradingOptions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;

public class ExitAtEndOfDay extends TradingOption {
	
	@Parameter(name="Exit At End Of Day", defaultValue="false", category="Trading options")
	@Help("Close all positions at end of day?")
	@ForEngine("*,-SP,-SA")
	public boolean ExitAtEndOfDay;

	@Parameter(name="End Of Day Exit Time", defaultValue="2355", category="Trading options")
	@Help("End Of Day Time when to close all positions. If set to 0, it will close all positions from previous day at midnight.\nTime is in timezone of the data.")
	@Editor(type=Editors.Time)	
	@Activator(param="ExitAtEndOfDay")
	@ForEngine("*,-SP,-SA")
	public int EODExitTime;

	private long dailyEODExitTime = -1;	
	private long EODTime = -1;
	private boolean closedThisDay = false;

	private long previousTickTime = -1;

	private boolean LimitTimeRangeChecked = false;
	private int SignalTimeRangeFrom = 0;
	private int SignalTimeRangeTo = 0;
	private long dailySignalTimeRangeFrom = 0;
	private long dailySignalTimeRangeTo = 0;
	private boolean LimitTimeRange = false;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return ExitAtEndOfDay;
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws Exception {
		if(!ExitAtEndOfDay) {
			return true;
		}
		long currentTime = Strategy.MarketData.TimeCurrent();

		if (!LimitTimeRangeChecked) {
			LimitTimeRangeChecked = true;
			TradingOption[] tradingOptions = Strategy.getTradingOptions();
			for(int i=0; i<tradingOptions.length; i++) {
				if (tradingOptions[i].getClass().getName().equals("SQ.TradingOptions.LimitTimeRange")) {
					LimitTimeRange tradingOptionsVar = (LimitTimeRange)tradingOptions[i]; 
					if (tradingOptionsVar.LimitTimeRange==true) {
						LimitTimeRange = true;
						SignalTimeRangeFrom = tradingOptionsVar.SignalTimeRangeFrom; 
						SignalTimeRangeTo = tradingOptionsVar.SignalTimeRangeTo;
					}
				}
			}
		}
		
		if(currentTime != 0 && currentTime > dailySignalTimeRangeTo) {
			//it is a new day
			if(LimitTimeRange) initLimitTimeRangeForCurrentDay(currentTime); // Must be calculated first
		}
		
		if(currentTime != 0 && currentTime > EODTime) {
			//it is a new day
            initTimesForCurrentDay(currentTime, Strategy);  
		}

		if(!LimitTimeRange && currentTime >= dailyEODExitTime) {
			// returning false means there will be no more processing on this tick
	        // this is what we want because we don't want to be trading after close of all positions
		    return(false); 
		}
		if(LimitTimeRange) {
			if (dailyEODExitTime < dailySignalTimeRangeFrom) {
				if(currentTime >= dailyEODExitTime && currentTime < dailySignalTimeRangeFrom) {
				    return(false); 
				}
			}else if (dailyEODExitTime >= dailySignalTimeRangeFrom && SignalTimeRangeFrom < SignalTimeRangeTo) {
				if(currentTime >= dailyEODExitTime) {
				    return(false); 
				}
			}
		}
	
		return true;
	}
	
	//------------------------------------------------------------------------

	@Override
	public void OnTick(StrategyBase Strategy, TickEvent tick, boolean includingPendingOrders) throws Exception {
		if(!ExitAtEndOfDay) {
			return;
		}
		
		long currentTime = tick.getTime();
		if(!closedThisDay) {
			if (!LimitTimeRange && currentTime >= dailyEODExitTime) {
				// time is over EOD closing time, we should close the positions
				if(Engines.isTradestationEngine(Strategy.getEngine())) {
					includingPendingOrders = false;
				}
				closedThisDay = closeAllPositions(Strategy, includingPendingOrders, currentTime, tick);
			}
			if (LimitTimeRange) {
				if (dailyEODExitTime < dailySignalTimeRangeFrom) {
					if(currentTime >= dailyEODExitTime && currentTime < dailySignalTimeRangeFrom) {
						// time is over EOD closing time, we should close the positions
						if(Engines.isTradestationEngine(Strategy.getEngine())) {
							includingPendingOrders = false;
						}
						closedThisDay = closeAllPositions(Strategy, includingPendingOrders, currentTime, tick);
					}
				}else if (dailyEODExitTime >= dailySignalTimeRangeFrom && SignalTimeRangeFrom < SignalTimeRangeTo) {
					if(currentTime >= dailyEODExitTime) {
						// time is over EOD closing time, we should close the positions
						if(Engines.isTradestationEngine(Strategy.getEngine())) {
							includingPendingOrders = false;
						}
						closedThisDay = closeAllPositions(Strategy, includingPendingOrders, currentTime, tick);
					}
				}
			}
		}
		
		previousTickTime  = currentTime;		
	}	

	//------------------------------------------------------------------------

	private void initTimesForCurrentDay(long currentTime, StrategyBase Strategy) {
		// set end time of the current day (so that we now when new day starts)
		EODTime = SQTime.correctDayEndMT(currentTime);

		// set time of EOD 
		if(EODExitTime == 0){
			dailyEODExitTime = EODTime;
		}
		else {
			if(LimitTimeRange && SignalTimeRangeFrom >= SignalTimeRangeTo) {
				dailyEODExitTime = SQTime.setHHMM(dailySignalTimeRangeTo, EODExitTime);	
				dailyEODExitTime = SQTime.setSecond(dailyEODExitTime, 0);		
				dailyEODExitTime = SQTime.setMiliSeconds(dailyEODExitTime, 0);	
			}else {
				dailyEODExitTime = SQTime.setHHMM(currentTime, EODExitTime);	
				dailyEODExitTime = SQTime.setSecond(dailyEODExitTime, 0);		
				dailyEODExitTime = SQTime.setMiliSeconds(dailyEODExitTime, 0);	
			}
		}
		closedThisDay = false;
	}
	
	//------------------------------------------------------------------------

	private void initLimitTimeRangeForCurrentDay(long currentTime) {
		// set time of range open 

		dailySignalTimeRangeFrom = SQTime.setHHMM(currentTime, SignalTimeRangeFrom);
		dailySignalTimeRangeFrom = SQTime.setSecond(dailySignalTimeRangeFrom, 0);
		dailySignalTimeRangeFrom = SQTime.setMiliSeconds(dailySignalTimeRangeFrom, 0);
		
		dailySignalTimeRangeTo = SQTime.setHHMM(currentTime, SignalTimeRangeTo);
		dailySignalTimeRangeTo = SQTime.setSecond(dailySignalTimeRangeTo, 0);
		dailySignalTimeRangeTo = SQTime.setMiliSeconds(dailySignalTimeRangeTo, 0);
		
		if(SignalTimeRangeFrom >= SignalTimeRangeTo) dailySignalTimeRangeTo = SQTime.addDays(dailySignalTimeRangeTo, 1);
	}

	//------------------------------------------------------------------------

	private boolean closeAllPositions(StrategyBase Strategy, boolean includingPendingOrders, long currentTime, TickEvent tick) throws TradingException {

		long currentTimeDayStart = -1;
		long currentTimeDayEnd = -1;
		boolean someClosed = false;
		
		int total = Strategy.Trader.getOpenOrdersCount(false);
		for(int i=total-1; i>=0; i--) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
			
			if(!includingPendingOrders && order.isPendingOrder()) {
				continue;
			}
			
			if(order.getStrategyName().equals(Strategy.getStrategyName())) {
				if(currentTimeDayStart == -1) {
					// day start wasn't yet computed, compute it
					currentTimeDayStart = SQTime.correctDayStart(currentTime);
					currentTimeDayEnd = SQTime.correctDayEndMT(currentTime);
				}
				
				if(Engines.isTradestationEngine(Strategy.getEngine())) {

					if(currentTimeDayEnd == EODTime) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						if(tick.isBarClose()) {
							if(order.getOpenTime() != previousTickTime || SQTime.getHHMM(order.getOpenTime()) != EODExitTime) {
								//((LiveOrderObj) order).setWaitingBars(1);
							}
							order.Close(OrderCloseTypes.EOD_TIME);
							someClosed = true;
						}
						
					} else if(order.getOpenTime() < currentTimeDayStart) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						order.Close(OrderCloseTypes.EOD);
						someClosed = true;
					}
				
				} else {

					if(currentTimeDayEnd == EODTime) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						order.Close(OrderCloseTypes.EOD_TIME);
						someClosed = true;
						
					} else if(order.getOpenTime() < currentTimeDayStart) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						order.Close(OrderCloseTypes.EOD);
						someClosed = true;
					}
				}
			}			
		}
		
		return someClosed;
	}
	
	
	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		ExitAtEndOfDay option = new ExitAtEndOfDay();
		option.ExitAtEndOfDay = this.ExitAtEndOfDay;
		option.EODExitTime = this.EODExitTime;
		
		return option;
	}
}
