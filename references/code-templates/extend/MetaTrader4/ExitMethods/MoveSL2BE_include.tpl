
// Move Stop Loss to Break Even
void sqManageSL2BE(int ticket) {

   double val = sqGetGlobalVariable(ticket, "MoveSL2BE");
   if(val == 0) {
      return;
   }

   if(!OrderSelect(ticket, SELECT_BY_TICKET)) {
      return;
   }

   double moveSLAtValue = sqGetValueByIdentification((string) val);

   if(moveSLAtValue > 0) {
      double newSL = 0;
      int error;

      int valueType = (int) sqGetGlobalVariable(ticket, "MoveSL2BEType");
      int orderType = OrderType();
      int digits = (int) MarketInfo(OrderSymbol(), MODE_DIGITS);

      double price;
      if(orderType == OP_BUY || orderType == OP_BUYSTOP || orderType == OP_BUYLIMIT) {
         price = Bid;
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = Bid - moveSLAtValue;
         }
      } else {
         price = Ask;
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = Ask + moveSLAtValue;
         }
      }

      moveSLAtValue = NormalizeDouble(moveSLAtValue, digits);
      double addPips = NormalizeDouble(sqGetValueByIdentification((string) sqGetGlobalVariable(ticket, "SL2BEAddPips")), digits);
      
      double currentSL = OrderStopLoss();

      if(orderType == OP_BUY) {
         newSL = NormalizeDouble(OrderOpenPrice() + addPips, digits);

         if ((OrderOpenPrice() <= moveSLAtValue || sqDoublesAreEqual(OrderOpenPrice(), moveSLAtValue)) && (price >= newSL  || sqDoublesAreEqual(price, newSL)) && (currentSL == 0 || currentSL < newSL) && !sqDoublesAreEqual(currentSL, newSL)) {
            Verbose("Moving SL 2 BE for order with ticket: ", IntegerToString(OrderTicket()), " to :", DoubleToString(newSL));
            if(!sqOrderModify(OrderTicket(), OrderOpenPrice(), newSL, OrderTakeProfit())) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(Ask), ", Bid: ", DoubleToString(Bid), " Current SL: ",  DoubleToString(currentSL));
            }
         }

      } else { // orderType == OP_SELL
         newSL = NormalizeDouble(OrderOpenPrice() - addPips, digits);

         if ((OrderOpenPrice() >= moveSLAtValue || sqDoublesAreEqual(OrderOpenPrice(), moveSLAtValue)) && (price <= newSL  || sqDoublesAreEqual(price, newSL)) && (currentSL == 0 || currentSL > newSL) && !sqDoublesAreEqual(currentSL, newSL)) {
            Verbose("Moving SL 2 BE for order with ticket: ", IntegerToString(OrderTicket()), "  to :", DoubleToString(newSL));
            if(!sqOrderModify(OrderTicket(), OrderOpenPrice(), newSL, OrderTakeProfit())) {
               error = GetLastError();
               Verbose("Failed, error: ", IntegerToString(error), " - ", ErrorDescription(error),", Ask: ", DoubleToString(Ask), ", Bid: ", DoubleToString(Bid), " Current SL: ", DoubleToString(currentSL));
            }
         }
      }
   }
}