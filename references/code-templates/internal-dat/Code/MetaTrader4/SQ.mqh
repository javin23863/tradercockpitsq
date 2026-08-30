//+------------------------------------------------------------------+

void sqInitStart() {
   // changed recognition of bar open to support also range/renko charts
   if(_sqLastOpenBarTime == 0) {
      _sqLastOpenBarTime = Time[0];
      _sqIsBarOpen = true;
   } 
   else {
      if(_sqLastOpenBarTime != Time[0]) {
        bool processBarOpen = true;

        if(OpenBarDelay > 0) {
           // set bar to open after X minutes from real open
           processBarOpen = false;
  
           long diffInSeconds = TimeCurrent() - Time[0];
           if(diffInSeconds >= OpenBarDelay * 60) {
              processBarOpen = true;
           }
        }
  
        if(processBarOpen) {
           _sqIsBarOpen = true;
           _sqLastOpenBarTime = Time[0];      
        } 
      } 
      else {
         _sqIsBarOpen = false;
      }
   }

   if(_sqIsBarOpen && Bars<30) {
      Print("NOT ENOUGH DATA: Less Bars than 30");
      return;
   }

   if(!IsTesting() && !IsOptimization()) {
      sqTextFillOpens();
      if(_sqIsBarOpen) {
         sqTextFillTotals();
      }
   }
}

//+------------------------------------------------------------------+

bool sqIsBarOpen() {
   return(_sqIsBarOpen);
}

//+------------------------------------------------------------------+

int sqGetOrderExpiration(int ticket) {
   return (int) sqGetGlobalVariable(ticket, "sqOrderExpiration");
}

//+------------------------------------------------------------------+

void sqSetOrderExpiration(int ticket, int bars) {
   sqSetGlobalVariable(ticket, "sqOrderExpiration", bars);
}

//+------------------------------------------------------------------+

void sqManageOrderExpiration(int ticket) {
   int tempValue = 0;                                                           
   int barsOpen = 0;

   // Stop/Limit Order Expiration
   if(OrderType() != OP_BUY && OrderType() != OP_SELL) {
      // handle only pending orders
      tempValue = sqGetOrderExpiration(ticket);
      if(tempValue > 0) {
         barsOpen = sqGetOpenBarsForOrder(ticket, tempValue+10);
         if(barsOpen >= tempValue) {
            Verbose("Order with ticket: ", IntegerToString(ticket), " expired");
            sqDeletePendingOrder(ticket);
         }
      }
   }
}

//----------------------------------------------------------------------------

double sqIndicatorHighest(int period, int nthValue, string indicatorIdentification) {     
   if(period > 1000) {
        Alert("Period used for sqIndicatorHighest function is too high. Max value is 1000");
        period = 1000;
   }
   
   if(nthValue < 0 || nthValue >= period) {
	   return(-1);
   }
   
   double indicatorValues[1000];
   int i;

   for(i=0; i<1000; i++) {
      indicatorValues[i] = -2147483647;
   }

   for(i=0; i<period; i++) {
      indicatorValues[i] = sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   ArraySort(indicatorValues, WHOLE_ARRAY,0,MODE_DESCEND);

   if(nthValue < 0 || nthValue >= period) {
      return(-1);
   }

   return(indicatorValues[nthValue]);
}

//----------------------------------------------------------------------------

double sqIndicatorLowest(int period, int nthValue, string indicatorIdentification) {           
   if(period > 1000) {
        Alert("Period used for sqIndicatorLowest function is too high. Max value is 1000");
        period = 1000;
   }
   
   if(nthValue < 0 || nthValue >= period) {
	   return(-1);
   }
   
   double indicatorValues[1000];
   int i;

   for(i=0; i<1000; i++) {
      indicatorValues[i] = 2147483647;
   }

   for(i=0; i<period; i++) {
      indicatorValues[i] = sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   ArraySort(indicatorValues,WHOLE_ARRAY,0,MODE_ASCEND);

   if(nthValue < 0 || nthValue >= period) {
      return(-1);
   }

   return(indicatorValues[nthValue]);
}

//----------------------------------------------------------------------------

double sqIndicatorAverage(int period, int maMethod, string indicatorIdentification) {
   double indicatorValues[10000];

   for(int i=0; i<period; i++) {
      indicatorValues[i] = sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   double maValue = iMAOnArray(indicatorValues, period, period, 0, maMethod, 0);

   return(maValue);
}


//----------------------------------------------------------------------------

double sqIndicatorRecent(int barsBack, string indicatorIdentification) {
   return(sqGetIndicatorByIdentification(indicatorIdentification, barsBack));
}

//----------------------------------------------------------------------------

bool sqIsRising(string indicatorIdentification, int bars, bool allowSameValues, int shift) {
   bool atLeastOnce = false;

   double previousValue = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1), 6);

   for(int i=1; i<bars; i++) {
      double currentValue = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1-i), 6);

      if(currentValue < previousValue) {
         // indicator was falling
         return(false);
      }
      if(currentValue == previousValue && allowSameValues == false) {
         // indicator was the same, not allowed
         return(false);
      }
      if(currentValue > previousValue) {
         atLeastOnce = true;
      }

      previousValue = currentValue;
   }

   if(atLeastOnce) {
      return(true);
   }

   // indicator was not rising once
   return(false);
}

//----------------------------------------------------------------------------

bool sqIsFalling(string indicatorIdentification, int bars, bool allowSameValues, int shift) {
   bool atLeastOnce = false;

   double previousValue = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1), 6);

   for(int i=1; i<bars; i++) {
      double currentValue = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1-i), 6);

      if(currentValue > previousValue) {
         // indicator was rising
         return(false);
      }
      if(currentValue == previousValue && allowSameValues == false) {
         // indicator was the same, not allowed
         return(false);
      }
      if(currentValue < previousValue) {
         atLeastOnce = true;
      }

      previousValue = currentValue;
   }

   if(atLeastOnce) {
      return(true);
   }

   // indicator was not falling once
   return(false);
}

//+------------------------------------------------------------------+

string getVariablePrefix(){
   return IsTesting() ? "SQX(Test)" : "SQX";
}

//+------------------------------------------------------------------+

void sqSetGlobalVariable(int ticket, string name, double value) {
   GlobalVariableSet(getVariablePrefix()+"_"+IntegerToString(ticket)+"_"+name, value);
}

//+------------------------------------------------------------------+

double sqGetGlobalVariable(int ticket, string name) {
   return (GlobalVariableGet(getVariablePrefix()+"_"+IntegerToString(ticket)+"_"+name));
}

//+------------------------------------------------------------------+

int sqStringHash(string str){
   int i, h=0, k;
   for (i=0; i<StringLen(str); i++){
      k = StringGetChar(str, i);
      h = (h << 5) + h + k;
   }
   return(h);
}

//+------------------------------------------------------------------+

void sqClosePosition(double size, int magicNumber, string symbol, int direction, string comment) {
   Verbose("Closing order with Magic Number: ", IntegerToString(magicNumber), ", symbol: ", symbol, ", direction: ", IntegerToString(direction), ", comment: ", comment);

   if(!sqSelectOrder(magicNumber, symbol, direction, comment, false, false)) {
      Verbose("Order cannot be found");
   } else {
      if(OrderType() == OP_BUY || OrderType() == OP_SELL) {
         sqClosePositionAtMarket(size == CLOSE_ALL ? OrderLots() : size);
      } else {
         sqDeletePendingOrder(OrderTicket());
      }
   }

   Verbose("Closing order finished ----------------");
}

//+------------------------------------------------------------------+

void sqClosePendingOrder(int magicNumber, string symbol, int direction, string comment) {
   Verbose("Closing pending order with Magic Number: ", IntegerToString(magicNumber), ", symbol: ", symbol, ", direction: ", IntegerToString(direction), ", comment: ", comment);

   if(!sqSelectOrder(magicNumber, symbol, direction, comment, false, false, true)) {
      Verbose("Order cannot be found");
   } else {
      sqDeletePendingOrder(OrderTicket());
   }

   Verbose("Closing pending order finished ----------------");
}

//+------------------------------------------------------------------+

void sqCloseAllPendingOrders(int magicNumber, string symbol, int direction, string comment) {
   Verbose("Closing pending orders with Magic Number: ", IntegerToString(magicNumber), ", symbol: ", symbol, ", direction: ", IntegerToString(direction), ", comment: ", comment);

   while(sqSelectOrder(magicNumber, symbol, direction, comment, false, false, true)) {
      sqDeletePendingOrder(OrderTicket());
   }

   Verbose("Closing pending orders finished ----------------");
}

//+------------------------------------------------------------------+

int sqGetOpenBarsForOrder(int ticket, int expBarsPeriod) {
   datetime opTime;

   if (OrderType() == OP_BUY || OrderType() == OP_SELL) {             //live order
       opTime = OrderOpenTime();
   }
   else {                                                         //pending order
       opTime = (datetime) sqGetGlobalVariable(ticket, "sqOrderOpenTime");
   }

   int numberOfBars = 0;
   int limit = MathMin(expBarsPeriod + 10, Bars);

   for(int i=0; i<limit; i++) {
      if(opTime < Time[i]) {
         numberOfBars++;
      }
   }

   return(numberOfBars);
}

//+------------------------------------------------------------------+

void Verbose(string st1, string st2="", string s3="", string s4="", string s5="", string s6="", string s7="", string s8="", string s9="", string s10="", string s11="", string s12="", string s13="", string s14="" ) {
   if(sqVerboseMode == 0) {
      return;
   } 
    
   if(sqVerboseMode == 1 && (IsTesting() || IsOptimization())) {
      return;
   }
   
   Print("- SQ LOG ", TimeToStr(TimeCurrent()), " ", st1, st2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14);
}


//+------------------------------------------------------------------+

