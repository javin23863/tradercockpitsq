
// Trailing Stop
void sqManageTrailingStop(int ticket) {

   double val = sqGetGlobalVariable(ticket, "TrailingStop");
   if(val == 0) {
      return;
   }

   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      return;
   }
   
   double tsValue = sqGetValueByIdentification((string) val);
   
   if(tsValue > 0) {
      double plValue;
      int error;

      int valueType = (int) sqGetGlobalVariable(ticket, "TrailingStopType");
      int orderType = OrderType();
      int digits = (int) MarketInfo(OrderSymbol(), MODE_DIGITS);

      if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = Bid - tsValue;
         }
      } else {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = Ask + tsValue;
         }
      }

      tsValue = NormalizeDouble(tsValue, digits);

      double tsActivation = NormalizeDouble(sqGetValueByIdentification((string) sqGetGlobalVariable(ticket, "TrailingActivation")), digits);
      double currentSL = OrderStopLoss();

      if(orderType == OP_BUY) {
         plValue = NormalizeDouble(Bid - OrderOpenPrice(), digits);

         if ((plValue >= tsActivation || sqDoublesAreEqual(plValue, tsActivation)) && (currentSL == 0 || currentSL < tsValue) && !sqDoublesAreEqual(currentSL, tsValue)) {
            Verbose("Moving trailing stop for order with ticket: ", IntegerToString(OrderTicket()), " to :", DoubleToString(tsValue));
            if(!sqOrderModify(OrderTicket(), OrderOpenPrice(), tsValue, OrderTakeProfit())) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(Ask), ", Bid: ", DoubleToString(Bid), " Current SL: ",  DoubleToString(currentSL));
            }
         }
      } else { // orderType == OP_SELL
         plValue = NormalizeDouble(OrderOpenPrice() - Ask, digits);

         if ((plValue >= tsActivation || sqDoublesAreEqual(plValue, tsActivation)) && (currentSL == 0 || currentSL > tsValue) && !sqDoublesAreEqual(currentSL, tsValue)) {
            Verbose("Moving trailing stop for order with ticket: ", IntegerToString(OrderTicket()), " to :", DoubleToString(tsValue));
            if(!sqOrderModify(OrderTicket(), OrderOpenPrice(), tsValue, OrderTakeProfit())) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(Ask), ", Bid: ", DoubleToString(Bid), " Current SL: ",  DoubleToString(currentSL));
            }
         }
      }
   }
}