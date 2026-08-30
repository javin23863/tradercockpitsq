


class CMaxTradesPerDay : public CTradingOption {
   private:
      int lastHistoryPositionChecked;
      datetime openTimeToday;
      datetime EODTime;
      bool reachedLimitToday;
      
   public:
      CMaxTradesPerDay() {
         EODTime = D'1970.01.01';
         lastHistoryPositionChecked = 0;
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         if(MaxTradesPerDay <= 0) {
            return true;
         }
		
         datetime currentTime = TimeCurrent();

         if(currentTime > EODTime) {
            // it is new day
            initTimeForCurrentDay(currentTime);
         }		
		
		   if(reachedLimitToday) {
		      return false;
		   }
		   
         if(getNumberOfTradesToday() >= MaxTradesPerDay) {
            reachedLimitToday = true;
            return(false);
         }
		
         return true;
      }

      //------------------------------------------------------------------------

      void initTimeForCurrentDay(datetime currentTime) {
			// set end time of the current day (so that we now when new day starts)
         EODTime = SQTime.correctDayEnd(currentTime);

         openTimeToday = SQTime.correctDayStart(currentTime);
         
         reachedLimitToday = false;
      }
      
      //------------------------------------------------------------------------

		int getNumberOfTradesToday() {
			int todayTradesCount = 0;
			int i = 0;

			// count closed trades that started today
			int startAt = lastHistoryPositionChecked -10;
			if(startAt < 0) {
				startAt = 0;
			}

         for(i=startAt;i<OrdersHistoryTotal();i++) {
            if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true && checkMagicNumber(OrderMagicNumber())) {
               lastHistoryPositionChecked = i;

               if(OrderOpenTime() >= openTimeToday) {
                  todayTradesCount++;
               }
            }
         }

         for(i=0; i<OrdersTotal(); i++) {
            if (OrderSelect(i,SELECT_BY_POS)==true && checkMagicNumber(OrderMagicNumber())) {

               if(OrderOpenTime() >= openTimeToday) {
                  todayTradesCount++;
               }
            }
         }
   
			return todayTradesCount;
		}      
};

// create variable for class instance (required)
CMaxTradesPerDay* objMaxTradesPerDay;