void VerboseLog(string s1, string s2="", string s3="", string s4="", string s5="", string s6="", string s7="", string s8="", string s9="", string s10="", string s11="", string s12="" ) {
   if(sqVerboseMode != 1) {
      Log(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
   }

   Verbose(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
}

//+------------------------------------------------------------------+

void Log(string s1, string s2="", string s3="", string s4="", string s5="", string s6="", string s7="", string s8="", string s9="", string s10="", string s11="", string s12="" ) {
   Print(TimeToStr(TimeCurrent()), " ", s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
}

//+------------------------------------------------------------------+

void sqLog(string st1, string st2="", string s3="", string s4="", string s5="", string s6="", string s7="", string s8="", string s9="", string s10="", string s11="", string s12="" ) {
   Print(TimeToStr(TimeCurrent()), " ", st1, st2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
}


//+------------------------------------------------------------------+

void sqLogToFile(string fileName, string st1, string st2="", string s3="", string s4="", string s5="", string s6="", string s7="", string s8="", string s9="", string s10="", string s11="", string s12="" ) {
   int handle = FileOpen(fileName, FILE_READ | FILE_WRITE, ";");
   if(handle>0) {
      FileSeek(handle,0,SEEK_END);
      FileWrite(handle, TimeToStr(TimeCurrent()), " ", st1, st2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
      FileClose(handle);
   }
}

//+------------------------------------------------------------------+

bool sqSelectOrder(int magicNumber, string symbol, int direction, string comment, bool goFromNewest=true, bool skipPending=true, bool skipFilled=false) {
    int cc = 0;
    
    if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
    }
    
    if(goFromNewest){
       for (cc = OrdersTotal() - 1; cc >= 0; cc--) {
           if(orderFits(cc, magicNumber, symbol, direction, comment, skipPending, skipFilled)){
              return true;
           }
       }
    }
    else {
       for (cc = 0; cc < OrdersTotal(); cc++) {
           if(orderFits(cc, magicNumber, symbol, direction, comment, skipPending, skipFilled)){
              return true;
           }
       }
    }
    return(false);
}

//+------------------------------------------------------------------+

bool orderFits(int index, int magicNumber, string symbol, int direction, string comment, bool skipPending=true, bool skipFilled=false){
    if (OrderSelect(index, SELECT_BY_POS)) {
       // skip pending orders
       if(skipPending && (OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP || OrderType() == OP_SELLLIMIT)) {
          return false;
       }
       // skip filled orders
       if(skipFilled && (OrderType() == OP_BUY || OrderType() == OP_SELL)) {
          return false;
       }

       if(direction != 0) {
          if(direction > 0 && (OrderType() != OP_BUY && OrderType() != OP_BUYSTOP && OrderType() != OP_BUYLIMIT)) return false;
          if(direction < 0 && (OrderType() != OP_SELL && OrderType() != OP_SELLSTOP && OrderType() != OP_SELLLIMIT)) return false;
       }

       if(magicNumber != 0) {
          if(!checkMagicNumber(OrderMagicNumber()) || OrderMagicNumber() != magicNumber) return false;
       }

       if(symbol != "Any") {
          if(OrderSymbol() != correctSymbol(symbol)) return false;
       }

     if(comment != "") {
       if(StringFind(OrderComment(), comment) == -1) return false;
     }

     // otherwise we found the order
     return(true);
   }
   else return false;
}

//+------------------------------------------------------------------+

void sqCloseAllPositions(string symbol, int magicNumber, int direction, string comment) {
   int count = 100; // maximum number of positions to close
   int lastTicket = -1;

   while(count > 0) {
      count--;
      if(!sqSelectOrder(magicNumber, symbol, direction, comment, false, false)) {
         // no position found
         break;
      }

      if(lastTicket == OrderTicket()) {
         // trying to close the same position one more time, there must be some error
         break;
      }
      lastTicket = OrderTicket();

      if(OrderType() == OP_BUY || OrderType() == OP_SELL) {
         sqClosePositionAtMarket(OrderLots());
      } else {
         sqDeletePendingOrder(OrderTicket());
      }
   }
}

//+------------------------------------------------------------------+

void sqCloseBestPosition(string symbol, int magicNumber, int direction, string comment) {
   double maxPL = -100000000;
   int ticket = 0;
   
   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }
         else if(!checkMagicNumber(OrderMagicNumber())) continue;
         
         if(OrderProfit() > maxPL) {
            // found order with better profit
            maxPL = OrderProfit();
            ticket = OrderTicket();
            Verbose("Better position found, ticket: ", IntegerToString(ticket),", PL: ", DoubleToString(maxPL));
         }
      }
   }

   if(ticket > 0) {
      if(OrderSelect(ticket, SELECT_BY_TICKET)) {
         sqClosePositionAtMarket(OrderLots());
      }
   }
}

//+------------------------------------------------------------------+

void sqCloseWorstPosition(string symbol, int magicNumber, int direction, string comment) {
   double minPL = 100000000;
   int ticket = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }  
         else if(!checkMagicNumber(OrderMagicNumber())) continue;
         
         if(OrderProfit() < minPL) {
            // found order with worse profit
            minPL = OrderProfit();
            ticket = OrderTicket();
            Verbose("Worse position found, ticket: ", IntegerToString(ticket),", PL: ", DoubleToString(minPL));
         }
      }
   }

   if(ticket > 0) {
      if(OrderSelect(ticket, SELECT_BY_TICKET)) {
         sqClosePositionAtMarket(OrderLots());
      }
   }
}

//+------------------------------------------------------------------+

int sqGetMarketPosition(string symbol, int magicNumber, string comment) {
   if(sqSelectOrder(magicNumber, symbol, 0, comment, false)) {
      if(OrderType() == OP_BUY) {
         return(1);
      } else {
         return(-1);
      }
   }
   return(0);
}                     

//+------------------------------------------------------------------+

bool sqMarketPositionIsShort(int magicNo, string symbol, string comment){
   return sqSelectOrder(magicNo, symbol, -1, comment, false);
}    

//+------------------------------------------------------------------+

bool sqMarketPositionIsNotShort(int magicNo, string symbol, string comment){
   if(sqSelectOrder(magicNo, symbol, -1, comment, false)) {
      return false; 	
	 }
	 
	 return true;
}     

//+------------------------------------------------------------------+

bool sqMarketPositionIsLong(int magicNo, string symbol, string comment){
   return sqSelectOrder(magicNo, symbol, 1, comment, false);
}     

//+------------------------------------------------------------------+

bool sqMarketPositionIsNotLong(int magicNo, string symbol, string comment){
   if(sqSelectOrder(magicNo, symbol, 1, comment, false)) {
      return false; 	
	 }
	 
	 return true;
}     

//+------------------------------------------------------------------+

bool sqMarketPositionIsFlat(int magicNo, string symbol, string comment){
   return sqGetMarketPosition(symbol, magicNo, comment) == 0;
}

//+------------------------------------------------------------------+

double sqGetOrderOpenPrice(string symbol, int magicNumber, int direction, string comment) {
   if(sqSelectOrder(magicNumber, symbol, direction, comment, false)) {
      return(OrderOpenPrice());
   }
   return(-1);
}

//+------------------------------------------------------------------+

double sqGetOrderStopLoss(string symbol, int magicNumber, int direction, string comment) {
   if(sqSelectOrder(magicNumber, symbol, direction, comment, false)) {
      return(OrderStopLoss());
   }
   return(-1);
}

//+------------------------------------------------------------------+

double sqGetOrderProfitTarget(string symbol, int magicNumber, int direction, string comment) {
   if(sqSelectOrder(magicNumber, symbol, direction, comment, false)) {
      return(OrderTakeProfit());
   }
   return(-1);
}

//+------------------------------------------------------------------+

double sqGetMarketPositionSize(string symbol, int magicNumber, int direction, string comment) {
   double lots = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         } 
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         lots += OrderLots();
      }
   }

   return(lots);
}

//+------------------------------------------------------------------+

double sqGetOpenPL(string symbol, int magicNumber, int direction, string comment) {
   double pl = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }    
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         pl += OrderProfit();
      }
   }

   return(pl);
}

//+------------------------------------------------------------------+

double sqGetOpenPLInPips(string symbol, int magicNumber, int direction, string comment) {
   double pl = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }  
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         if(OrderType() == OP_BUY) {
            pl += sqGetBid(OrderSymbol()) - OrderOpenPrice();
         } else {
            pl += OrderOpenPrice() - sqGetAsk(OrderSymbol());
         }
      }
   }

   return(sqConvertToPips(OrderSymbol(), pl));
}

//+------------------------------------------------------------------+

double sqGetClosedPLInPips(string symbol, int magicNumber, int direction, string comment, int shift) {
   int index = 0;

   for(int i=OrdersHistoryTotal(); i>=0; i--) {
      if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true ) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }     
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         if(index == shift) {
            if(OrderType() == OP_BUY) {
               return(sqConvertToPips(OrderSymbol(), OrderClosePrice() - OrderOpenPrice()));
            } else {
               return(sqConvertToPips(OrderSymbol(), OrderOpenPrice() - OrderClosePrice()));
            }
         }

         index++;
      }
   }

   return(0);
}

//+------------------------------------------------------------------+

double sqGetClosedPLInMoney(string symbol, int magicNumber, int direction, string comment, int shift) {
   int index = 0;

   for(int i=OrdersHistoryTotal(); i>=0; i--) {
      if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true ) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }      
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         if(index == shift) {
            return(OrderProfit());
         }

         index++;
      }
   }

   return(0);
}

//+------------------------------------------------------------------+

int sqGetMarketPositionCount(string symbol, int magicNumber, int direction, string comment) {
   int count = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP ||OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }   
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         count++;
      }
   }

   return(count);
}

//+------------------------------------------------------------------+

int sqGetBarsSinceOpen(string symbol, int magicNumber, int direction, string comment) {
   if(sqSelectOrder(magicNumber, symbol, direction, comment, false, false)) {
      datetime opTime = OrderOpenTime();

      int numberOfBars = 0;
      int limit = MathMin(10000, Bars);

      for(int i=0; i<limit; i++) {
         if(opTime < Time[i]) {
            numberOfBars++;
         }
      }

      return(numberOfBars);
   }

   return(-1);
}

//+------------------------------------------------------------------+

int sqGetBarsSinceClose(string symbol, int magicNumber, int direction, string comment) {
   if(sqSelectOrderInHistory(magicNumber, symbol, direction, comment)) {
      datetime clTime = OrderCloseTime();

      int numberOfBars = 0;
      int limit = MathMin(10000, Bars);

      for(int i=0; i<limit; i++) {
         if(clTime < Time[i]) {
            numberOfBars++;
         }
      }

      return(numberOfBars);
   }

   return(-1);
}


//+------------------------------------------------------------------+

int sqGetLastOrderType(string symbol, int magicNumber, string comment) {
   if(sqSelectOrderInHistory(magicNumber, symbol, 0, comment)) {
      if(OrderType() == OP_BUY || OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT) {
         return(1);
      } else {
         return(-1);
      }
   }

   return(0);
}

//+------------------------------------------------------------------+

bool sqSelectOrderInHistory(int magicNumber, string symbol, int direction, string comment) {
   for (int cc = OrdersHistoryTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS,MODE_HISTORY)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP || OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && OrderType() != OP_BUY) continue;
            if(direction < 0 && OrderType() != OP_SELL) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }   
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

       if(symbol != "Any") {
         if(OrderSymbol() != correctSymbol(symbol)) continue;
       }

       if(comment != "") {
         if(StringFind(OrderComment(), comment) == -1) continue;
       }

       // otherwise we found the order
       return(true);
     }
   }

   return(false);
}

//+------------------------------------------------------------------+

bool sqSelectPendingOrderByType(int magicNumber, string symbol, int orderType, string comment) {
   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {
         if(OrderType() == OP_BUY || OrderType() == OP_SELL) {
            continue;
         }
         
         if(orderType != 0) {
            if(OrderType() != orderType) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }   
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
            if(StringFind(OrderComment(), comment) == -1) continue;
         }

         // otherwise we found the order
         return(true);
      }
   }

   return(false);
}

//+------------------------------------------------------------------+

