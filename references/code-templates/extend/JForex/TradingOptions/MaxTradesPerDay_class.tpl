

//------------------------------------------------------------------
// -- CMaxTradesPerDay
//------------------------------------------------------------------
private class  CMaxTradesPerDay implements ITradingOption {
	private int lastHistoryPositionChecked;
	private long openTimeToday;
	private long EODTime;
	private boolean reachedLimitToday;

	public CMaxTradesPerDay() {
		EODTime = -1;
		lastHistoryPositionChecked = 0;
	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		if(MaxTradesPerDay <= 0) {
			return true;
		}

		long currentTime = getCurrentTime();

		if(currentTime > EODTime) {
			// it is new day
			initTimeForCurrentDay(currentTime);
		}		

		if(reachedLimitToday) {
			return false;
		}

		if(getNumberOfTradesToday() >= MaxTradesPerDay) {
			reachedLimitToday = true;
			return false;
		}

		return true;
	}

	//------------------------------------------------------------------------

	private void initTimeForCurrentDay(long currentTime) {
		// set end time of the current day (so that we now when new day starts)
		EODTime = SQTime.correctDayEnd(currentTime);

		openTimeToday = SQTime.correctDayStart(currentTime);

		reachedLimitToday = false;
	}
	
	//------------------------------------------------------------------------

	private int getNumberOfTradesToday() throws JFException {
		int todayTradesCount = 0;
		int i = 0;

		// count closed trades that started today
		int startAt = lastHistoryPositionChecked -10;
		if(startAt < 0) {
			startAt = 0;
		}

		orders = sqGetHistoryOrders("any");
		for(i=startAt;i<orders.size();i++) {
			order = orders.get(i);
			lastHistoryPositionChecked = i;

			if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

			if(order.getFillTime() >= openTimeToday) {
				todayTradesCount++;
			}
		}

		orders = getOrders();	
		for(i=0; i<orders.size(); i++) {
			order = orders.get(i);

			if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

			if(order.getFillTime() >= openTimeToday) {
				todayTradesCount++;
			}
		}

		return todayTradesCount;
	}      
}

// create variable for class instance (required)
private CMaxTradesPerDay objMaxTradesPerDay = new CMaxTradesPerDay();