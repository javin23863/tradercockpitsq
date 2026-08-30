
// Trailing Stop
void sqManageTrailingStop(IOrder order) throws JFException {
   ticket = order.getLabel();
	
   int val = sqGetGlobalIntVariable(ticket, "TrailingStop");
   if(val == 0) {
      return;
   }
   
   instrument = order.getInstrument();

   double tsValue = sqGetValueByIdentification(val, instrument);
   
   double bid = history.getLastTick(instrument).getBid();
   double ask = history.getLastTick(instrument).getAsk();
   
   if(tsValue > 0) {
      double plValue;
      int error;

      int valueType = sqGetGlobalIntVariable(ticket, "TrailingStopType");
      orderType = order.getOrderCommand();

      if(orderType.isLong()) {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = bid - tsValue;
         }
      } else {
         if(valueType == SLPTTYPE_RANGE) {
            tsValue = ask + tsValue;
         }
      }

      tsValue = sqFixMarketPrice(tsValue, instrument);

      double tsActivation = sqFixMarketPrice(sqGetValueByIdentification(sqGetGlobalIntVariable(ticket, "TrailingActivation"), instrument), instrument);
      double currentSL = order.getStopLossPrice();

      if(orderType.isLong()) {
         plValue = sqFixMarketPrice(bid - order.getOpenPrice(), instrument);

         if ((plValue >= tsActivation || sqDoublesAreEqual(plValue, tsActivation)) && (currentSL == 0 || currentSL < tsValue) && !sqDoublesAreEqual(currentSL, tsValue)) {
            print("Moving trailing stop for order with ticket: " + order.getLabel() + " to :" + tsValue);
            sqOrderModify(order, order.getOpenPrice(), tsValue, order.getTakeProfitPrice());
         }
      } else { // orderType == SELL
         plValue = sqFixMarketPrice(order.getOpenPrice() - ask, instrument);

         if ((plValue >= tsActivation || sqDoublesAreEqual(plValue, tsActivation)) && (currentSL == 0 || currentSL > tsValue) && !sqDoublesAreEqual(currentSL, tsValue)) {
            print("Moving trailing stop for order with ticket: " + order.getLabel() + " to :" + tsValue);
            sqOrderModify(order, order.getOpenPrice(), tsValue, order.getTakeProfitPrice());
         }
      }
   }
}