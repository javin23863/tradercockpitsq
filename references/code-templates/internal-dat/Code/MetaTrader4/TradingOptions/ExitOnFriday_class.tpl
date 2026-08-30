class CExitOnFriday : public CTradingOption {
   private:
      datetime thisFridayExitTime; 
      datetime thisSundayBeginTime;
      datetime thisDefaultTime;  
      datetime EOFDayTime;
      bool closedThisWeek;
       
   public:
      CExitOnFriday() {
         thisDefaultTime = D'1970.01.01';
         thisFridayExitTime = D'1970.01.01';    
         thisSundayBeginTime = D'1970.01.01';
         closedThisWeek = false;
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         if(!ExitOnFriday) {
	  		    return true;
         }
         
         onTick();

         datetime currentTime = TimeCurrent();

         if(thisFridayExitTime < 100) {
            initFridayExitTime(currentTime, 0);
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
    		}
         else {
            // new week starting
            initFridayExitTime(currentTime, DayOfWeek() == 0 ? 1 : 0); 

            return true;
         }
      }           
        
	   //------------------------------------------------------------------------

     virtual void onTick() {
        if(!ExitOnFriday) {
	  		    return;
        }
        
        datetime currentTime = TimeCurrent();      
        datetime currentTimeDayStart = SQTime.correctDayStart(currentTime);      
				datetime currentTimeDayEnd = SQTime.correctDayEnd(currentTime);

    		if(!closedThisWeek && currentTime >= thisFridayExitTime && currentTime < thisSundayBeginTime && thisDefaultTime != thisFridayExitTime) {
   				 // time is over friday closing time, we should close the positions
   				 for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
              if (OrderSelect(cc, SELECT_BY_POS)) {
        
                 if(!checkMagicNumber(OrderMagicNumber())) continue;
                 
                 bool isLiveOrder = OrderType() == OP_BUY || OrderType() == OP_SELL;
                 
                 //Close all orders at the end of Friday. When there is a data gap, on the first tick of new day close all pending orders and orders filled before current day start
                 if(currentTimeDayEnd == EOFDayTime || !isLiveOrder || OrderOpenTime() < currentTimeDayStart) {
                    if(isLiveOrder){
                        sqClosePositionAtMarket(OrderLots());
                    }
                    else {
                        sqDeletePendingOrder(OrderTicket());
                    }
                 }
              }
           }
           
   				 closedThisWeek = true;
    		} 
     }

      //+----------------------------------------------+

      void initFridayExitTime(datetime currentTime, int addDays) {
         if(addDays > 0) {
			    thisFridayExitTime = SQTime.addDays(currentTime, addDays);
	      } else {
			    thisFridayExitTime = currentTime;
	      }
	
	      // set time of EOD 
	      thisFridayExitTime = SQTime.setDayOfWeek(thisFridayExitTime, (FridayExitTime == "00:00" || FridayExitTime == "0:00") ? SATURDAY : FRIDAY);
	      thisFridayExitTime = SQTime.setHHMM(thisFridayExitTime, FridayExitTime);	
	     
        EOFDayTime = SQTime.correctDayEnd(thisFridayExitTime);
       
	      thisSundayBeginTime = SQTime.setDayOfWeek(thisFridayExitTime, SUNDAY);
	      thisSundayBeginTime = SQTime.correctDayStart(thisSundayBeginTime);
        
        closedThisWeek = false;
      }
};

// create variable for class instance (required)
CExitOnFriday* objExitOnFriday;