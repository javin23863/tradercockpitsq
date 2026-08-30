


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
         
         MqlDateTime timeStruct;
         datetime currentTime = TimeCurrent(timeStruct);

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
            initFridayExitTime(currentTime, timeStruct.day_of_week == 0 ? 1 : 0); 
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

    		if(!closedThisWeek && currentTime >= thisFridayExitTime  && currentTime < thisSundayBeginTime && thisDefaultTime != thisFridayExitTime) {
   				 // time is over friday closing time, we should close the positions
   				 
           //Close open positions. If there was a gap at the end of a day, close only positions opened before current day start
          for (int cc = PositionsTotal() - 1; cc >= 0; cc--) {
             ulong positionTicket = PositionGetTicket(cc);
        
             if (PositionSelectByTicket(positionTicket) && 
                checkMagicNumber(PositionGetInteger(POSITION_MAGIC)) &&
                (currentTimeDayEnd == EOFDayTime || PositionGetInteger(POSITION_TIME) < currentTimeDayStart)
             ) {
                 Verbose("Exit On Friday - Closing position...");
                 sqClosePositionAtMarket(positionTicket);
             }
          }
          
          //Close pending orders
          for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
            ulong orderTicket = OrderGetTicket(cc);
       
            if (OrderSelect(orderTicket) && 
                checkMagicNumber(OrderGetInteger(ORDER_MAGIC))
            ) {
               Verbose("Exit On Friday - Closing order...");
               closeOrder(orderTicket);
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