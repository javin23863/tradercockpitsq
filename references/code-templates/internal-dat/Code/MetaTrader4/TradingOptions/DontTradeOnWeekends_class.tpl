class CDontTradeOnWeekends: public CTradingOption {
   private:
      datetime thisFridayCloseTime; 
      datetime thisSundayOpenTime;
       
   public:
      CDontTradeOnWeekends() {
         thisFridayCloseTime = D'1970.01.01';    
         thisSundayOpenTime = D'1970.01.01';
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         if(!DontTradeOnWeekends) {
	  		    return true;
         }

         datetime currentTime = TimeCurrent();

         if(thisFridayCloseTime < 100) {
            initTimes(currentTime, 0);
         }
         
         if(currentTime < thisFridayCloseTime) {
            // trade normally
            return true;
         }
         
         if(currentTime < thisSundayOpenTime) {
    		   // do not allow opening new positions until sunday.
   			   // returning false means there will be no more processing on this tick.
   			   // this is what we want because we don't want to be trading after close of all positions
   			   return false;
    		}
         else {
            // new week starting
            initTimes(currentTime, DayOfWeek() == 0 ? 1 : 0); 

            return true;
         }
      }           

      //+----------------------------------------------+

      void initTimes(datetime currentTime, int addDays) {
         if(addDays > 0) {
			    thisFridayCloseTime = SQTime.addDays(currentTime, addDays);
	      } else {
			    thisFridayCloseTime = currentTime;
	      }
	
	      // set time of EOD 
	      thisFridayCloseTime = SQTime.setDayOfWeek(thisFridayCloseTime, (FridayCloseTime == "00:00" || FridayCloseTime == "0:00") ? SATURDAY : FRIDAY);
	      thisFridayCloseTime = SQTime.setHHMM(thisFridayCloseTime, FridayCloseTime);	
	     
	      thisSundayOpenTime = SQTime.setDayOfWeek(currentTime, SUNDAY);
         thisSundayOpenTime = SQTime.setHHMM(thisSundayOpenTime, SundayOpenTime);	
      }
};

// create variable for class instance (required)
CDontTradeOnWeekends* objDontTradeOnWeekends;