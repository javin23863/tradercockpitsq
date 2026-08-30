
// Move Stop Loss to Break Even
void sqManageSL2BE(ulong ticket) {
   if(!PositionSelectByTicket(ticket)){
       Verbose("Cannot select position with ticket ", IntegerToString(ticket));
       return;
   }
                                                                         
   valueIdentificationSymbol = PositionGetString(POSITION_SYMBOL); 
   int symbolDigits = (int) SymbolInfoInteger(valueIdentificationSymbol, SYMBOL_DIGITS);
   double moveSLAtValue = NormalizeDouble(sqGetValueByIdentification( sqGetMoveSL2BE(ticket) ), symbolDigits);

   if(moveSLAtValue > 0) {
      double newSL = 0;
      int error;

      int valueType = sqGetMoveSL2BEType(ticket);
      ENUM_POSITION_TYPE orderType = (ENUM_POSITION_TYPE) PositionGetInteger(POSITION_TYPE);
      double price;
      if(orderType == POSITION_TYPE_BUY) {
         price = sqGetBid(NULL);
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = NormalizeDouble(sqGetBid(NULL) - moveSLAtValue, symbolDigits);
         }
      } else {
         price = sqGetAsk(NULL);
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = NormalizeDouble(sqGetAsk(NULL) + moveSLAtValue, symbolDigits);
         }
      }
      
      double addPips = NormalizeDouble(sqGetValueByIdentification(sqGetSL2BEAddPips(ticket)) + 0.0000000001, symbolDigits);
      double currentSL = NormalizeDouble(PositionGetDouble(POSITION_SL), symbolDigits);
      double openPrice = PositionGetDouble(POSITION_PRICE_OPEN); 
      double takeProfit = PositionGetDouble(POSITION_TP);

      if(orderType == POSITION_TYPE_BUY) {
         newSL = NormalizeDouble(openPrice + addPips, symbolDigits);
         
         // Added in Build 140 - fixes prices for futures (with 0.25 step and similar)
         newSL = sqFixMarketPrice(newSL, "Current");
         
         if (openPrice <= moveSLAtValue  && price >= newSL && (currentSL == 0 || currentSL < newSL)) {
            Verbose("Moving SL 2 BE for order with ticket: ", IntegerToString(ticket), " to :", DoubleToString(newSL));
            if(!OrderModify(ticket, newSL, takeProfit)) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(sqGetAsk(NULL)), ", Bid: ", DoubleToString(sqGetBid(NULL)), " Current SL: ",  DoubleToString(currentSL));
            }
         }

      } else { // orderType == OP_SELL
         newSL = NormalizeDouble(openPrice - addPips, symbolDigits);

         // Added in Build 140 - fixes prices for futures (with 0.25 step and similar)
         newSL = sqFixMarketPrice(newSL, "Current");
         
         if (openPrice >= moveSLAtValue && price <= newSL && (currentSL == 0 || currentSL > newSL)) {
            Verbose("Moving SL 2 BE for order with ticket: ", IntegerToString(ticket), " to :", DoubleToString(newSL));
            if(!OrderModify(ticket, newSL, takeProfit)) {
               error = GetLastError();
                Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(sqGetAsk(NULL)), ", Bid: ", DoubleToString(sqGetBid(NULL)), " Current SL: ",  DoubleToString(currentSL));
            }
         }
      }
   }
}