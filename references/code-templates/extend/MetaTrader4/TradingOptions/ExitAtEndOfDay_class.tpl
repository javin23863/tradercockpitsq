
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
		      if (LimitTimeRange) {
                 initTimesRangeForCurrentDay(currentTime);
            }
	      }
	      
	      if(!LimitTimeRange && currentTime >= dailyEODExitTime) {
		      // returning false means there will be no more processing on this tick
	        // this is what we want because we don't want to be trading after close of all positions
		      return(false); 
	      }
   	    if (LimitTimeRange) {
            int currentTimeHHmm = TimeHour(currentTime) * 100 + TimeMinute(currentTime);
            int dailyEODExitTimeHHmm = TimeHour(dailyEODExitTime) * 100 + TimeMinute(dailyEODExitTime);
            int SignalTimeRangeFromHHmm = TimeHour(dailySignalTimeRangeFrom) * 100 + TimeMinute(dailySignalTimeRangeFrom);
            int SignalTimeRangeToHHmm = TimeHour(dailySignalTimeRangeTo) * 100 + TimeMinute(dailySignalTimeRangeTo);    	
            if (currentTimeHHmm >= dailyEODExitTimeHHmm && currentTimeHHmm < SignalTimeRangeFromHHmm){
               return false;
            }
            if (dailyEODExitTimeHHmm >= SignalTimeRangeFromHHmm && SignalTimeRangeFromHHmm < SignalTimeRangeToHHmm) {
               if (currentTimeHHmm >= dailyEODExitTimeHHmm){
                  return false;
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
		      for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
               if (OrderSelect(cc, SELECT_BY_POS)) {
         
                  if(!checkMagicNumber(OrderMagicNumber())) continue;
                  
                  bool isLiveOrder = OrderType() == OP_BUY || OrderType() == OP_SELL;
                  
                  //Close all orders at the end of a day. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
                  if(currentTimeDayEnd == EODTime || !isLiveOrder || OrderOpenTime() < currentTimeDayStart) {
                     if(isLiveOrder){
                        sqClosePositionAtMarket(OrderLots());
                     }
                     else {
                        sqDeletePendingOrder(OrderTicket());
                     }
                  }
               }
            }
          
			closedThisDay = true;
	      }
	      if (LimitTimeRange) {
            int currentTimeHHmm = TimeHour(currentTime) * 100 + TimeMinute(currentTime);
            int dailyEODExitTimeHHmm = TimeHour(dailyEODExitTime) * 100 + TimeMinute(dailyEODExitTime);
            int SignalTimeRangeFromHHmm = TimeHour(dailySignalTimeRangeFrom) * 100 + TimeMinute(dailySignalTimeRangeFrom);
            int SignalTimeRangeToHHmm = TimeHour(dailySignalTimeRangeTo) * 100 + TimeMinute(dailySignalTimeRangeTo);
            if (currentTimeHHmm >= dailyEODExitTimeHHmm && currentTimeHHmm < SignalTimeRangeFromHHmm){
               for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
                  if (OrderSelect(cc, SELECT_BY_POS)) {
            
                     if(!checkMagicNumber(OrderMagicNumber())) continue;
                     
                     bool isLiveOrder = OrderType() == OP_BUY || OrderType() == OP_SELL;
                     
                     //Close all orders at the end of a day. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
                     if(currentTimeDayEnd == EODTime || !isLiveOrder || OrderOpenTime() < currentTimeDayStart) {
                        if(isLiveOrder){
                           sqClosePositionAtMarket(OrderLots());
                        }
                        else {
                           sqDeletePendingOrder(OrderTicket());
                        }
                     }
                  }
               }
            }
            
            if (dailyEODExitTimeHHmm >= SignalTimeRangeFromHHmm && SignalTimeRangeFromHHmm < SignalTimeRangeToHHmm) {
               if (currentTimeHHmm >= dailyEODExitTimeHHmm){
                  for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
                     if (OrderSelect(cc, SELECT_BY_POS)) {
               
                        if(!checkMagicNumber(OrderMagicNumber())) continue;
                        
                        bool isLiveOrder = OrderType() == OP_BUY || OrderType() == OP_SELL;
                        
                        //Close all orders at the end of a day. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
                        if(currentTimeDayEnd == EODTime || !isLiveOrder || OrderOpenTime() < currentTimeDayStart) {
                           if(isLiveOrder){
                              sqClosePositionAtMarket(OrderLots());
                           }
                           else {
                              sqDeletePendingOrder(OrderTicket());
                           }
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
      dailySignalTimeRangeFrom = setHHMM(currentTime, SignalTimeRangeFrom);
      dailySignalTimeRangeTo = setHHMM(currentTime, SignalTimeRangeTo);

      int timeFrom = getHHMM(SignalTimeRangeFrom);
      int timeTo = getHHMM(SignalTimeRangeTo);

      if (timeFrom >= timeTo) {
         if (getHHMM(TimeToStr(currentTime)) < timeTo)
            dailySignalTimeRangeFrom -= 86400;
         else
            dailySignalTimeRangeTo += 86400;
      } else {
         if (currentTime > dailySignalTimeRangeTo) {
            dailySignalTimeRangeFrom += 86400;
            dailySignalTimeRangeTo += 86400;
         }
      }

      if (dailyEODExitTime < dailySignalTimeRangeTo)
         dailyEODExitTime += 86400;
   }

   // Helpers (mock or define them in your MQL4 project):
    int getHHMM(string timeStr) {
      int h = StrToInteger(StringSubstr(timeStr, 0, 2));
      int m = StrToInteger(StringSubstr(timeStr, 3, 2));
      return h * 100 + m;
    }

    datetime setHHMM(datetime baseTime, string hhmm) {
      int h = StrToInteger(StringSubstr(hhmm, 0, 2));
      int m = StrToInteger(StringSubstr(hhmm, 3, 2));
      datetime dt = iTime(NULL, PERIOD_D1, 0); // Start of day
      return dt + h * 3600 + m * 60;
    }
};

// create variable for class instance (required)
CExitAtEndOfDay* objExitAtEndOfDay;
