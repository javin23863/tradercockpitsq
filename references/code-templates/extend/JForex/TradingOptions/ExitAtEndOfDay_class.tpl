

//------------------------------------------------------------------
// -- CExitAtEndOfDay
//------------------------------------------------------------------
private class  CExitAtEndOfDay implements ITradingOption {
	private long dailyEODExitTime;
	private long EODTime;
	private boolean closedThisDay;

	public CExitAtEndOfDay() {
		dailyEODExitTime = -1;
		EODTime = -1;
		closedThisDay = false;
	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		if(!ExitAtEndOfDay) {
			return true;
		}                   

		long currentTime = getCurrentTime();

		if(currentTime > EODTime) {
			//it is a new day
			initTimesForCurrentDay(currentTime);
		}

		checkOrders();

		if(currentTime >= dailyEODExitTime) {
			// returning false means there will be no more processing on this tick
			// this is what we want because we don't want to be trading after close of all positions
			return false; 
		}

		return true;
	}     

	//------------------------------------------------------------------------

	private void checkOrders() throws JFException {
		long currentTime = getCurrentTime();
		long currentTimeDayStart = SQTime.correctDayStart(currentTime);    
		long currentTimeDayEnd = SQTime.correctDayEnd(currentTime);

		if(!closedThisDay && currentTime >= dailyEODExitTime) {
			orders = getOrders();
			for (int cc = 0; cc < orders.size(); cc++) {
				order = orders.get(cc);
				orderType = order.getOrderCommand();

				if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

				boolean isLiveOrder = !orderType.isConditional();

				//Close all orders at the end of a day. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
				if(currentTimeDayEnd == EODTime || !isLiveOrder || order.getFillTime() < currentTimeDayStart) {
					if(isLiveOrder){
						sqClosePositionAtMarket(order, 0, "End Of Day");
					} else {
						sqDeletePendingOrder(order, "End Of Day");
					}
				}
			}

			closedThisDay = true;
		}  
	}

	//------------------------------------------------------------------------

	private void initTimesForCurrentDay(long currentTime) {
		// set end time of the current day (so that we now when new day starts)
		EODTime = SQTime.correctDayEnd(currentTime);

		// set time of EOD
		if(DayExitTime.equals("00:00") || DayExitTime.equals("0:00")){
			dailyEODExitTime = EODTime;
		} else {
			dailyEODExitTime = SQTime.setHHMM(currentTime, DayExitTime);
		}

		closedThisDay = false;
	}
}

// create variable for class instance (required)
private CExitAtEndOfDay objExitAtEndOfDay = new CExitAtEndOfDay();


