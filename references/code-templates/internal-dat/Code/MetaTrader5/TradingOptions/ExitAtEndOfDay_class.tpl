
class CExitAtEndOfDay : public CTradingOption {
      private:
	   datetime dailyEODExitTime;
	   datetime EODTime;
	   bool closedThisDay;
      datetime dailySignalTimeRangeFrom;
      datetime dailySignalTimeRangeTo;
     

   public:
      CExitAtEndOfDay() {
         dailyEODExitTime = D'1970.01.01';
         EODTime = D'1970.01.01';         
         closedThisDay = false;
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
        if(!ExitAtEndOfDay) {
		      return(true);
	      } 
                  
         onTick();
        
	      datetime currentTime = TimeCurrent();

	       if(currentTime > EODTime) {
	         //it is a new day
		      initTimesForCurrentDay(currentTime);
   	   	      if(LimitTimeRange) {                                           
   			      initTimesRangeForCurrentDay(currentTime);
   		      }	 	
	      }
         
	     if (!LimitTimeRange && currentTime >= dailyEODExitTime) {
		      // returning false means there will be no more processing on this tick
	          // this is what we want because we don't want to be trading after close of all positions
		      return(false); 
	      }
	      
	     if (LimitTimeRange){

         	MqlDateTime currentTimedt;
         	TimeToStruct(currentTime, currentTimedt);
         	int currentTimeHHmm = currentTimedt.hour * 100 + currentTimedt.min;
         
         	MqlDateTime dailyEODExitTimedt;
         	TimeToStruct(dailyEODExitTime, dailyEODExitTimedt);
         	int dailyEODExitTimeHHmm = dailyEODExitTimedt.hour * 100 + dailyEODExitTimedt.min;
         
         	MqlDateTime SignalTimeRangeFromdt;
         	TimeToStruct(dailySignalTimeRangeFrom, SignalTimeRangeFromdt);
         	int SignalTimeRangeFromHHmm = SignalTimeRangeFromdt.hour * 100 + SignalTimeRangeFromdt.min;
         
         	MqlDateTime SignalTimeRangeTodt;
         	TimeToStruct(dailySignalTimeRangeTo, SignalTimeRangeTodt);
         	int SignalTimeRangeToHHmm = SignalTimeRangeTodt.hour * 100 + SignalTimeRangeTodt.min;

	            if (currentTimeHHmm >= dailyEODExitTimeHHmm  && currentTimeHHmm < SignalTimeRangeFromHHmm) {
	         	   return(false); 
	         	}
	            if (dailyEODExitTimeHHmm >= SignalTimeRangeFromHHmm && SignalTimeRangeFromHHmm < SignalTimeRangeToHHmm) {
   	         	if (currentTimeHHmm >= dailyEODExitTimeHHmm) {
   				   return(false); 
   			    }
			}
	    }
 	    return(true);
     }       
        
	   //------------------------------------------------------------------------

     virtual void onTick() {
        if(!ExitAtEndOfDay) {
		      return;
	      }
        
        datetime currentTime = TimeCurrent();
        datetime currentTimeDayStart = SQTime.correctDayStart(currentTime);     
		  datetime currentTimeDayEnd = SQTime.correctDayEnd(currentTime);
        
        if(!LimitTimeRange && !closedThisDay && currentTime >= dailyEODExitTime) {
             // we should close all positions at midnight, so close them at the first tick of a new day
             //Close open positions. If there was a gap at the end of a day, close only positions opened before current day start
             for (int cc = PositionsTotal() - 1; cc >= 0; cc--) {
                ulong positionTicket = PositionGetTicket(cc);
           
                if (PositionSelectByTicket(positionTicket) && 
                   checkMagicNumber(PositionGetInteger(POSITION_MAGIC)) &&
                   (currentTimeDayEnd == EODTime || PositionGetInteger(POSITION_TIME) < currentTimeDayStart)
                ) {
                    Verbose("Exit At End Of Day - Closing position...");
                    sqClosePositionAtMarket(positionTicket);
                }
             }
             
            //Close pending orders
            for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
               ulong orderTicket = OrderGetTicket(cc);
          
               if (OrderSelect(orderTicket) && 
                   checkMagicNumber(OrderGetInteger(ORDER_MAGIC))
               ) {                                     
                  Verbose("Exit At End Of Day - Closing order...");
                  closeOrder(orderTicket);
               }
            } 
			closedThisDay = true;
	      }

