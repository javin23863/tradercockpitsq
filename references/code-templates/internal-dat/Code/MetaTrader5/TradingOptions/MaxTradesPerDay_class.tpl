


class CMaxTradesPerDay : public CTradingOption {
   private:
      datetime openTimeToday;
      datetime EODTime;
      bool reachedLimitToday;
      
   public:
      CMaxTradesPerDay() {
         EODTime = D'1970.01.01';
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

			HistorySelect(openTimeToday, TimeCurrent());
			
       //History orders not filled
       for(int i=HistoryOrdersTotal() - 1; i>=0; i--) {
         ulong ticket = HistoryOrderGetTicket(i);
         ENUM_ORDER_STATE state = (ENUM_ORDER_STATE) HistoryOrderGetInteger(ticket, ORDER_STATE);
         
         if(HistoryOrderSelect(ticket) && 
             checkMagicNumber(HistoryOrderGetInteger(ticket, ORDER_MAGIC)) &&
             HistoryOrderGetInteger(ticket, ORDER_TIME_SETUP) >= openTimeToday &&
             (state != ORDER_STATE_FILLED && state != ORDER_STATE_PARTIAL)
         ) {
            todayTradesCount++;
         }
       }
                                   
       //History deals
       for(int i=HistoryDealsTotal() - 1; i>=0; i--) {
         ulong ticket = HistoryDealGetTicket(i);
         
         if(HistoryDealSelect(ticket) && 
            checkMagicNumber(HistoryDealGetInteger(ticket, DEAL_MAGIC)) &&
            HistoryDealGetInteger(ticket, DEAL_ENTRY) == DEAL_ENTRY_IN
         ){ 
            if(HistoryDealGetInteger(ticket, DEAL_TIME) >= openTimeToday){
               todayTradesCount++;
            }
         }
       }
       
       //Pending orders
       for(int i=OrdersTotal() - 1; i>=0; i--) {
         ulong ticket = OrderGetTicket(i);
         
         if(OrderSelect(ticket) && 
            checkMagicNumber(OrderGetInteger(ORDER_MAGIC)) &&
            OrderGetInteger(ORDER_TIME_SETUP) >= openTimeToday
         ){ 
            todayTradesCount++;
         }
       }
   
			return todayTradesCount;
		}      
};

// create variable for class instance (required)
CMaxTradesPerDay* objMaxTradesPerDay;