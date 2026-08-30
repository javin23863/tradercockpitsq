//------------------------------------------------------------------
// -- CDontTradeOnWeekends
//------------------------------------------------------------------

private class  CDontTradeOnWeekends implements ITradingOption {
	  private long thisFridayCloseTime; 
	  private long thisSundayOpenTime;
       
	  public CDontTradeOnWeekends() {
         thisFridayCloseTime = -1;    
         thisSundayOpenTime = -1;
      }

      //+----------------------------------------------+

	  @Override
	  public boolean onTick() throws JFException {
         if(!DontTradeOnWeekends) {
			return true;
         }

         long currentTime = getCurrentTime();

         if(thisFridayCloseTime < 0) {
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
    	} else {
            // new week starting
            initTimes(currentTime, SQTime.getDayOfWeek(currentTime) == 0 ? 1 : 0); 

            return true;
         }
      }           

      //+----------------------------------------------+

      void initTimes(long currentTime, int addDays) {
		if(addDays > 0) {
			thisFridayCloseTime = SQTime.addDays(currentTime, addDays);
		} else {
			thisFridayCloseTime = currentTime;
		}

		// set time of EOD 
		thisFridayCloseTime = SQTime.setDayOfWeek(thisFridayCloseTime, (FridayCloseTime.equals("00:00") || FridayCloseTime.equals("0:00")) ? SQTime.SATURDAY : SQTime.FRIDAY);
		thisFridayCloseTime = SQTime.setHHMM(thisFridayCloseTime, FridayCloseTime);	

		thisSundayOpenTime = SQTime.setDayOfWeek(currentTime, SQTime.SUNDAY);
		thisSundayOpenTime = SQTime.setHHMM(thisSundayOpenTime, SundayOpenTime);	
	}
};

// create variable for class instance (required)
private CDontTradeOnWeekends objDontTradeOnWeekends = new CDontTradeOnWeekends();