	    if (LimitTimeRange){
          	MqlDateTime currentTimedt;
         	TimeToStruct(currentTime, currentTimedt);
         	int currentTimeHHmm = currentTimedt.hour * 100 + currentTimedt.min;
         
         	MqlDateTime dailyEODExitTimedt;
         	TimeToStruct(dailyEODExitTime, dailyEODExitTimedt);
         	int dailyEODExitTimeHHmm = dailyEODExitTimedt.hour * 100 + dailyEODExitTimedt.min;
         
         	MqlDateTime SignalTimeRangeFromdt;
         	TimeToStruct(dailySignalTimeRangeFrom, SignalTimeRangeFromdt);
         	int SignalTimeRangeFromHHmm = SignalTimeRangeFromdt.hour * 100 + SignalTimeRangeFromdt.min;
         
         	MqlDateTime SignalTimeRangeTodt;
         	TimeToStruct(dailySignalTimeRangeTo, SignalTimeRangeTodt);
         	int SignalTimeRangeToHHmm = SignalTimeRangeTodt.hour * 100 + SignalTimeRangeTodt.min;      	

	        if (currentTimeHHmm >= dailyEODExitTimeHHmm  && currentTimeHHmm < SignalTimeRangeFromHHmm) {
	            if (PositionsTotal() > 0){
                      // we should close all positions at midnight, so close them at the first tick of a new day
                      //Close open positions. If there was a gap at the end of a day, close only positions opened before current day start
                      for (int cc = PositionsTotal() - 1; cc >= 0; cc--) {
                         ulong positionTicket = PositionGetTicket(cc);
                    
                         if (PositionSelectByTicket(positionTicket) && 
                            checkMagicNumber(PositionGetInteger(POSITION_MAGIC)) &&
                            (currentTimeDayEnd == EODTime || PositionGetInteger(POSITION_TIME) < currentTimeDayStart)
                         ) {
                             Verbose("Exit At End Of Day - Closing position...");
                             sqClosePositionAtMarket(positionTicket);
                         }
                      }
                }
                if (OrdersTotal() > 0){     
                      //Close pending orders
                      for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
                        ulong orderTicket = OrderGetTicket(cc);
                   
                        if (OrderSelect(orderTicket) && 
                            checkMagicNumber(OrderGetInteger(ORDER_MAGIC))
                        ) {                                     
                           Verbose("Exit At End Of Day - Closing order...");
                           closeOrder(orderTicket);
                        }
                    } 
      			}
   		    }
   		    if (dailyEODExitTimeHHmm >= SignalTimeRangeFromHHmm && SignalTimeRangeFromHHmm < SignalTimeRangeToHHmm) {
   	         	if (currentTimeHHmm >= dailyEODExitTimeHHmm) {    
      	         	if (PositionsTotal() > 0){
                         for (int cc = PositionsTotal() - 1; cc >= 0; cc--) {
                            ulong positionTicket = PositionGetTicket(cc);
                       
                            if (PositionSelectByTicket(positionTicket) && 
                               checkMagicNumber(PositionGetInteger(POSITION_MAGIC)) &&
                               (currentTimeDayEnd == EODTime || PositionGetInteger(POSITION_TIME) < currentTimeDayStart)
                            ) {
                                Verbose("Exit At End Of Day - Closing position...");
                                sqClosePositionAtMarket(positionTicket);
                            }
                         }
                    }
                    if (OrdersTotal() > 0){  
                         //Close pending orders
                         for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
                           ulong orderTicket = OrderGetTicket(cc);
                      
                           if (OrderSelect(orderTicket) && 
                               checkMagicNumber(OrderGetInteger(ORDER_MAGIC))
                           ) {                                     
                              Verbose("Exit At End Of Day - Closing order...");
                              closeOrder(orderTicket);
                           }
                        }
                    }
                } 
   	        }
	    }
          
    }
     
     //+----------------------------------------------+

    void initTimesForCurrentDay(datetime currentTime) {
	      // set end time of the current day (so that we now when new day starts)
	      EODTime = SQTime.correctDayEnd(currentTime);

	      // set time of EOD

   	      if(EODExitTime == "00:00" || EODExitTime == "0:00"){
   	         dailyEODExitTime = EODTime;
   	      }
   	      else {
   	         dailyEODExitTime = SQTime.setHHMM(currentTime, EODExitTime);
   	      }
	      
	      closedThisDay = false;
    }
     //------------------------------------------------------------------------

	void initTimesRangeForCurrentDay(datetime currentTime) {
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
      if (dailyEODExitTime < dailySignalTimeRangeTo) dailyEODExitTime +=86400;
	 }
};

// create variable for class instance (required)
CExitAtEndOfDay* objExitAtEndOfDay;