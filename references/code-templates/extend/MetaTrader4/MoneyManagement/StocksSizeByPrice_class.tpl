

double sqMMStockSizeByPrice(string symbol, int orderType, double price, bool UseAccountBalance, double MaximumLots, double multiplier, double sizeStep) {
   Verbose("Computing Money Management for order - Stock size by price");

   if(UseMoneyManagement == false) {
      Verbose("Use Money Management = false, MM not used");
      return (mmLotsIfNoMM);
   }
   
   symbol = correctSymbol(symbol);
   
   double openPrice = price > 0 ? price : (orderType == OP_BUY ? sqGetAsk(symbol) : sqGetBid(symbol));
   
   Verbose("Price: ", DoubleToStr(openPrice), ", UseAccountBalance: ", UseAccountBalance ? "yes" : "no", ", MaximumLots: ", DoubleToStr(MaximumLots));

   double LotSize = 0;

   if(UseAccountBalance) {
			LotSize = AccountBalance() / openPrice;
	 } else {
			LotSize = initialBalance / openPrice;
	 }
    
    //Order size multiplier
    LotSize = LotSize * multiplier;
	
    // round computed trade size 
	LotSize = roundDown(LotSize, sizeStep, 0);
	 
   double Smallest_Lot = MarketInfo(symbol, MODE_MINLOT);
   double Largest_Lot = MarketInfo(symbol, MODE_MAXLOT);    
   double LotStep = MarketInfo(symbol, MODE_LOTSTEP);

   //--- MAXLOT and MINLOT management

   Verbose("Computing Money Management - Smallest_Lot: ", DoubleToStr(Smallest_Lot), ", Largest_Lot: ", DoubleToStr(Largest_Lot), ", Computed LotSize: ", DoubleToStr(LotSize));
   
   if(LotSize <= 0) {
			return(0);
	 }
   
   if(LotSize > MaximumLots) {
      Verbose("LotSize is too big. LotSize set to maximal allowed value (MaximumLots): ", DoubleToStr(MaximumLots));
      LotSize = MaximumLots;
   }

   //--------------------------------------------

   if (LotSize < Smallest_Lot) {
      Verbose("LotSize is too small. Minimal allowed lot size: ", DoubleToStr(Smallest_Lot));
      LotSize = 0;
   }
   else if (LotSize > Largest_Lot) {
      Verbose("LotSize is too big. LotSize set to maximal allowed market value: ", DoubleToStr(Largest_Lot));
      LotSize = Largest_Lot;
   }

   return (LotSize);
}