bool sqSelectPendingOrderByDir(int magicNumber, string symbol, int direction, string comment) {
   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS)) {
         if(OrderType() == OP_BUY || OrderType() == OP_SELL) {
            continue;
         }
       
         if(direction != 0) {
            int orderDirection = sqGetDirectionFromOrderType(OrderType());
            if(orderDirection != direction) continue;
         }

         if(magicNumber != 0) {
            if(OrderMagicNumber() != magicNumber) continue;
         }     
         else if(!checkMagicNumber(OrderMagicNumber())) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         if(comment != "") {
           if(StringFind(OrderComment(), comment) == -1) continue;
         }

         // otherwise we found the order
         return(true);
      }
   }

   return(false);
}
//+------------------------------------------------------------------+

bool sqClosePositionAtMarket(double size) {
   int ticket = OrderTicket();

   Verbose("Closing order with ticket: ", IntegerToString(ticket));

   int orderType = OrderType();

   if(orderType != OP_BUY && orderType != OP_SELL) {
      Verbose("Trying to close non-live order");
      return(false);
   }
   if(!sqCheckConnected()) {
      return(false);
   }

   bool result;

   double price = sqGetClosePrice(orderType, OrderSymbol(), 0);
   result = OrderCloseReliable(ticket, size, price, correctSlippage(sqMaxCloseSlippage, OrderSymbol()));
   if(result) {
      Verbose("Order deleted successfuly");
      return(true);
   }

   return(false);
}

//+------------------------------------------------------------------+

double sqGetAsk(string symbol) {
   if(symbol == "NULL" || symbol == "Current") {
     return(NormalizeDouble(Ask, Digits));
   } else {
     return(NormalizeDouble(MarketInfo(correctSymbol(symbol),MODE_ASK), Digits));
   }
}

//+------------------------------------------------------------------+

double sqGetBid(string symbol) {
   if(symbol == "NULL" || symbol == "Current") {
     return(NormalizeDouble(Bid, Digits));
   } else {
     return(NormalizeDouble(MarketInfo(correctSymbol(symbol),MODE_BID), Digits));
   }
}

//+------------------------------------------------------------------+

bool sqDeleteOrder(int ticket){
   if(!OrderSelect(ticket, SELECT_BY_TICKET)){
      Verbose("Warning! Cannot delete order - order with ticket ", IntegerToString(ticket), " can't be selected");
      return false;
   }

   if(sqIsPendingOrder(OrderType())){
      return OrderDeleteReliable(ticket);
   }
   else {
      return OrderCloseReliableMKT(ticket, OrderLots(), sqGetDirectionFromOrderType(OrderType()) == 1 ? Bid : Ask, correctSlippage(0, OrderSymbol()));
   }
}

//+------------------------------------------------------------------+

bool sqDeletePendingOrder(int ticket) {
   Verbose(" Deleting pending order, ticket: " + IntegerToString(ticket));

   int orderType = OrderType();

   if(orderType == OP_BUY || orderType == OP_SELL) {
      Verbose("Trying to delete non-pending order");
      return(false);
   }
   if(!sqCheckConnected()) {
      return(false);
   }
         
   bool result = OrderDeleteReliable(ticket);
   if(result) {
      Verbose("Order deleted successfuly");
      return(true);
   }
   
   return(false);
}

//+------------------------------------------------------------------+

int sqOpenOrder(int orderType, string symbol, double size, double price, int magicNumber, string comment, datetime expirationInTime, bool replaceExisting, bool allowDuplicateTrades, color arrowColor, const bool isExitLevel = false) {
   if(size <= 0) return (0);

   string correctedSymbol = correctSymbol(symbol);

   if(isMarketClosed(correctedSymbol)){
      return 0;
   }

   double ask = sqGetAsk(correctedSymbol);
   double bid = sqGetBid(correctedSymbol);
   
   int direction = sqGetDirectionFromOrderType(orderType);
   
   double marketPrice = direction == 1 ? ask : bid;

   price = (orderType == OP_BUY || orderType == OP_SELL) ? marketPrice : price;               
   price = sqFixMarketPrice(price, correctedSymbol);

   <#if tradingOptionExists("MaxDistanceFromMarket", "MaxDistanceFromMarket") >
   if(LimitMaxDistanceFromMarketPrice && (orderType != OP_BUY && orderType != OP_SELL)){
      double pctDistance = NormalizeDouble(MathAbs(price-marketPrice) / marketPrice * 100, 2);
      if(pctDistance > NormalizeDouble(MaxDistanceFromMarketPct, 2)){
         Verbose("Order skipped - too far from market price. Open price: ", DoubleToStr(price), ", Market price: ", DoubleToStr(marketPrice), ", Max distance: ", DoubleToStr(MaxDistanceFromMarketPct), "%"); 
         return(0);  
      }
   }
   </#if>
   
   openingOrdersAllowed = openingOrdersAllowed && sqHandleTradingOptions();
   
   if(!openingOrdersAllowed) return(0);                                                        

   Verbose("Opening order type ", OrderTypeToString(orderType)," with price ", DoubleToString(price), ". Current market prices: ", DoubleToString(ask), " / ", DoubleToString(bid));

   // check if live order exists
   if(!isExitLevel && sqSelectOrder(magicNumber, correctedSymbol, 0, comment) && !allowDuplicateTrades) {
      Verbose("Order with these parameters already exists and duplicate trades are not allowed. Canceling order...");
      Verbose("----------------------------------");
      return(0);
   }

   // check if pending order exists
   if(!isExitLevel){
      while(sqSelectOrder(magicNumber, correctedSymbol, direction, comment, false, false, true)) {
         if(replaceExisting) {
         
            if(!strategyUsesATM && ModifyInsteadOfReplacing && size == OrderLots()) {
               // modify existing pending order
               if(OrderModifyReliable(OrderTicket(), price, 0, 0, expirationInTime)) {
                  // reset global variables for this order
                  sqResetGlobalVariablesForTicket(OrderTicket());
                  return ( OrderTicket() );
               
               } else {
                  Verbose("Modifying order failed, deleting it");
                  sqDeletePendingOrder(OrderTicket());
                  return(0);
               }
            } else {
               // delete existing pending order
               sqDeletePendingOrder(OrderTicket());
            }
   
         } else {
            Verbose("Pending Order with these parameters already exists, and replace is not allowed. Canceling order...", " ----------------");
            return(0);
         }
      }
   }

   if(!checkOrderPriceValid(orderType, correctedSymbol, price, marketPrice)){
      return 0;
   }

   //check free margin
   if(AccountFreeMarginCheck(correctedSymbol, isLongOrder(orderType) ? OP_BUY : OP_SELL, size) <= 0 || GetLastError()==134) {
      Alert("Balance too low for trading!");
      return(0);
	}

   string commentToUse = "";
   if(comment != ""){
      commentToUse = comment;
   }
   else {
      commentToUse = CustomComment;
   }

   int ticket = OrderSendReliable(correctedSymbol, orderType, size, price, correctSlippage(sqMaxEntrySlippage, correctedSymbol), 0, 0, commentToUse, magicNumber, expirationInTime, arrowColor);

   if(ticket > 0) {
      // reset global variables for this order
      sqResetGlobalVariablesForTicket(ticket);
   }

	 return(ticket);
}

//+------------------------------------------------------------------+

bool isLongOrder(int orderType){
   return orderType == OP_BUY || orderType == OP_BUYLIMIT || orderType == OP_BUYSTOP;
}

//----------------------------------------------------------------------------

int correctSlippage(int slippage, string symbol = NULL){
    if(slippage <= 0) return 100000;
    
    if(autoCorrectMaxSlippage){
       int realDigits = (int) MarketInfo(correctSymbol(symbol), MODE_DIGITS);
       if(realDigits > 0 && realDigits != 2 && realDigits != 4) {
          return slippage * 10;
       }
    }
    
    return slippage;
}

//----------------------------------------------------------------------------

bool checkOrderPriceValid(int orderType, string symbol, double price, double marketPrice){
   if(orderType == OP_BUY || orderType == OP_SELL){
      if(marketPrice == price){
         return true;
      }
      else {
         Verbose("Based on its logic, the strategy tried to place order a market order at incorrect price. Market price: ", DoubleToString(marketPrice), ", order price: ", DoubleToString(price), " (this is NOT an error)");
         return false;
      }
   }
   
   return checkStopPriceValid(orderType, symbol, price, marketPrice, "stop/limit order");
}

//----------------------------------------------------------------------------

bool checkStopPriceValid(int orderType, string symbol, double price, double marketPrice, string name){
   int stopLevel = (int) MarketInfo(symbol, MODE_STOPLEVEL);
   double point = MarketInfo(symbol, MODE_POINT);
   double minDistance = point * stopLevel;
   double minDistanceSQ = sqMinDistance * sqGetPointCoef(symbol);
   
   if(minDistanceSQ > minDistance){
      minDistance = minDistanceSQ;
   }
   
   double priceLevel;
   
   if(orderType == OP_BUYLIMIT || orderType == OP_SELLSTOP){
      priceLevel = marketPrice - minDistance;
      
      if(price <= priceLevel){
         return true;
      }
      else {
         Verbose("Based on its logic, the strategy tried to place ", name, " at incorrect price. Min Distance: ", DoubleToString(minDistance), " Market price: ", DoubleToString(marketPrice), ", max. price allowed: ", DoubleToString(priceLevel), ", ", name, " price: ", DoubleToString(price), " (this is NOT an error)");
         return false;
      }
   }
   else if(orderType == OP_BUYSTOP || orderType == OP_SELLLIMIT){
      priceLevel = marketPrice + minDistance;
      
      if(price >= priceLevel){
         return true;
      }
      else {
         Verbose("Based on its logic, the strategy tried to place ", name, " at incorrect price. Min Distance: ", DoubleToString(minDistance), " Market price: ", DoubleToString(marketPrice), ", min. price allowed: ", DoubleToString(priceLevel), ", ", name," price: ", DoubleToString(price), " (this is NOT an error)");
         return false;
      }
   }
   else return true;
}   

//+------------------------------------------------------------------+

double Luminance(color col)
{
   int r = ColR(col);
   int g = ColG(col);
   int b = ColB(col);
   return 0.2126 * r + 0.7152 * g + 0.0722 * b; // 0..255
}

//+------------------------------------------------------------------+

bool IsLightBackground(double threshold = 140.0)
{
   long bg_l = ChartGetInteger(0, CHART_COLOR_BACKGROUND);
   if(bg_l < 0) return false; // conservative fallback
   color bg = (color)bg_l;
   return (Luminance(bg) >= threshold);
}

//+------------------------------------------------------------------+

color GetAdaptiveTextColor()
{
   return IsLightBackground() ? TextColorLightBackground : TextColorDarkBackground;
}

//+------------------------------------------------------------------+

long GetChartBg()
{
   return ChartGetInteger(0, CHART_COLOR_BACKGROUND);
}

//+------------------------------------------------------------------+

void OnBgChanged(long oldBg, long newBg)
{
   if(sqDisplayInfoPanel) {
     sqInitInfoPanel();
   }
}

//+------------------------------------------------------------------+

void CheckBgChange()
{
   long bg = GetChartBg();
   if(bg < 0) return;

   if(bg != g_lastBg)
   {
      OnBgChanged(g_lastBg, bg);
      g_lastBg = bg;
   }
}

