

double sqMMStockSizeByPrice(String symbol, OrderCommand orderType, double price, boolean UseAccountBalance, double MaximumLots) throws JFException {
   print("Computing Money Management for order - Stock size by price");
   
   if(UseMoneyManagement == false) {
      print("Use Money Management = false, MM not used");
      return mmLotsIfNoMM;
   }
   
   symbol = correctSymbol(symbol);
   
   double openPrice = price > 0 ? price : (orderType.isLong() ? sqGetAsk(symbol) : sqGetBid(symbol));
   
   print("Price: ", openPrice, ", UseAccountBalance: ", UseAccountBalance, ", MaximumLots: ", MaximumLots);

   double LotSize = 0;

   if(UseAccountBalance) {
			LotSize = context.getAccount().getBalance() / openPrice;
	 } else {
			LotSize = initialBalance / openPrice;
	 }

	 // round computed trade size 
	 LotSize = roundDown(LotSize, 0);
	 
   double Smallest_Lot = instrument.getMinTradeAmount() / 100000;
   double Largest_Lot = instrument.getMaxTradeAmount()  / 100000;
   double LotStep = instrument.getTradeAmountIncrement()  / 100000;

   //--- MAXLOT and MINLOT management

   print("Computing Money Management - Smallest_Lot: ", Smallest_Lot, ", Largest_Lot: ", Largest_Lot,", Computed LotSize: ", LotSize);
   
   if(LotSize <= 0) {
			return(0);
	 }
   
   if(LotSize > MaximumLots) {
      print("LotSize is too big. LotSize set to maximal allowed value (MaximumLots): ", MaximumLots);
      LotSize = MaximumLots;
   }

   //--------------------------------------------

   if (LotSize < Smallest_Lot) {
      print("LotSize is too small. Minimal allowed lot size: ", Smallest_Lot);
      LotSize = 0;
   }
   else if (LotSize > Largest_Lot) {
      print("LotSize is too big. LotSize set to maximal allowed market value: ", Largest_Lot);
      LotSize = Largest_Lot;
   }

   return (LotSize);
}