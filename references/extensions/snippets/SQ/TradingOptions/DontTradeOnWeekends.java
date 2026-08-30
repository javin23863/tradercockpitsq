package SQ.TradingOptions;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class DontTradeOnWeekends extends TradingOption {
	
	@Parameter(name="Don't trade on weekends", defaultValue="false", category="Trading options")
	@Help("Strategy won't perform any operations during weekends")
	@ForEngine("*,-SP,-SA")
	public boolean DontTradeOnWeekends;
	
	@Parameter(name="Friday Close Time", defaultValue="2300", category="Trading options")
	@Help("From this time on the strategy won't trade.\nTime is in timezone of the data.")
	@Editor(type=Editors.Time)	
	@Activator(param="DontTradeOnWeekends")
	@ForEngine("*,-SP,-SA")
	public int FridayCloseTime;
	
	@Parameter(name="Sunday Open Time", defaultValue="2300", category="Trading options")
	@Help("Strategy reenables trading at this time")
	@Editor(type=Editors.Time)	
	@Activator(param="DontTradeOnWeekends")
	@ForEngine("*,-SP,-SA")
	public int SundayOpenTime;

	private long thisFridayExitTime = -1;
	private long thisSundayBeginTime = -1;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return DontTradeOnWeekends;
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws TradingException {
		if(!DontTradeOnWeekends) {
			return true;
		}
		
		long currentTime = Strategy.MarketData.TimeCurrent();
		
		if(thisFridayExitTime == -1) {
			initTimes(currentTime, false, Strategy);
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
			initTimes(currentTime, true, Strategy);
			return true;
		}
	}

	//------------------------------------------------------------------------

	@Override
	public void OnTick(StrategyBase Strategy, TickEvent tick, boolean includingPendingOrders) throws Exception {
		return;
	}	

	//------------------------------------------------------------------------

	private void initTimes(long currentTime, boolean addDays, StrategyBase Strategy) {
		// get exact exit time this friday as long number for quick comparison

		if(addDays) {
			int dowNow = SQTime.getDayOfWeek(currentTime);
			thisFridayExitTime = SQTime.addDays(currentTime, dowNow == 7 ? 1 : 0);
			thisSundayBeginTime = thisFridayExitTime;
		} 
		else {
			thisFridayExitTime = currentTime;
			thisSundayBeginTime = currentTime;
		}

		// set time of EOD 
		thisFridayExitTime = SQTime.setDayOfWeek(thisFridayExitTime, FridayCloseTime == 0 ? SQTime.SATURDAY : SQTime.FRIDAY);
		thisFridayExitTime = SQTime.setHHMM(thisFridayExitTime, FridayCloseTime);	
		thisFridayExitTime = SQTime.setSecond(thisFridayExitTime, 0);		
		thisFridayExitTime = SQTime.setMiliSeconds(thisFridayExitTime, 0);	

		// set time of beginning of Sunday 
		thisSundayBeginTime = SQTime.setDayOfWeek(thisSundayBeginTime, SQTime.SUNDAY);
		thisSundayBeginTime = SQTime.setHHMM(thisSundayBeginTime, SundayOpenTime);	
		thisSundayBeginTime = SQTime.setSecond(thisSundayBeginTime, 0);		
		thisSundayBeginTime = SQTime.setMiliSeconds(thisSundayBeginTime, 0);	
	}
	
	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		DontTradeOnWeekends option = new DontTradeOnWeekends();
		option.DontTradeOnWeekends = this.DontTradeOnWeekends;
		option.FridayCloseTime = this.FridayCloseTime;
		option.SundayOpenTime = this.SundayOpenTime;
		
		return option;
	}
	
}
