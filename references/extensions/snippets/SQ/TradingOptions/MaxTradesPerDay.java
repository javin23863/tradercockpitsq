package SQ.TradingOptions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class MaxTradesPerDay extends TradingOption {
	
	@Parameter(name="Maximum Trades Per Day", defaultValue="0", minValue=0, category="Trading options")
	@Help("Maximum allowed number of trades per day, 0 means there is no limit.")
	@ForEngine("*,-SP,-SA")
	public int MaxTradesPerDay;
	

	private int historyPositionPreviousDay = -1;
	private long openTimeToday;
	private long EODTime;
	private boolean reachedLimitToday = false;

	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return (MaxTradesPerDay != 0);
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws TradingException {
		if(MaxTradesPerDay == 0) {
			return true;
		}
		
		long currentTime = Strategy.MarketData.TimeCurrent();

		if(currentTime > EODTime) {
			// it is new day
			initForCurrentDay(currentTime);
		}		
		
		if(reachedLimitToday) {
			return false;
		}

		if(getNumberOfTradesToday(Strategy) >= MaxTradesPerDay) {
			reachedLimitToday = true;

			return(false);
		}
		
		return true;
	}

	//------------------------------------------------------------------------

	private void initForCurrentDay(long currentTime) {
		// set end time of the current day (so that we now when new day starts)
		EODTime = SQTime.correctDayEndMT(currentTime);

		openTimeToday = SQTime.correctDayStart(currentTime);
		
		reachedLimitToday = false;
	}
	
	//------------------------------------------------------------------------
	
	private int getNumberOfTradesToday(StrategyBase Strategy) {
		
		int count = 0;
		
		// count closed (history) trades that started today
		int startAt = historyPositionPreviousDay+1;
		
		for(int i=startAt; i<Strategy.Trader.getHistoryOrdersCount(); i++) {
			Order order = Strategy.Trader.getHistoryOrder(i);

			if(order.isPendingOrder() || !order.StrategyName.equals(Strategy.getStrategyName())) {
				continue;
			}
			
			if(order.OpenTime >= openTimeToday) {
				count++;
			} 
			else {
				// trade belongs to some day earlier, we don't need to check this history
				historyPositionPreviousDay = i;
			}
		}

		// count open trades that started today
		for(int i=0; i<Strategy.Trader.getOpenOrdersCount(true); i++) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, true);
			
			if(!order.isFilled()) {
				// only filled orders should be counted as open trades today
				continue;
			}
			
			long openTime = order.isFilled() ? order.getOpenTime() : order.getOriginalOpenTime();

			if(order.getStrategyName().equals(Strategy.getStrategyName()) && openTime >= openTimeToday) {
				count++;
			}
		}

		return count;
	}
	
	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		MaxTradesPerDay option = new MaxTradesPerDay();
		option.MaxTradesPerDay = this.MaxTradesPerDay;

		return option;
	}		
}
