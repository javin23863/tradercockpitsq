

//------------------------------------------------------------------
// -- CExitOnFriday
//------------------------------------------------------------------
private class  CExitOnFriday implements ITradingOption {
	private long thisFridayExitTime; 
	private long thisSundayBeginTime;
	private long EOFDayTime;
	private boolean closedThisWeek;

	public CExitOnFriday() {
		thisFridayExitTime = -1;    
		thisSundayBeginTime = -1;
		closedThisWeek = false;
	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		if(!ExitOnFriday) {
			return true;
		}

		long currentTime = getCurrentTime();

		if(thisFridayExitTime < 0) {
			initFridayExitTime(currentTime, 0);
		}

		checkOrders();

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
			initFridayExitTime(currentTime, SQTime.getDayOfWeek(currentTime) == 0 ? 1 : 0); 

			return true;
		}
	}           

	//------------------------------------------------------------------------

	private void checkOrders() throws JFException {
		long currentTime = getCurrentTime();      
		long currentTimeDayStart = SQTime.correctDayStart(currentTime);      
		long currentTimeDayEnd = SQTime.correctDayEnd(currentTime);

		if(!closedThisWeek && currentTime >= thisFridayExitTime) {
			// time is over friday closing time, we should close the positions
			orders = getOrders();
			for (int cc = 0; cc < orders.size(); cc++) {
				order = orders.get(cc);
				orderType = order.getOrderCommand();

				if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

				boolean isLiveOrder = !orderType.isConditional();

				//Close all orders at the end of Friday. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
				if(currentTimeDayEnd == EOFDayTime || !isLiveOrder || order.getFillTime() < currentTimeDayStart) {
					if(isLiveOrder){
						sqClosePositionAtMarket(order, 0, "End Of Friday");
					} else {
						sqDeletePendingOrder(order, "End Of Friday");
					}
				}
			}

			closedThisWeek = true;
		} 
	}

	//------------------------------------------------------------------------

	private void initFridayExitTime(long currentTime, int addDays) {
		if(addDays > 0) {
			thisFridayExitTime = SQTime.addDays(currentTime, addDays);
		} else {
			thisFridayExitTime = currentTime;
		}

		// set time of EOD 
		thisFridayExitTime = SQTime.setDayOfWeek(thisFridayExitTime, (FridayExitTime.equals("00:00") || FridayExitTime.equals("0:00")) ? SQTime.SATURDAY : SQTime.FRIDAY);
		thisFridayExitTime = SQTime.setHHMM(thisFridayExitTime, FridayExitTime);	

		EOFDayTime = SQTime.correctDayEnd(thisFridayExitTime);

		thisSundayBeginTime = SQTime.setDayOfWeek(currentTime, SQTime.SUNDAY);
		thisSundayBeginTime = SQTime.correctDayStart(thisSundayBeginTime);

		closedThisWeek = false;
	}
}

// create variable for class instance (required)
private CExitOnFriday objExitOnFriday = new CExitOnFriday();