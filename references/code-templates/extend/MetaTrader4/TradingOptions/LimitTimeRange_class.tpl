

class CLimitTimeRange : public CTradingOption {
   private:
      datetime dailySignalTimeRangeFrom;
      datetime dailySignalTimeRangeTo;
      bool closedThisDay;
      
   public:
      CLimitTimeRange() {
         closedThisDay = false;
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {     
	    if(!LimitTimeRange) {
		    return true;
	    }
                                
        onTick();
        
	    datetime currentTime = TimeCurrent();
	
	    if(currentTime > dailySignalTimeRangeTo) {
		    // it is new day
		    initTimesForCurrentDay(currentTime);
	    }

	    if(currentTime < dailySignalTimeRangeFrom || currentTime >= dailySignalTimeRangeTo) {
		    // time is outside given range
		    // returning false means there will be no more processing on this tick
		   	// this is what we want because we don't want to be trading outside of this time range
		    
		    return false; 
	    }
	
	    return true;
	 }
        
     //------------------------------------------------------------------------

     virtual void onTick() {
        if(!LimitTimeRange) {
		    return;
	    }
        
        datetime currentTime = TimeCurrent();
        if(!closedThisDay && ExitAtEndOfRange && currentTime >= dailySignalTimeRangeTo) {
		   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
              if (OrderSelect(cc, SELECT_BY_POS)) {
        
                 if(!checkMagicNumber(OrderMagicNumber())) continue;
                 
                 bool isLiveOrder = OrderType() == OP_BUY || OrderType() == OP_SELL;
               
                 if(isLiveOrder){
                    if(OrderTypeToExit != 2) { // not pending only
                       sqClosePositionAtMarket(OrderLots());
                    }
                 }
                 else {
                    if(OrderTypeToExit != 1) { // not live only
                       sqDeletePendingOrder(OrderTicket());
                    }
                 }
              }
           }
           
		   closedThisDay = true;
	    }
     }

    //------------------------------------------------------------------------

    void initTimesForCurrentDay(datetime currentTime) {
   	// set time of range open 
   	dailySignalTimeRangeFrom = SQTime.setHHMM(currentTime, SignalTimeRangeFrom);
	
	   dailySignalTimeRangeTo = SQTime.setHHMM(currentTime, SignalTimeRangeTo);    

      int timeFrom = getHHMM(SignalTimeRangeFrom);
      int timeTo = getHHMM(SignalTimeRangeTo);

      if(timeFrom >= timeTo){
         if(getSQTime(currentTime) < timeTo){
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
};


// create variable for class instance (required)
CLimitTimeRange* objLimitTimeRange;
