

//------------------------------------------------------------------
// -- CLimitTimeRange
//------------------------------------------------------------------
private class  CLimitTimeRange implements ITradingOption {
	private long dailySignalTimeRangeFrom;
	private long dailySignalTimeRangeTo;
	private boolean closedThisDay;

	public CLimitTimeRange() {
		closedThisDay = false;
	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		if(!LimitSignalsTimeRange) {
			return true;
		}

		long currentTime = getCurrentTime();

		if(currentTime > dailySignalTimeRangeTo) {
			// it is new day
			initTimesForCurrentDay(currentTime);
		}

		checkOrders();

		if(currentTime < dailySignalTimeRangeFrom || currentTime >= dailySignalTimeRangeTo) {
			// time is outside given range
			// returning false means there will be no more processing on this tick
			// this is what we want because we don't want to be trading outside of this time range

			return false; 
		}

		return true;
	}

	//------------------------------------------------------------------------

	private void checkOrders() throws JFException {
		long currentTime = getCurrentTime();
		if(!closedThisDay && ExitAtEndOfRange && currentTime >= dailySignalTimeRangeTo) {
			orders = getOrders();
			for (int cc = 0; cc < orders.size(); cc++) {
				order = orders.get(cc);
				orderType = order.getOrderCommand();

				if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

				if(!orderType.isConditional()) {
					sqClosePositionAtMarket(order, 0, "End Of Range");
				} else {
					sqDeletePendingOrder(order, "End Of Range");
				}
			}

			closedThisDay = true;
		}
	}

	//------------------------------------------------------------------------

	void initTimesForCurrentDay(long currentTime) {
		// set time of range open 
		dailySignalTimeRangeFrom = SQTime.setHHMM(currentTime, SignalTimeRangeFrom);
		
		dailySignalTimeRangeTo = SQTime.setHHMM(currentTime, SignalTimeRangeTo);    

		int timeFrom = getHHMM(SignalTimeRangeFrom);
		int timeTo = getHHMM(SignalTimeRangeTo);

		if(timeFrom >= timeTo){
			if(getSQTime(currentTime) < timeTo){
				dailySignalTimeRangeFrom = SQTime.addDays(dailySignalTimeRangeFrom, -1);
			} else {
				dailySignalTimeRangeTo = SQTime.addDays(dailySignalTimeRangeTo, 1);
			}
		} else {
			if(currentTime > dailySignalTimeRangeTo) {
				dailySignalTimeRangeFrom = SQTime.addDays(dailySignalTimeRangeFrom, 1);
				dailySignalTimeRangeTo = SQTime.addDays(dailySignalTimeRangeTo, 1);
			}
		}

	   closedThisDay = false;
    }
}


// create variable for class instance (required)
private CLimitTimeRange objLimitTimeRange = new CLimitTimeRange();