//+------------------------------------------------------------------+

void OnChartEvent(const int id,
                  const long &lparam,
                  const double &dparam,
                  const string &sparam)
{
   // Not always triggered for theme changes in MT4, but harmless and useful
   if(id == CHARTEVENT_CHART_CHANGE)
      CheckBgChange();
   else if(id == CHARTEVENT_OBJECT_CLICK) {
      if(sparam == "sqBtnLong") {
         LongEnabled = !LongEnabled;
         sqUpdateDirectionButtons();
         ObjectSetInteger(0, "sqBtnLong", OBJPROP_STATE, false);
         ChartRedraw();
      }
      else if(sparam == "sqBtnShort") {
         ShortEnabled = !ShortEnabled;
         sqUpdateDirectionButtons();
         ObjectSetInteger(0, "sqBtnShort", OBJPROP_STATE, false);
         ChartRedraw();
      }
   }
}

//+------------------------------------------------------------------+

int SqCornerToMT4(ENUM_SQ_CORNER c)
{
   switch(c)
   {
      case SQ_LEFT_UPPER:  return CORNER_LEFT_UPPER;
      case SQ_RIGHT_UPPER: return CORNER_RIGHT_UPPER;
      case SQ_LEFT_LOWER:  return CORNER_LEFT_LOWER;
      case SQ_RIGHT_LOWER: return CORNER_RIGHT_LOWER;
   }
   return CORNER_LEFT_UPPER;
}

//+------------------------------------------------------------------+

int sqGetDirectionFromOrderType(int orderType) {
   if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
      return(1);
   } else {
      return(-1);
   }
}

//+------------------------------------------------------------------+

bool sqIsPendingOrder(int orderType) {
   if(orderType != OP_BUY && orderType != OP_SELL) {
      return(true);
   }
   return(false);
}

//+------------------------------------------------------------------+

void sqSetSLandPT(int ticket, double sl, double pt) {
   if(sl == 0 && pt == 0) return;
   
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      Verbose("Cannot select order with ticket: ",IntegerToString(ticket));
      return;
   }
   
   if(sl == OrderOpenPrice()) {
      Verbose("SL is the same as order price, cannot set it, so we'll delete the order!");
      if(!sqDeleteOrder(ticket)) {
         Verbose("Warning! Cannot delete order and SL/PT was not set! Error: ", IntegerToString(GetLastError()));
      }

      return;
   }

   if(pt == OrderOpenPrice()) {
      pt = 0;
   }
   
   if(sl > 0 || pt > 0) {
      bool result = sqOrderModify(ticket, OrderOpenPrice(), sqFixMarketPrice(sl, OrderSymbol()), sqFixMarketPrice(pt, OrderSymbol()));
      if(!result) {
         Verbose("Cannot set SL / PT for this order, deleting it!");
         if(!sqDeleteOrder(ticket)){
            Verbose("Warning! Cannot delete order and SL/PT was not set! Error: ", IntegerToString(GetLastError()));
         }
      }
   }
}

//+------------------------------------------------------------------+

bool sqOrderModifySL(int ticket,double stopLoss, int type) {
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      Verbose("Cannot select order with ticket: ",IntegerToString(ticket));
   }

   if(type == SLPTTYPE_RANGE) {
      // convert range to price level
      if(sqGetDirectionFromOrderType(OrderType()) == 1) {
         // it is long order
         stopLoss = OrderOpenPrice() - stopLoss;
      } else {
         stopLoss = OrderOpenPrice() + stopLoss;
      }
   }

   return(sqOrderModify(ticket, OrderOpenPrice(), sqFixMarketPrice(stopLoss, OrderSymbol()), OrderTakeProfit()));
}

//+------------------------------------------------------------------+

bool sqOrderModifyPT(int ticket,double profitTarget, int type) {
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      Verbose("Cannot select order with ticket: ",IntegerToString(ticket));
   }

   if(type == SLPTTYPE_RANGE) {
      // convert range to price level
      if(sqGetDirectionFromOrderType(OrderType()) == 1) {
         // it is long order
         profitTarget = OrderOpenPrice() + profitTarget;
      } else {
         profitTarget = OrderOpenPrice() - profitTarget;
      }
   }

   return(sqOrderModify(ticket, OrderOpenPrice(), OrderStopLoss(), sqFixMarketPrice(profitTarget, OrderSymbol())));
}

//+------------------------------------------------------------------+

bool sqOrderModify(int ticket, double price, double stopLoss, double profitTarget) {
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      Verbose("Cannot select order with ticket: ",IntegerToString(ticket));
   }

   int digits = (int) MarketInfo(OrderSymbol(), MODE_DIGITS);
   if (digits > 0) {
      stopLoss = NormalizeDouble(stopLoss, digits);
      profitTarget = NormalizeDouble(profitTarget, digits);
	}

   Verbose("Modifying order with ticket: ", IntegerToString(ticket), ", SL: ", DoubleToString(stopLoss), " and PT: ", DoubleToString(profitTarget));

   int orderType = OrderType();

   if(!sqCheckConnected()) {
      return(false);
   }

   bool result;
   double closestPossibleSL, closestPossiblePT;

   double point = MarketInfo(OrderSymbol(), MODE_POINT);
   double minStopLevel = MarketInfo(OrderSymbol(),MODE_STOPLEVEL);
   
   if(stopLoss > 0) {
      // check if SL isn't too close to price
      if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
         if(orderType == OP_BUY) {
            closestPossibleSL = NormalizeDouble(Bid-minStopLevel*point, digits);
         } else {
            closestPossibleSL = NormalizeDouble(OrderOpenPrice()-minStopLevel*point, digits);
         }

         if(stopLoss > closestPossibleSL) {
            Verbose("Cannot modify SL, it cannot be closer than minimum allowed: ", DoubleToString(stopLoss), " > ", DoubleToString(closestPossibleSL));
            return(false);
         }

      } else {
         if(orderType == OP_SELL) {
            closestPossibleSL = NormalizeDouble(Ask+minStopLevel*point, digits);
         } else {
            closestPossibleSL = NormalizeDouble(OrderOpenPrice()+minStopLevel*point, digits);
         }

         if(stopLoss < closestPossibleSL) {
            Verbose("Cannot modify SL, it cannot be closer than minimum allowed: ", DoubleToString(stopLoss), " < ", DoubleToString(closestPossibleSL));
            return(false);
         }
      }
   }
   
   if(profitTarget > 0) {
      // check if PT isn't too close to price
      if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
         if(orderType == OP_BUY) {
            closestPossiblePT = NormalizeDouble(Bid+minStopLevel*point, digits);
         } else {
            closestPossiblePT = NormalizeDouble(OrderOpenPrice()+minStopLevel*point, digits);
         }

         if(profitTarget < closestPossiblePT) {
            Verbose("Cannot modify PT, it cannot be closer than minimum allowed: ", DoubleToString(profitTarget), " < ", DoubleToString(closestPossiblePT));
            return(false);
         }

      } else {
         if(orderType == OP_SELL) {
            closestPossiblePT = NormalizeDouble(Ask-minStopLevel*point, digits);
         } else {
            closestPossiblePT = NormalizeDouble(OrderOpenPrice()-minStopLevel*point, digits);
         }

         if(profitTarget > closestPossiblePT) {
            Verbose("Cannot modify PT, it cannot be closer than minimum allowed: ", DoubleToString(profitTarget), " > ", DoubleToString(closestPossiblePT));
            return(false);
         }
      }
   }
      
   result = OrderModifyReliable(ticket, price, sqFixMarketPrice(stopLoss, OrderSymbol()), sqFixMarketPrice(profitTarget, OrderSymbol()), OrderExpiration());
   if(result) {
      Verbose("Order modified successfuly");
      return(true);
   }

   return(false);
}

//+------------------------------------------------------------------+

double sqGetPrice(int orderType, string symbol, double price) {
   if(price == 0 && (orderType == OP_BUY || orderType == OP_SELL)) {
      // get newest Ask/Bid price for market order
      RefreshRates();
      if(orderType == OP_BUY) {
         price = sqGetAsk(symbol);
      } else {
         price = sqGetBid(symbol);
      }
   }
   
   int digits = (int) MarketInfo(symbol, MODE_DIGITS);

  	if (digits > 0) price = NormalizeDouble(price, digits);

   return(price);
}


//+------------------------------------------------------------------+

double sqGetClosePrice(int orderType, string symbol, double price) {
   if(price == 0 && (orderType == OP_BUY || orderType == OP_SELL)) {
      // get newest Ask/Bid price for market order
      RefreshRates();
      if(orderType == OP_BUY) {
         price = sqGetBid(symbol);
      } else {
         price = sqGetAsk(symbol);
      }
   }
   
   int digits = (int) MarketInfo(symbol, MODE_DIGITS);

  	if (digits > 0) price = NormalizeDouble(price, digits);

   return(price);
}

//+------------------------------------------------------------------+

bool sqCheckConnected() {
   if (!IsConnected()) {
      Verbose("Not connected!");
      return(false);
   }
   if (IsStopped()) {
      Verbose("EA stopped!");
      return(false);
   }

   return(true);
}

//+------------------------------------------------------------------+

int sleepPeriod = 500; // 0.5 s
int maxSleepPeriod = 20000; // 20 s.

void sqSleep() {
   //if(IsTesting()) return;

   Sleep(sleepPeriod);

   int periods = maxSleepPeriod / sleepPeriod;

   for(int i=0; i<periods; i++) {
      if (MathRand() > 16383) {
         // 50% chance of quitting
         break;
      }

      Sleep(sleepPeriod);
   }
}

//+------------------------------------------------------------------+

string sqGetOrderTypeAsString(int type) {
   switch(type) {
      case OP_BUY: return("Buy");
      case OP_SELL: return("Sell");
      case OP_BUYLIMIT: return("Buy Limit");
      case OP_BUYSTOP: return("Buy Stop");
      case OP_SELLLIMIT: return("Sell Limit");
      case OP_SELLSTOP: return("Sell Stop");
   }

   return("Unknown");
}

//+------------------------------------------------------------------+

