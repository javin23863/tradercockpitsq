
// Move Stop Loss to Break Even
void sqManageSL2BE(IOrder order) throws JFException {  
   ticket = order.getLabel();

   int val = sqGetGlobalIntVariable(ticket, "MoveSL2BE");
   if(val == 0) {
      return;
   }
   
   instrument = order.getInstrument();

   double bid = history.getLastTick(instrument).getBid();
   double ask = history.getLastTick(instrument).getAsk();

   double moveSLAtValue = sqGetValueByIdentification(val, instrument);

   if(moveSLAtValue > 0) {
      double newSL = 0;
      int error;

      int valueType = sqGetGlobalIntVariable(ticket, "MoveSL2BEType");
      orderType = order.getOrderCommand();

      if(orderType.isLong()) {
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = bid - moveSLAtValue;
         }
      } else {
         if(valueType == SLPTTYPE_RANGE) {
            moveSLAtValue = ask + moveSLAtValue;
         }
      }

      moveSLAtValue = sqFixMarketPrice(moveSLAtValue, instrument);
      double addPips = sqFixMarketPrice(sqGetValueByIdentification(sqGetGlobalIntVariable(ticket, "SL2BEAddPips"), instrument), instrument);
      
      double currentSL = order.getStopLossPrice();

      if(orderType.isLong()) {
         newSL = sqFixMarketPrice(order.getOpenPrice() + addPips, instrument);

         if ((order.getOpenPrice() <= moveSLAtValue || sqDoublesAreEqual(order.getOpenPrice(), moveSLAtValue)) && (currentSL == 0 || currentSL < newSL) && !sqDoublesAreEqual(currentSL, newSL)) {
            print("Moving SL 2 BE for order with ticket: " + ticket + " to :" + newSL);
            sqOrderModify(order, order.getOpenPrice(), newSL, order.getTakeProfitPrice());
         }

      } else { // orderType == SELL
         newSL = sqFixMarketPrice(order.getOpenPrice() - addPips, instrument);

         if ((order.getOpenPrice() >= moveSLAtValue || sqDoublesAreEqual(order.getOpenPrice(), moveSLAtValue)) && (currentSL == 0 || currentSL > newSL) && !sqDoublesAreEqual(currentSL, newSL)) {
            print("Moving SL 2 BE for order with ticket: " + ticket + "  to :" + newSL);
            sqOrderModify(order, order.getOpenPrice(), newSL, order.getTakeProfitPrice());
         }
      }
   }
}