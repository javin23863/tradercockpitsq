double sqMMSimpleMartingale(string symbol, int orderType, int magicNumber, double multiplier, double sizeStep, int decimals) {
   Verbose("Computing Money Management for order - Simple Martingale MM");

   if(UseMoneyManagement == false) {
      Verbose("Use Money Management = false, MM not used");
      return roundDown(mmLotsIfNoMM * multiplier, sizeStep, decimals);
   }
   
   symbol = correctSymbol(symbol);
   
   int direction = 0;
   if(mmSeparateByDirection) {
      direction = (orderType == OP_BUY || orderType == OP_BUYLIMIT || orderType == OP_BUYSTOP) ? 1 : -1;
   }
   
   if(!sqSelectOrderInHistory(magicNumber, symbol, direction, "")) {
      // there is no previous order
      Verbose("Simple Martingale MM - no previous order found");
      return roundDown(mmLotsStart * multiplier, sizeStep, decimals);
   }

   double PL = OrderProfit();
   double lastOrderSize = OrderLots();
   Verbose("Simple Martingale MM - previous order found, PL: " + DoubleToStr(PL) + ", size: " + DoubleToStr(lastOrderSize));
   if(PL > 0) {
      // it was profit, reset
      return roundDown(mmLotsStart * multiplier, sizeStep, decimals);
   }
   
   double newSize = lastOrderSize * mmLotsMultiplier;
   
   if(newSize > mmLotsReset) {
      // we reached maximum allowed size, reset it back to the start one
      Verbose("Simple Martingale MM - exceeded maximum allowed size, resetting to start");
      return roundDown(mmLotsStart * multiplier, sizeStep, decimals);
   }
   
   return roundDown(newSize * multiplier, sizeStep, decimals);
}