void sqInitInfoPanel() {
      sqLabelCorner       = SqCornerToMT4(InpInfoPanelCorner);
      ObjectCreate("line1", OBJ_LABEL, 0, 0, 0);
      ObjectSet("line1", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("line1", OBJPROP_YDISTANCE, sqOffsetVertical + 0 );
      ObjectSet("line1", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("line1", "${getOptions("StrategyName")}"  + " | Magic: " + (string)MagicNumber, 9, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("linec", OBJ_LABEL, 0, 0, 0);
      ObjectSet("linec", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("linec", OBJPROP_YDISTANCE, sqOffsetVertical + 16 );
      ObjectSet("linec", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("linec", "Generated by StrategyQuant EA Wizard", 8, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("line2", OBJ_LABEL, 0, 0, 0);
      ObjectSet("line2", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("line2", OBJPROP_YDISTANCE, sqOffsetVertical + 28);
      ObjectSet("line2", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("line2", "------------------------------------------", 8, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("lines", OBJ_LABEL, 0, 0, 0);
      ObjectSet("lines", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("lines", OBJPROP_YDISTANCE, sqOffsetVertical + 44);
      ObjectSet("lines", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("lines", "Last Signal:  -", 9, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("lineopl", OBJ_LABEL, 0, 0, 0);
      ObjectSet("lineopl", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("lineopl", OBJPROP_YDISTANCE, sqOffsetVertical + 60);
      ObjectSet("lineopl", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("lineopl", "Open P/L: -", 8, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("linea", OBJ_LABEL, 0, 0, 0);
      ObjectSet("linea", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("linea", OBJPROP_YDISTANCE, sqOffsetVertical + 76);
      ObjectSet("linea", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("linea", "Account Balance: -", 8, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("lineto", OBJ_LABEL, 0, 0, 0);
      ObjectSet("lineto", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("lineto", OBJPROP_YDISTANCE, sqOffsetVertical + 92);
      ObjectSet("lineto", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("lineto", "Total profits/losses so far: -/-", 8, "Tahoma", GetAdaptiveTextColor());

      ObjectCreate("linetp", OBJ_LABEL, 0, 0, 0);
      ObjectSet("linetp", OBJPROP_CORNER, sqLabelCorner);
      ObjectSet("linetp", OBJPROP_YDISTANCE, sqOffsetVertical + 108);
      ObjectSet("linetp", OBJPROP_XDISTANCE, sqOffsetHorizontal);
      ObjectSetText("linetp", "Total P/L so far: -", 8, "Tahoma", GetAdaptiveTextColor());

      sqCreateDirectionButtons();
}

//+------------------------------------------------------------------+

void sqCreateDirectionButtons() {
   int btnWidth = 100;
   int btnHeight = 20;
   int btnGap = 4;
   // Position buttons below the info panel
   bool isBottomCorner = (sqLabelCorner == CORNER_LEFT_LOWER || sqLabelCorner == CORNER_RIGHT_LOWER);
   int btnY;
   if(isBottomCorner) {
      // For bottom corners, Y grows upward from bottom edge
      // Place buttons near bottom, shift info panel up to make room
      int panelShift = btnHeight - 8;
      btnY = sqOffsetVertical + 4;
      // Shift all info panel lines upward
      ObjectSet("line1",   OBJPROP_YDISTANCE, sqOffsetVertical + 0   + panelShift);
      ObjectSet("linec",   OBJPROP_YDISTANCE, sqOffsetVertical + 16  + panelShift);
      ObjectSet("line2",   OBJPROP_YDISTANCE, sqOffsetVertical + 28  + panelShift);
      ObjectSet("lines",   OBJPROP_YDISTANCE, sqOffsetVertical + 44  + panelShift);
      ObjectSet("lineopl", OBJPROP_YDISTANCE, sqOffsetVertical + 60  + panelShift);
      ObjectSet("linea",   OBJPROP_YDISTANCE, sqOffsetVertical + 76  + panelShift);
      ObjectSet("lineto",  OBJPROP_YDISTANCE, sqOffsetVertical + 92  + panelShift);
      ObjectSet("linetp",  OBJPROP_YDISTANCE, sqOffsetVertical + 108 + panelShift);
   } else {
      btnY = sqOffsetVertical + 128;
   }
   int btnLongX, btnShortX;

   bool isRightCorner = (sqLabelCorner == CORNER_RIGHT_UPPER || sqLabelCorner == CORNER_RIGHT_LOWER);
   if(isRightCorner) {
      // For right corners, XDISTANCE is from right edge to button's left edge
      // Button extends rightward toward edge, so add btnWidth to keep it on screen
      btnShortX = sqOffsetHorizontal + btnWidth;
      btnLongX  = sqOffsetHorizontal + 2 * btnWidth + btnGap;
   } else {
      btnLongX  = sqOffsetHorizontal;
      btnShortX = sqOffsetHorizontal + btnWidth + btnGap;
   }

   // Long button
   ObjectCreate(0, "sqBtnLong", OBJ_BUTTON, 0, 0, 0);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_XDISTANCE, btnLongX);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_YDISTANCE, btnY);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_XSIZE, btnWidth);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_YSIZE, btnHeight);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_CORNER, sqLabelCorner);
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_FONTSIZE, 8);
   ObjectSetString(0, "sqBtnLong", OBJPROP_FONT, "Tahoma");
   ObjectSetInteger(0, "sqBtnLong", OBJPROP_SELECTABLE, false);

   // Short button
   ObjectCreate(0, "sqBtnShort", OBJ_BUTTON, 0, 0, 0);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_XDISTANCE, btnShortX);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_YDISTANCE, btnY);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_XSIZE, btnWidth);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_YSIZE, btnHeight);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_CORNER, sqLabelCorner);
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_FONTSIZE, 8);
   ObjectSetString(0, "sqBtnShort", OBJPROP_FONT, "Tahoma");
   ObjectSetInteger(0, "sqBtnShort", OBJPROP_SELECTABLE, false);

   sqUpdateDirectionButtons();
}

//+------------------------------------------------------------------+

void sqUpdateDirectionButtons() {
   // Long button
   if(LongEnabled) {
      ObjectSetString(0, "sqBtnLong", OBJPROP_TEXT, "Long Enabled");
      ObjectSetInteger(0, "sqBtnLong", OBJPROP_BGCOLOR, clrForestGreen);
      ObjectSetInteger(0, "sqBtnLong", OBJPROP_COLOR, clrWhite);
   } else {
      ObjectSetString(0, "sqBtnLong", OBJPROP_TEXT, "Long Disabled");
      ObjectSetInteger(0, "sqBtnLong", OBJPROP_BGCOLOR, clrFireBrick);
      ObjectSetInteger(0, "sqBtnLong", OBJPROP_COLOR, clrWhite);
   }

   // Short button
   if(ShortEnabled) {
      ObjectSetString(0, "sqBtnShort", OBJPROP_TEXT, "Short Enabled");
      ObjectSetInteger(0, "sqBtnShort", OBJPROP_BGCOLOR, clrForestGreen);
      ObjectSetInteger(0, "sqBtnShort", OBJPROP_COLOR, clrWhite);
   } else {
      ObjectSetString(0, "sqBtnShort", OBJPROP_TEXT, "Short Disabled");
      ObjectSetInteger(0, "sqBtnShort", OBJPROP_BGCOLOR, clrFireBrick);
      ObjectSetInteger(0, "sqBtnShort", OBJPROP_COLOR, clrWhite);
   }
}

//+------------------------------------------------------------------+

void sqDeinitInfoPanel() {
   ObjectDelete("line1");
   ObjectDelete("linec");
   ObjectDelete("line2");
   ObjectDelete("lines");
   ObjectDelete("lineopl");
   ObjectDelete("linea");
   ObjectDelete("lineto");
   ObjectDelete("linetp");
   ObjectDelete("sqBtnLong");
   ObjectDelete("sqBtnShort");
}

//+------------------------------------------------------------------+

void sqTextFillOpens() {
   ObjectSetText("lineopl", "Open P/L: "+DoubleToStr(sqGetOpenPLInMoney(0), 2), 8, "Tahoma", GetAdaptiveTextColor());
   ObjectSetText("linea", "Account Balance: "+DoubleToStr(AccountBalance(), 2) , 8, "Tahoma", GetAdaptiveTextColor());
}

//+------------------------------------------------------------------+

void sqTextFillTotals() {
   ObjectSetText("lineto", "Total profits/losses so far: "+DoubleToStr(sqGetTotalProfits(0, 100))+"/"+DoubleToStr(sqGetTotalLosses(0, 100)), 8, "Tahoma", GetAdaptiveTextColor());
   ObjectSetText("linetp", "Total P/L so far: "+DoubleToStr(sqGetTotalClosedPLInMoney(0, 1000), 2), 8, "Tahoma", GetAdaptiveTextColor());
}


//+------------------------------------------------------------------+

double sqGetOpenPLInMoney(int orderMagicNumber) {
   double pl = 0;

   if(orderSelectTimeout > 0){
       Sleep(orderSelectTimeout);
   }
   
   for (int cc = OrdersTotal() - 1; cc >= 0; cc--) {
      if (!OrderSelect(cc, SELECT_BY_POS) ) continue;
      if(OrderType() != OP_BUY && OrderType() != OP_SELL) continue;
      if(OrderSymbol() != Symbol()) continue;
      if(orderMagicNumber != 0 && OrderMagicNumber() != orderMagicNumber) continue;

      pl += OrderProfit();
   }

   return(pl);
}

//+------------------------------------------------------------------+

int sqGetTotalProfits(int orderMagicNumber, int numberOfLastOrders) {
   double pl = 0;
   int count = 0;
   int profits = 0;

   for(int i=OrdersHistoryTotal(); i>=0; i--) {
      if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true && OrderSymbol() == Symbol()) {

         if(orderMagicNumber == 0 || OrderMagicNumber() == orderMagicNumber) {
            // return the P/L of last order
            // or return the P/L of last order with given Magic Number
            count++;

            if(OrderType() == OP_BUY) {
               pl = (OrderClosePrice() - OrderOpenPrice());
            } else {
               pl = (OrderOpenPrice() - OrderClosePrice());
            }

            if(pl > 0) {
               profits++;
            }

            if(count >= numberOfLastOrders) break;
         }
      }
   }

   return(profits);
}

//+------------------------------------------------------------------+

int sqGetTotalLosses(int orderMagicNumber, int numberOfLastOrders) {
   double pl = 0;
   int count = 0;
   int losses = 0;

   for(int i=OrdersHistoryTotal(); i>=0; i--) {
      if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true && OrderSymbol() == Symbol()) {

         if(orderMagicNumber == 0 || OrderMagicNumber() == orderMagicNumber) {
            // return the P/L of last order
            // or return the P/L of last order with given Magic Number
            count++;

            if(OrderType() == OP_BUY) {
               pl = (OrderClosePrice() - OrderOpenPrice());
            } else {
               pl = (OrderOpenPrice() - OrderClosePrice());
            }

            if(pl < 0) {
               losses++;
            }

            if(count >= numberOfLastOrders) break;
         }
      }
   }

   return(losses);
}


//+------------------------------------------------------------------+

double sqGetTotalClosedPLInMoney(int orderMagicNumber, int numberOfLastOrders) {
   double pl = 0;
   int count = 0;

   for(int i=OrdersHistoryTotal(); i>=0; i--) {
      if(OrderSelect(i,SELECT_BY_POS,MODE_HISTORY)==true && OrderSymbol() == Symbol()) {
         if(orderMagicNumber == 0 || OrderMagicNumber() == orderMagicNumber) {
            // return the P/L of last order or the P/L of last order with given Magic Number

            count++;
            pl = pl + OrderProfit();

            if(count >= numberOfLastOrders) break;
         }
      }
   }

   return(pl);
}

//+------------------------------------------------------------------+

double sqGetSLPercentLevel(string symbol, int orderType, double price, int valueInPips, double value) {
   string correctedSymbol = correctSymbol(symbol);
   if(price == 0) {
      // price can be zero for market order
      if(orderType == OP_BUY) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   double gap = price * value / 100;
   return sqGetSLPTLevel(-1.0, symbol, orderType, price, 2, gap);
}

//+------------------------------------------------------------------+

double sqGetPTPercentLevel(string symbol, int orderType, double price, int valueInPips, double value) {
   string correctedSymbol = correctSymbol(symbol);
   if(price == 0) {
      // price can be zero for market order
      if(orderType == OP_BUY) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   double gap = price * value / 100;
   return sqGetSLPTLevel(1.0, symbol, orderType, price, 2, gap);
}

//+------------------------------------------------------------------+

double sqGetSLLevel(string symbol, int orderType, double price, int valueInPips, double value) {
   return(sqGetSLPTLevel(-1.0, symbol, orderType, price, valueInPips, value));
}

//+------------------------------------------------------------------+

double sqGetPTLevel(string symbol, int orderType, double price, int valueInPips, double value) {
   return(sqGetSLPTLevel(1.0, symbol, orderType, price, valueInPips, value));
}

//+------------------------------------------------------------------+

/**
* valueType: 1 - pips, 2 - real pips (ATR range), 3 - price level
*/
double sqGetSLPTLevel(double SLorPT, string symbol, int orderType, double price, int valueType, double value) {
   string correctedSymbol = correctSymbol(symbol);
   double pointCoef = sqGetPointCoef(symbol);

   if(valueType == 1) {
      // convert from pips to real points
      value = sqConvertToRealPips(correctedSymbol, value);
   }
   
   if(price == 0) {
      // price can be zero for market order
      if(orderType == OP_BUY) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   
   double slptValue = value;
   
   if(valueType != 3) {
      if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
         slptValue = price + (SLorPT * value);
      } else {
         slptValue = price - (SLorPT * value);
      }
   }

   // check that SL / PT is within predefined boundaries
   double minSLPTValue, maxSLPTValue;
   
   if(SLorPT < 0) {
      // it is SL
      
      if(MinimumSL <= 0) {
         minSLPTValue = slptValue;
      } else {
         if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
            minSLPTValue = price + (SLorPT * MinimumSL * pointCoef);
            slptValue = MathMin(slptValue, minSLPTValue);
            
         } else {
         
            minSLPTValue = price - (SLorPT * MinimumSL * pointCoef);
            slptValue = MathMax(slptValue, minSLPTValue);
         }
   
      }
      
      if(MaximumSL <= 0) {
         maxSLPTValue = slptValue;
      } else {
         if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
            maxSLPTValue = price + (SLorPT * MaximumSL * pointCoef);
            slptValue = MathMax(slptValue, maxSLPTValue);

         } else {
            maxSLPTValue = price - (SLorPT * MaximumSL * pointCoef);
            slptValue = MathMin(slptValue, maxSLPTValue);
         }

      }
      
   } else {
      // it is PT

      if(MinimumPT <= 0) {
         minSLPTValue = slptValue;
      } else {
         if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
            minSLPTValue = price + (SLorPT * MinimumPT * pointCoef);
            slptValue = MathMax(slptValue, minSLPTValue);
            
         } else {
            minSLPTValue = price - (SLorPT * MinimumPT * pointCoef);
            slptValue = MathMin(slptValue, minSLPTValue);
         }

      }
      
      if(MaximumPT <= 0) {
         maxSLPTValue = slptValue;
      } else {

         if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
            maxSLPTValue = price + (SLorPT * MaximumPT * pointCoef);
            slptValue = MathMin(slptValue, maxSLPTValue);

         } else {
         
            maxSLPTValue = price - (SLorPT * MaximumPT * pointCoef);
            slptValue = MathMax(slptValue, maxSLPTValue);
         }
      }
   }
               
   return (slptValue);
}  

//+------------------------------------------------------------------+

double sqConvertToPips(string symbol, double value) {
   if(symbol == "NULL" || symbol == "Current") {
      return(value / gPointCoef);
   }

   // recognize point coeficient         
   double ticksize = sqGetMarketTickSize(symbol);
   if(ticksize < 0){
      ticksize = calculatePointCoef(symbol);
   }

   return(value / ticksize);
}                  
                         
//+------------------------------------------------------------------+

double sqConvertToRealPips(string symbol, double value) {
   if(symbol == "NULL" || symbol == "Current" || symbol == "Same as main chart") {
      return NormalizeDouble(gPointCoef * value, 6);
   }

   double pointCoef = sqGetPointCoef(symbol);

   return NormalizeDouble(pointCoef * value, 6);
}

//+------------------------------------------------------------------+

double sqGetPointCoef(string symbol) {
   if(symbol == "NULL" || symbol == "Current" || symbol == "Same as main chart") {
      return(gPointCoef);
   }

   return calculatePointCoef(symbol);
}

//+------------------------------------------------------------------+

double calculatePointCoef(string symbol){
   string correctedSymbol = correctSymbol(symbol);
   
   if(UseSQTickSize) {
      double ticksize = sqGetMarketTickSize(correctedSymbol);
      if(ticksize >= 0){
         return ticksize;
      }
   }
      
   int realDigits = (int) MarketInfo(correctedSymbol, MODE_DIGITS);
   if(realDigits > 0 && realDigits != 2 && realDigits != 4) {
      realDigits -= 1;
   }
   return 1.0 / MathPow(10, realDigits);
}

//+------------------------------------------------------------------+

bool sqDoublesAreEqual(double n1, double n2) {
   string st1 = DoubleToStr(n1, Digits);
   string st2 = DoubleToStr(n2, Digits);

   return (st1 == st2);
}

//+------------------------------------------------------------------+

double sqHighest(string symbol, int timeframe, int computedFrom, int period, int shift) {
   double maxnum = -100000000;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val > maxnum) {
         maxnum = val;
      }
   }

   return(maxnum);
}

//+------------------------------------------------------------------+

double sqHighestIndex(string symbol, int timeframe, int computedFrom, int period, int shift) {
   double maxnum = -100000000;
   int index = 0;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val > maxnum) {
         maxnum = val;
         index = i;
      }
   }

   return(index);
}

