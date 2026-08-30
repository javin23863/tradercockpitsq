
// Trailing Stop
void sqManageTrailingStop(ulong ticket) {
   if(!PositionSelectByTicket(ticket)){
       Verbose("Cannot select position with ticket ", IntegerToString(ticket));
       return;
   }
                                                                                            
   valueIdentificationSymbol = PositionGetString(POSITION_SYMBOL); 
   int symbolDigits = (int) SymbolInfoInteger(valueIdentificationSymbol, SYMBOL_DIGITS);

   double tsValue = NormalizeDouble(sqGetValueByIdentification( sqGetTrailingStop(ticket) ), symbolDigits);
   
   if(tsValue > 0) {
      double plValue;
      int error;

      int valueType = sqGetTrailingStopType(ticket);
      ENUM_POSITION_TYPE orderType = (ENUM_POSITION_TYPE) PositionGetInteger(POSITION_TYPE);

      if(orderType == POSITION_TYPE_BUY) {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = NormalizeDouble(sqGetBid(NULL) - tsValue, symbolDigits);
         }
      } else {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = NormalizeDouble(sqGetAsk(NULL) + tsValue, symbolDigits);
         }
      }
      
      // Added in Build 140 - fixes prices for futures (with 0.25 step and similar)
      tsValue = sqFixMarketPrice(tsValue, "Current");
      
      double tsActivation = NormalizeDouble(sqGetValueByIdentification(sqGetTSActivation(ticket)), symbolDigits);
      double currentSL = NormalizeDouble(PositionGetDouble(POSITION_SL), symbolDigits);       
      double openPrice = PositionGetDouble(POSITION_PRICE_OPEN); 
      double takeProfit = PositionGetDouble(POSITION_TP);
      
      if(orderType == POSITION_TYPE_BUY) {
         plValue = NormalizeDouble(sqGetBid(NULL) - openPrice, symbolDigits);

         if (plValue >= tsActivation && (currentSL == 0 || currentSL < tsValue)) {
            Verbose("Moving trailing stop for order with ticket: ", IntegerToString(ticket), " to :", DoubleToString(tsValue));
            if(!OrderModify(ticket, tsValue, takeProfit)) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(sqGetAsk(NULL)), ", Bid: ", DoubleToString(sqGetBid(NULL)), " Current SL: ",  DoubleToString(currentSL));
            }
         }
      } else { // orderType == OP_SELL
         plValue = NormalizeDouble(openPrice - sqGetAsk(NULL), symbolDigits);

         if (plValue >= tsActivation && (currentSL == 0 || currentSL > tsValue)) {
            Verbose("Moving trailing stop for order with ticket: ", IntegerToString(ticket), " to :", DoubleToString(tsValue));
            if(!OrderModify(ticket, tsValue, takeProfit)) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(sqGetAsk(NULL)), ", Bid: ", DoubleToString(sqGetBid(NULL)), " Current SL: ",  DoubleToString(currentSL));
            }
         }
      }
   }
}