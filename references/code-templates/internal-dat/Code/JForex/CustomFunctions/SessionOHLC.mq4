double SessionOpen(int startHours, int startMinutes, int daysAgo){
   return getSessionPrice(1, startHours, startMinutes, startHours, startMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionHigh(int startHours, int startMinutes, int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(2, startHours, startMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionLow(int startHours, int startMinutes, int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(3, startHours, startMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionClose(int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(4, endHours, endMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double getSessionPrice(int type, int startHours, int startMinutes, int endHours, int endMinutes, int daysShift){
   int totalEndDaysShift = daysShift;
   int totalStartDaysShift = daysShift;
   
   datetime currentTime = Time[0];
   
   int currentHHMM = TimeHour(currentTime) * 100 + TimeMinute(currentTime);
   int startHHMM = startHours * 100 + startMinutes;
   int endHHMM = endHours * 100 + endMinutes;
   
   if(type == 1){
      if(currentHHMM < startHHMM){
         totalEndDaysShift++;
         totalStartDaysShift++;
      }
   }
   else if(type == 4){
      if(currentHHMM >= startHHMM){
         totalEndDaysShift--;
         totalStartDaysShift--;
      }
   }
   else {
      if(startHHMM < endHHMM){
         if(currentHHMM < startHHMM){
            totalEndDaysShift++;
            totalStartDaysShift++;
         }
      }
      else {
         if(currentHHMM < startHHMM){
            totalStartDaysShift++;
         }
         else {
            totalEndDaysShift--;
         }
      }
   }
   
   int totalBars = Bars(NULL, 0);
   int currentIndex = 0;
   int lastDayOfYear = TimeDayOfYear(currentTime);
   int daysAgo = 0;
   
   double highest = -1000000000;
   double lowest = 1000000000;
   
   while(currentIndex < totalBars){
      datetime time = Time[currentIndex];
      int hhmm = TimeHour(time) * 100 + TimeMinute(time);
      int curDayOfYear = TimeDayOfYear(time);
      
      if(curDayOfYear != lastDayOfYear) {
			lastDayOfYear = curDayOfYear;
			daysAgo++;
		}
		
		if(type == 2 || type == 3){
		   if(totalStartDaysShift == totalEndDaysShift){
		      if(daysAgo == totalEndDaysShift && hhmm < endHHMM && hhmm >= startHHMM) {
      		   if(type == 2){
      		      highest = MathMax(highest, High[currentIndex]);
      		   }
      		   else {
      		      lowest = MathMin(lowest, Low[currentIndex]);
      		   }
      		}
		   }
		   else {
		      if((daysAgo == totalEndDaysShift && hhmm < endHHMM) || (daysAgo == totalStartDaysShift && hhmm >= startHHMM)) {
      		   if(type == 2){
      		      highest = MathMax(highest, High[currentIndex]);
      		   }
      		   else {
      		      lowest = MathMin(lowest, Low[currentIndex]);
      		   }
      		}
		   }
		}
		
		if(daysAgo > totalStartDaysShift || (daysAgo == totalStartDaysShift && ((type < 4 && hhmm <= startHHMM) || (type == 4 && hhmm < endHHMM)))) {
		   switch(type){
		      case 1: 
		         return Open[currentIndex];
		      case 2: return highest != -1000000000 ? highest : 0;
		      case 3: return lowest != 1000000000 ? lowest : 0;
		      case 4:
		         return Close[currentIndex];
		      default: return 0;
		   }
		}
		
		currentIndex++;
   }
   
   switch(type){
      case 2: return highest != -1000000000 ? highest : 0;
      case 3: return lowest != 1000000000 ? lowest : 0;
      default: return 0;
   }
}