//+------------------------------------------------------------------+

double sqLowest(string symbol, int timeframe, int computedFrom, int period, int shift) {
   double minnum = 100000000;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val < minnum) {
         minnum = val;
      }
   }

   return(minnum);
}

//+------------------------------------------------------------------+

double sqLowestIndex(string symbol, int timeframe, int computedFrom, int period, int shift) {
   double minnum = 100000000;
   int index = 0;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val < minnum) {
         minnum = val;
         index = i;
      }
   }

   return(index);
}

//+------------------------------------------------------------------+

double sqGetValue(string symbol, int timeframe, int computedFrom, int shift) {
   double val = 0;
   
   if(symbol == "NULL" || symbol == "Current") {
      switch(computedFrom) {
         case PRICE_OPEN: val = iOpen(NULL, timeframe, shift); break;
         case PRICE_HIGH: return iHigh(NULL, timeframe, shift); break;
         case PRICE_LOW: val = iLow(NULL, timeframe, shift); break;
         case PRICE_CLOSE: val = iClose(NULL, timeframe, shift); break;
         case PRICE_MEDIAN: val = (iHigh(NULL, timeframe, shift)+iLow(NULL, timeframe, shift))/2; break;
         case PRICE_TYPICAL: val = (iHigh(NULL, timeframe, shift)+iLow(NULL, timeframe, shift)+iClose(NULL, timeframe, shift))/3; break;
         case PRICE_WEIGHTED: val = (iHigh(NULL, timeframe, shift)+iLow(NULL, timeframe, shift)+iClose(NULL, timeframe, shift)+iClose(NULL, timeframe, shift))/4; break;
      }

   } else {
      switch(computedFrom) {
         case PRICE_OPEN: val = iOpen(symbol, timeframe, shift); break;
         case PRICE_HIGH: val = iHigh(symbol, timeframe, shift); break;
         case PRICE_LOW: val = iLow(symbol, timeframe, shift); break;
         case PRICE_CLOSE: val = iClose(symbol, timeframe, shift); break;
         case PRICE_MEDIAN: val = (iHigh(symbol, timeframe, shift)+iLow(symbol, timeframe, shift))/2; break;
         case PRICE_TYPICAL: val = (iHigh(symbol, timeframe, shift)+iLow(symbol, timeframe, shift)+iClose(symbol, timeframe, shift))/3; break;
         case PRICE_WEIGHTED: val = (iHigh(symbol, timeframe, shift)+iLow(symbol, timeframe, shift)+iClose(symbol, timeframe, shift)+iClose(symbol, timeframe, shift))/4; break;
      }
   }

   return roundValue(val);
}

//+------------------------------------------------------------------+

void sqDrawUpArrow(int shift) {
   string name = StringConcatenate("Arrow_", MathRand());

   ObjectCreate(name, OBJ_ARROW, 0, Time[shift], Low[shift]-100*Point); //draw an up arrow
   ObjectSet(name, OBJPROP_STYLE, STYLE_SOLID);
   ObjectSet(name, OBJPROP_ARROWCODE, SYMBOL_ARROWUP);
   ObjectSet(name, OBJPROP_COLOR, Green);
}

//+------------------------------------------------------------------+

void sqDrawDownArrow(int shift) {
   string name = StringConcatenate("Arrow_", MathRand());

   ObjectCreate(name, OBJ_ARROW, 0, Time[shift], High[shift]+100*Point); //draw an down arrow
   ObjectSet(name, OBJPROP_STYLE, STYLE_SOLID);
   ObjectSet(name, OBJPROP_ARROWCODE, SYMBOL_ARROWDOWN);
   ObjectSet(name, OBJPROP_COLOR, Red);
}

//+------------------------------------------------------------------+

void sqDrawVerticalLine(int shift) {
   string name = StringConcatenate("VerticalLine", MathRand());

   ObjectCreate(name, OBJ_VLINE, 0, Time[shift], 0);                       //draw a vertical line
   ObjectSet(name,OBJPROP_COLOR, Red);
   ObjectSet(name,OBJPROP_WIDTH, 1);
   ObjectSet(name,OBJPROP_STYLE, STYLE_DOT);
}

//+------------------------------------------------------------------+

double sqDaily(string symbol, int tf, string mode, int shift) {
   return sqGetOHLC(symbol, PERIOD_D1, mode, shift);
}                

//+------------------------------------------------------------------+

double sqWeekly(string symbol, int tf, string mode, int shift) {
   return sqGetOHLC(symbol, PERIOD_W1, mode, shift);
}

//+------------------------------------------------------------------+

double sqMonthly(string symbol, int tf, string mode, int shift) {
   return sqGetOHLC(symbol, PERIOD_MN1, mode, shift);
}
  
//+------------------------------------------------------------------+

double sqGetOHLC(string symbol, int tf, string mode, int shift){
   if(symbol == "NULL" || symbol == "Current") {
      if(mode == "Open") {
         return(iOpen(NULL, tf, shift));
      }
      if(mode == "Close") {
         return(iClose(NULL, tf, shift));
      }
      if(mode == "High") {
         return(iHigh(NULL, tf, shift));
      }
      if(mode == "Low") {
         return(iLow(NULL, tf, shift));
      }
   } 
   else {
      if(mode == "Open") {
         return(iOpen(symbol, tf, shift));
      }
      if(mode == "Close") {
         return(iClose(symbol, tf, shift));
      }
      if(mode == "High") {
         return(iHigh(symbol, tf, shift));
      }
      if(mode == "Low") {
         return(iLow(symbol, tf, shift));
      }
   }                 

   return(-1);
}

