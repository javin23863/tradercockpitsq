package SQ.TradingOptions;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;

public class ExitOnFriday extends TradingOption {
	
	@Parameter(name="Exit On Friday", defaultValue="false", category="Trading options")
	@Help("Close all positions at end of the week (Friday)?")
	@ForEngine("*,-SP,-SA")
	public boolean ExitOnFriday;
	
	@Parameter(name="Friday Exit Time", defaultValue="2300", category="Trading options")
	@Help("Time on Friday when to close all positions.\nTime is in timezone of the data.")
	@Editor(type=Editors.Time)	
	@Activator(param="ExitOnFriday")
	@ForEngine("*,-SP,-SA")
	public int FridayExitTime;

	private long thisFridayExitTime = -1;
	private long thisSundayBeginTime = -1;
	private boolean closedThisWeek = false;
	private long EOFDayTime;
	
	private long previousTickTime = -1;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return ExitOnFriday;
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws TradingException {
		if(!ExitOnFriday) {
			return true;
		}
		
		long currentTime = Strategy.MarketData.TimeCurrent();
		
		if(thisFridayExitTime == -1) {
			initFridayExitTime(currentTime, false, Strategy);
		}
		
		if(currentTime < thisFridayExitTime) {
			// trade normally
			return true;
		}
		
		if(currentTime < thisSundayBeginTime) {
			// do not allow opening new positions until sunday.
			// returning false means there will be no more processing on this tick.
			// this is what we want because we don't want to be trading after close of all positions
			return false;
			
		} else {
			// new week starting
			initFridayExitTime(currentTime, true, Strategy);
			return true;
		}
	}

	//------------------------------------------------------------------------

	@Override
	public void OnTick(StrategyBase Strategy, TickEvent tick, boolean includingPendingOrders) throws Exception {
		if(!ExitOnFriday) {
			return;
		}
		
		long currentTime = tick.getTime();

		if(!closedThisWeek && thisFridayExitTime != -1 && currentTime >= thisFridayExitTime && currentTime < thisSundayBeginTime) {
			if(Engines.isTradestationEngine(Strategy.getEngine())) {
				includingPendingOrders = false;
			}
			
			closedThisWeek = closeAllPositions(Strategy, includingPendingOrders, currentTime, tick);
		}
		
		previousTickTime = currentTime;
	}	

	//------------------------------------------------------------------------

	private void initFridayExitTime(long currentTime, boolean addDays, StrategyBase Strategy) {
		// get exact exit time this friday as long number for quick comparison

		if(addDays) {
			int dowNow = SQTime.getDayOfWeek(currentTime);
			thisFridayExitTime = SQTime.addDays(currentTime, dowNow == 7 ? 1 : 0);
		} else {
			thisFridayExitTime = currentTime;
		}

		// set time of EOD 
		thisFridayExitTime = SQTime.setDayOfWeek(thisFridayExitTime, FridayExitTime == 0 ? SQTime.SATURDAY : SQTime.FRIDAY);
		
		thisFridayExitTime = SQTime.setHHMM(thisFridayExitTime, FridayExitTime);	
		thisFridayExitTime = SQTime.setSecond(thisFridayExitTime, 0);		
		thisFridayExitTime = SQTime.setMiliSeconds(thisFridayExitTime, 0);	

		EOFDayTime = SQTime.correctDayEndMT(thisFridayExitTime);
		
		// set time of beginning of Sunday 
		thisSundayBeginTime = SQTime.setDayOfWeek(thisFridayExitTime, SQTime.SUNDAY);
		thisSundayBeginTime = SQTime.setHHMM(thisSundayBeginTime, 0);	
		thisSundayBeginTime = SQTime.setSecond(thisSundayBeginTime, 0);		
		thisSundayBeginTime = SQTime.setMiliSeconds(thisSundayBeginTime, 0);	
		
		
		closedThisWeek = false;
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
					
					int previousDow = SQTime.getDayOfWeek(previousTickTime);
					
					if(currentTimeDayEnd == EOFDayTime) {
						// we are closing trades on Friday after specified time
						if(tick.isBarClose()) {

							if(order.getOpenTime() == previousTickTime && SQTime.getHHMM(order.getOpenTime()) == FridayExitTime) {

							} else {
								//((LiveOrderObj) order).setWaitingBars(1);
							}
							order.Close(OrderCloseTypes.EOF_TIME);

							someClosed = true;
						}
						
					} else if((order.getOpenTime() < currentTimeDayStart && previousDow == SQTime.FRIDAY)) {
						// we are closing trades on Friday 
						if(previousTickTime == currentTime) {
							// special handling if there is gap in data 
							if(currentTime >= currentTimeDayEnd) {
								order.Close(OrderCloseTypes.EOF);
								someClosed = true;
							}
							
						} else {
							order.Close(OrderCloseTypes.EOF);
							someClosed = true;
							
						}
					}
					
				} else {
					if(currentTimeDayEnd == EOFDayTime) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						order.Close(OrderCloseTypes.EOF_TIME);
						someClosed = true;
						
					} else if(order.getOpenTime() < currentTimeDayStart) {
						// we are either closing trades on Friday, 
						// or we are closing at the first tick of a new day AND order wasn't opened today (at this new day) 
						order.Close(OrderCloseTypes.EOF);
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
		ExitOnFriday option = new ExitOnFriday();
		option.ExitOnFriday = this.ExitOnFriday;
		option.FridayExitTime = this.FridayExitTime;
		
		return option;
	}
	
}
