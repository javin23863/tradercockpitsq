
private double sqMMSimpleMartingale(String symbol, OrderCommand orderType, int magicNumber, int Decimals) throws JFException {
   print("Computing Money Management for order - Simple Martingale MM");
   
   if(UseMoneyManagement == false) {
      print("Use Money Management = false, MM not used");
      return roundDown(mmLotsIfNoMM, Decimals);
   }
   
   symbol = correctSymbol(symbol);
   instrument = getInstrument(symbol);
   
   int direction = 0;
   if(mmSeparateByDirection) {
      direction = (orderType.isLong()) ? 1 : -1;
   }
   
   order = sqGetOrderInHistory(magicNumber, symbol, direction, "");
   if(order==null) {
      // there is no previous order
      print("Simple Martingale MM - no previous order found");
      return roundDown(mmLotsStart, Decimals);
   }

   double PL = order.getProfitLossInAccountCurrency();
   double lastOrderSize = order.getAmount();
   print("Simple Martingale MM - previous order found, PL: "+PL+", size: "+lastOrderSize);
   if(PL > 0) {
      // it was profit, reset
      return roundDown(mmLotsStart, Decimals);
   }
   
   double newSize = lastOrderSize * mmLotsMultiplier * 10;
   
   if(newSize > mmLotsReset) {
      // we reached maximum allowed size, reset it back to the start one
      print("Simple Martingale MM - exceeded maximum allowed size, resetting to start");
      return roundDown(mmLotsStart, Decimals);
   }
   
   return roundDown(newSize, Decimals);
}