//+------------------------------------------------------------------+

int getHHMM(string time){
   string result[];           
   int k = StringSplit(time, ':', result);
   
   if(k == 2){
      int hour = StrToInteger(StringSubstr(result[0], 0, 1) == "0" ? StringSubstr(result[0], 1, 1) : result[0]);
      int minute = StrToInteger(StringSubstr(result[1], 0, 1) == "0" ? StringSubstr(result[1], 1, 1) : result[1]);
      return (hour * 100) + minute;
   }
   else {
      Print("Incorrect time value format. Value: '" + time + "'");
      return 0;
   }
}

//+------------------------------------------------------------------+

string HHMMToString(int hhmm){
   int hours = hhmm / 100;
   int minutes = hhmm - (hours * 100);
   string time = (hours < 10 ? "0" : "") + IntegerToString(hours) + ":" + (minutes < 10 ? "0" : "") + IntegerToString(minutes);
   return time;
}

//+------------------------------------------------------------------+

int sqGetDate(int day, int month, int year) {
   return (year - 1900) * 10000 + month * 100 + day;
}

//+------------------------------------------------------------------+

int sqGetBarDate(datetime dt) {
   return (TimeYear(dt) - 1900) * 10000 + TimeMonth(dt) * 100 + TimeDay(dt);
}

//+------------------------------------------------------------------+

double sqGetTime(int hour, int minute, int second) {
   return 100 * hour + minute + 0.01 * second;
}

//+------------------------------------------------------------------+

double getSQTime(datetime time){
   return 100 * TimeHour(time) + TimeMinute(time) + 0.1 * TimeSeconds(time);
}

//+------------------------------------------------------------------+

double sqSafeDivide(double var1, double var2) {
   if(var2 == 0) return(100000000);
   return(var1/var2);
}

//+------------------------------------------------------------------+
//+ Candle Pattern functions
//+------------------------------------------------------------------+

bool sqBearishEngulfing(string symbol, int timeframe, int shift) {
   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);

   double ocDiff = NormalizeDouble(O-C, _Digits);
   double o1c1Diff = NormalizeDouble(C1-O1, _Digits);

   if ((C1>O1)&&(O>C)&&(O>=C1)&&(O1>=C)&&(ocDiff>o1c1Diff)) {
      return(true);
   }

   return(false);
}

//+------------------------------------------------------------------+

bool sqBullishEngulfing(string symbol, int timeframe, int shift) {
   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);

   double coDiff = NormalizeDouble(C-O, _Digits);
   double o1c1Diff = NormalizeDouble(O1-C1, _Digits);
   
   if ((O1>C1)&&(C>O)&&(C>=O1)&&(C1>=O)&&(coDiff>o1c1Diff)) {
      return(true);
   }

   return(false);
}

//+------------------------------------------------------------------+

bool sqDarkCloudCover(string symbol, int timeframe, int shift) {
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);

   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);
   
 	double tickSize = sqGetPointCoef(symbol);

 	double Piercing_Line_Ratio = 0.5f;
 	double Piercing_Candle_Length = 10.0f;
 	
 	double HL = NormalizeDouble(H-L, _Digits);
 	double OC = NormalizeDouble(O-C, _Digits);
 	double OC_HL = HL != 0 ? NormalizeDouble(OC/HL, 6) : 0;
 	double O1C1_D2 = NormalizeDouble((O1+C1)/2, _Digits);
 	double PCL_MTS = NormalizeDouble(Piercing_Candle_Length*tickSize, _Digits);
 			
 	if(C1 > O1 && O1C1_D2 > C && O > C && C > O1 && OC_HL > Piercing_Line_Ratio && HL >= PCL_MTS) {
 		return true;
 	}

   return(false);
}

//+------------------------------------------------------------------+

bool sqDoji(string symbol, int timeframe, int shift) {
   double diff = NormalizeDouble(MathAbs(sqOpen(symbol, timeframe, shift) - sqClose(symbol, timeframe, shift)), _Digits);
   double coef = NormalizeDouble(sqGetPointCoef(symbol) * 0.6, _Digits); 
   
   if(diff < coef) {
      return(true);
   }
   
   return(false);
}

//+------------------------------------------------------------------+

bool sqHammer(string symbol, int timeframe, int shift) {
   double H = sqHigh(symbol, timeframe, shift);
   double L = sqLow(symbol, timeframe, shift);
   double L1 = sqLow(symbol, timeframe, shift+1);
   double L2 = sqLow(symbol, timeframe, shift+2);
   double L3 = sqLow(symbol, timeframe, shift+3);

   double O = sqOpen(symbol, timeframe, shift);
   double C = sqClose(symbol, timeframe, shift);
   double CL = NormalizeDouble(H-L, _Digits);

   double BodyLow, BodyHigh;
   double Candle_WickBody_Percent = 0.9;
   double CandleLength = 12;

   if (O > C) {
      BodyHigh = O;
      BodyLow = C;
   } else {
      BodyHigh = C;
      BodyLow = O;
   }

   double LW = NormalizeDouble(BodyLow - L, _Digits);
   double UW = NormalizeDouble(H - BodyHigh, _Digits);
   double BLa = NormalizeDouble(MathAbs(O - C), _Digits);
   double BL90 = NormalizeDouble(BLa * Candle_WickBody_Percent, _Digits);
   
   double pipValue = sqGetPointCoef(symbol);
   
   double LW_D2 = NormalizeDouble(LW / 2, _Digits);
   double LW_D3 = NormalizeDouble(LW / 3, _Digits);
   double LW_D4 = NormalizeDouble(LW / 4, _Digits);
   double BL90_M2 = NormalizeDouble(2 * BL90, _Digits);
   double CL_MPV = NormalizeDouble(CandleLength * pipValue, _Digits);
     
   if(L <= L1 && L < L2 && L < L3)  {
 		if(LW_D2 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D3 <= UW && LW_D4 <= UW)  {
    	  	return(true);
      }
      if(LW_D3 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D4 <= UW)  {
      	return(true);
      }
      if(LW_D4 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C)  {
    	  	return(true);
      }
   }
   
   return(false);
}

//+------------------------------------------------------------------+

bool sqPiercingLine(string symbol, int timeframe, int shift) {
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);

   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);
   
 	double tickSize = sqGetPointCoef(symbol);

 	double Piercing_Line_Ratio = 0.5f;
 	double Piercing_Candle_Length = 10.0f;
 	
 	double HL = NormalizeDouble(H-L, _Digits);
 	double CO = NormalizeDouble(C-O, _Digits);
 	double CO_HL = HL != 0 ? NormalizeDouble(CO/HL, 6) : 0;
 	double O1C1_D2 = NormalizeDouble((O1+C1)/2, _Digits);
 	double PCL_MTS = NormalizeDouble(Piercing_Candle_Length*tickSize, _Digits);
 			
 	if(C1 < O1 && O1C1_D2 < C && O < C && C < O1 && CO_HL > Piercing_Line_Ratio && HL >= PCL_MTS) {
 		return true;
 	}

   return(false);
}

//+------------------------------------------------------------------+

bool sqShootingStar(string symbol, int timeframe, int shift) {
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);
   double H1 = sqHigh(symbol, timeframe, shift + 1);
   double H2 = sqHigh(symbol, timeframe, shift + 2);
   double H3 = sqHigh(symbol, timeframe, shift + 3);

   double O = sqOpen(symbol, timeframe, shift);
   double C = sqClose(symbol, timeframe, shift);
   double CL = NormalizeDouble(H - L, _Digits);

   double BodyLow, BodyHigh;
   double Candle_WickBody_Percent = 0.9;
   double CandleLength = 12;

   if (O > C) {
      BodyHigh = O;
      BodyLow = C;
   } else {
      BodyHigh = C;
      BodyLow = O;
   }

   double LW = NormalizeDouble(BodyLow - L, _Digits);
   double UW = NormalizeDouble(H - BodyHigh, _Digits);
   double BLa = NormalizeDouble(MathAbs(O - C), _Digits);
   double BL90 = NormalizeDouble(BLa * Candle_WickBody_Percent, _Digits);
   
   double pipValue = sqGetPointCoef(symbol);
   
   double UW_D2 = NormalizeDouble(UW / 2, _Digits);
   double UW_D3 = NormalizeDouble(UW / 3, _Digits);
   double UW_D4 = NormalizeDouble(UW / 4, _Digits);
   double BL90_M2 = NormalizeDouble(2 * BL90, _Digits);
   double CL_MPV = NormalizeDouble(CandleLength * pipValue, _Digits);

   if(H >= H1 && H > H2 && H > H3)  {
      if(UW_D2 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C && UW_D3 <= LW && UW_D4 <= LW)  {
         return(true);
      }
      if(UW_D3 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C && UW_D4 <= LW)  {
         return(true);
      }
      if(UW_D4 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C)  {
         return(true);
      }
   }

   return(false);
} 


//+------------------------------------------------------------------+

double sqOpen(string symbol, int timeframe, int shift) {
   if(symbol == "NULL") {
      return (iOpen(NULL, timeframe, shift));
   } else {
      return (iOpen(symbol, timeframe, shift));
   }
}   

//+------------------------------------------------------------------+

double sqHigh(string symbol, int timeframe, int shift) {
   if(symbol == "NULL") {
      return (iHigh(NULL, timeframe, shift));
   } else {
      return (iHigh(symbol, timeframe, shift));
   }
}   

//+------------------------------------------------------------------+

double sqLow(string symbol, int timeframe, int shift) {
   if(symbol == "NULL") {
      return (iLow(NULL, timeframe, shift));
   } else {
      return (iLow(symbol, timeframe, shift));
   }
}   

//+------------------------------------------------------------------+

double sqClose(string symbol, int timeframe, int shift) {
   if(symbol == "NULL") {
      return (iClose(NULL, timeframe, shift));
   } else {
      return (iClose(symbol, timeframe, shift));
   }
}   


//+------------------------------------------------------------------+

double sqTrueRange(string symbol, int timeframe, int shift) {

   double close1 = sqClose(symbol, timeframe, shift+1);
   double high = sqHigh(symbol, timeframe, shift+1);
   double low = sqLow(symbol, timeframe, shift+1);

   double TrueHigh, TrueLow;

	if(close1 > high) {
		TrueHigh = close1;
	} else {
		TrueHigh = high;
	}

	if(close1 < low) {
		TrueLow = close1;
	} else {
		TrueLow = low;
	}

	double TrueRange = TrueHigh - TrueLow;
	
	return (TrueRange);
}   

//+------------------------------------------------------------------+

bool sqTradeRecentlyClosed(string symbol, int magicNumber, bool checkThisBar, bool checkThisMinute) {
	int ordersChecked = 0;

	string strCurrentTimeMinutes = TimeToStr( TimeCurrent(), TIME_DATE|TIME_MINUTES);

   for (int cc = OrdersHistoryTotal() - 1; cc >= 0; cc--) {
      if (OrderSelect(cc, SELECT_BY_POS,MODE_HISTORY)) {

         // skip pending orders
         if(OrderType() == OP_BUYSTOP || OrderType() == OP_BUYLIMIT || OrderType() == OP_SELLSTOP || OrderType() == OP_SELLLIMIT) {
            continue;
         }

         if(magicNumber != 0 && OrderMagicNumber() != magicNumber) continue;

         if(symbol != "Any") {
            if(OrderSymbol() != correctSymbol(symbol)) continue;
         }

         ordersChecked++;
			if(ordersChecked > 10) {
				// check only the very last 10 orders
				break;
			}

			if(checkThisBar) {
				if(OrderCloseTime() >= Time[0]) {
					// order finished this bar
					return(true);
				}
			}

			if(checkThisMinute) {
			   string strCloseTimeMinutes = TimeToStr( OrderCloseTime(), TIME_DATE|TIME_MINUTES);

				if(strCurrentTimeMinutes == strCloseTimeMinutes) {
					// order finished this minute
					return(true);
				}
			}
		}
   }

   return(false);
}

//+------------------------------------------------------------------+

double roundDown(double value, double step, int decimals) {
    if(step > 0) {
	   value = MathFloor(value / step) * step;
    }
	
	return roundDown(value, decimals);
}
 
//+------------------------------------------------------------------+

double roundDown(double value, int decimals) {
  	double p = 0;
  	
  	switch(decimals) {
  		case 0: return (int) value; 
  		case 1: p = 10; break;
  		case 2: p = 100; break;
  		case 3: p = 1000; break;
  		case 4: p = 10000; break;
  		case 5: p = 100000; break;
  		case 6: p = 1000000; break;
  		default: p = MathPow(10, decimals);
  	}

  	value = value * p;
  	double tmp = MathFloor(value + 0.00000001);
  	return NormalizeDouble(tmp/p, decimals);
}

//+------------------------------------------------------------------+

int sqWeekOfMonth(datetime date){
   MqlDateTime tm;
   TimeToStruct(date, tm);
   
   // Strip time to midnight
   tm.hour = 0;
   tm.min = 0;
   tm.sec = 0;
   datetime dateOnly = StructToTime(tm);
   
   // Get first day of month
   tm.day = 1;
   datetime firstMonthDay = StructToTime(tm);
   
   // Get day-of-week of the 1st (ISO: Mon=1, ..., Sun=7)
   MqlDateTime tmFirst;
   TimeToStruct(firstMonthDay, tmFirst);
   int dow = tmFirst.day_of_week; // 0=Sun, 1=Mon, ..., 6=Sat
   int isoDow = (dow == 0) ? 7 : dow;
   
   // Find Monday of the ISO week containing the 1st
   datetime firstMonthMonday = firstMonthDay - (isoDow - 1) * 86400;
   
   // Compute week of month = daysBetween / 7 + 1
   int daysBetween = (int)((long)(dateOnly - firstMonthMonday) / 86400);
   int weekOfMonth = daysBetween / 7 + 1;
   
   return(weekOfMonth);
}

//+------------------------------------------------------------------+

class CSQTime {
public:

   datetime setHHMM(datetime time, string hhmm) {
      string date = TimeToStr(time,TIME_DATE);//"yyyy.mm.dd"
      return (StrToTime(date + " " + hhmm));
   }

   //+------------------------------------------------------------------+

   datetime correctDayStart(datetime time) {
      MqlDateTime strTime;
      TimeToStruct(time, strTime);
      strTime.hour = 0;
      strTime.min = 0;
      strTime.sec = 0;
      return (StructToTime(strTime));
   }
   
  //+------------------------------------------------------------------+

   datetime getDateInMs(datetime time) {
      MqlDateTime strTime;
      TimeToStruct(time, strTime);
      strTime.hour = 0;
      strTime.min = 0;
      strTime.sec = 0;
      return (StructToTime(strTime));
   }
      
   //+------------------------------------------------------------------+

   datetime correctDayEnd(datetime time) {
      MqlDateTime strTime;
      TimeToStruct(time, strTime);
      strTime.hour = 23;
      strTime.min = 59;
      strTime.sec = 59;
      return (StructToTime(strTime));
   }
   
   //+------------------------------------------------------------------+

   datetime setDayOfMonth(datetime time, int day) {
		MqlDateTime strTime;
		TimeToStruct(time, strTime);
		strTime.day = day;
		return (StructToTime(strTime));
   }
   
   //+------------------------------------------------------------------+

   int getMonth(datetime time) {
	  return TimeMonth(time);
   }
   
   //+------------------------------------------------------------------+

   int getDaysInMonth(datetime time) {
      MqlDateTime strTime;
      TimeToStruct(time, strTime);
	  if(strTime.mon==2) {
		return  28+isLeapYear(strTime.year);
	  }
	  
	  return 31-((strTime.mon-1)%7)%2;
   }
   
   //+------------------------------------------------------------------+

	bool isLeapYear(const int _year){
	   if(_year%4 == 0){
		  if(_year%400 == 0)return true;
		  if(_year%100 > 0)return true;
	   }
	   return false;
	}
   
   //+------------------------------------------------------------------+

   datetime addDays(datetime time, int days) {
      int oneDay = 60 * 60 * 24;
      
      return (time + (days * oneDay));
   }
   
   //+------------------------------------------------------------------+

   datetime setDayOfWeek(datetime time, int desiredDow) {
      int dow = convertToSQDOW(TimeDayOfWeek(time));
      desiredDow = convertToSQDOW(desiredDow);
      
      int diffInDays = desiredDow - dow;
      
      //Print("DiffInDays: ", diffInDays, ", dow: ", dow, ", desiredDow: ", desiredDow);
      
      return addDays(time, diffInDays);
   }  
   
   //+------------------------------------------------------------------+

   /**
    * converts from MT DOW format: 0 = Sunday, 1 = Monday ... 6 = Saturday
    * to SQ DOW format: 1 = Monday, 2 = Tuesday ... 7 = Sunday
   */
   int convertToSQDOW(int dow) {
      if(dow == 0) dow = 7;
      
      return(dow);
   } 
};

// create variable for class instance (required)
CSQTime* SQTime;

//+------------------------------------------------------------------+

bool sqEvaluateFuzzySignal(int conditionsCount, int minTrueConditions) {

	bool signalValue = false;
  int trueConditionsCount = 0;
   
  if(minTrueConditions <= 0) {
  	minTrueConditions = 1;
  }
	    
	for(int i=0; i<conditionsCount; i++) {
		bool value = cond[i];
				
		if(value) {
			trueConditionsCount++;
		}
				
		if(trueConditionsCount >= minTrueConditions) {
			signalValue = true;
			break;
		}
	}
			
	return(signalValue);
}

//+------------------------------------------------------------------+

string correctSymbol(string symbol){
  if(symbol == "NULL" || symbol == "Current" || symbol == "Same as main chart") {
      return Symbol();
  }
  <@mt4PrintSubchartConditions />
  else return symbol;
}

//+------------------------------------------------------------------+

double sqFixMarketPrice(double price, string symbol){             
   symbol = correctSymbol(symbol);
   
   double tickSize = MarketInfo(symbol, MODE_TICKSIZE);  
   
   if(tickSize == 0){
      return price;
   }
       
   int digits = (int) MarketInfo(symbol, MODE_DIGITS);
   double finalPrice = tickSize * MathRound(NormalizeDouble(price, digits) / tickSize);
   return NormalizeDouble(finalPrice, digits);
}
//+------------------------------------------------------------------+

bool sqIsUptrend(string symbol, int timeframe, int method) {
   if(method == 0) {
      return (iClose(symbol, timeframe, 1) > sqMA(symbol, timeframe, 200, 0, MODE_SMA, PRICE_CLOSE, 1));      
	 }
	 return(false);	
}

//+------------------------------------------------------------------+

bool sqIsDowntrend(string symbol, int timeframe, int method) {
   if(method == 0) {
      return (iClose(symbol, timeframe, 1) < sqMA(symbol, timeframe, 200, 0, MODE_SMA, PRICE_CLOSE, 1));      
	 }
	 return(false);	
}


//+------------------------------------------------------------------+

int sqGetMonthLastTradingDay(string symbol, int timeframe, bool includeWeekends) {
	datetime barTime = iTime(symbol, timeframe, 0);
    datetime lastTradingDate = SQTime.setDayOfMonth(barTime, SQTime.getDaysInMonth(barTime));

	if(!includeWeekends) {
		if(TimeDayOfWeek(lastTradingDate) == 6) {
			lastTradingDate = SQTime.addDays(lastTradingDate, -1);
		
		} else if(TimeDayOfWeek(lastTradingDate) == 0) {
			lastTradingDate = SQTime.addDays(lastTradingDate, -2);
		}
	}

    return TimeDay(lastTradingDate);
}

//+------------------------------------------------------------------+

datetime monthFirstTradingDay = 0;

bool sqIsMonthFirstTradingDay(string symbol, int timeframe, bool includeWeekends) {
	datetime date = SQTime.getDateInMs(iTime(symbol, timeframe, 0));
	
	if(monthFirstTradingDay == 0) {
		monthFirstTradingDay = date;
	}

	if(SQTime.getMonth(monthFirstTradingDay)!=SQTime.getMonth(date)) {
		if(!includeWeekends) {
			if(TimeDayOfWeek(date) != 6 && TimeDayOfWeek(date) != 0) {
				monthFirstTradingDay = date;
			}
		} else {
			monthFirstTradingDay = date;
		}
	}

	return monthFirstTradingDay==date;
}

//+------------------------------------------------------------------+

bool isMarketClosed(string symbol){
   return !IsTesting() && !IsOptimization() && MarketInfo(symbol, MODE_TRADEALLOWED) == 0;
}

//+------------------------------------------------------------------+

double fixLotSize(string symbol, double size){
   string correctedSymbol = correctSymbol(symbol);
   double Smallest_Lot = MarketInfo(correctedSymbol, MODE_MINLOT);
   double Largest_Lot = MarketInfo(correctedSymbol, MODE_MAXLOT);    
   double LotStep = MarketInfo(correctedSymbol, MODE_LOTSTEP);

   double mod = MathMod(size, LotStep);
   double finalSize = size;
   
   if(MathAbs(mod - LotStep) > 0.000001){
      finalSize -= mod;
   } 

   if(finalSize < Smallest_Lot){
      Verbose("Calculated lot size (", DoubleToStr(finalSize), ") is lower than minimum possible (", DoubleToStr(Smallest_Lot), "). Using minimum lot size...");
      return Smallest_Lot;
   }
   else if(finalSize > Largest_Lot){
      Verbose("Calculated lot size (", DoubleToStr(finalSize), ") is larger than maximum possible (", DoubleToStr(Largest_Lot), "). Using maximum lot size...");
      return Largest_Lot;
   }
   else {
      return finalSize;
   }
}
