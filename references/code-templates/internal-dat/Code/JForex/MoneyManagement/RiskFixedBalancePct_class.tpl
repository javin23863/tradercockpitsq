

private double sqMMRiskFixedBalancePct(String symbol, OrderCommand orderType, double price, double sl, double RiskInPercent, int Decimals, double LotsIfNoMM, double MaximumLots) throws JFException {
   print("Computing Money Management for order -  Risk fixed % of account balance");
   
   if(UseMoneyManagement == false) {
      print("Use Money Management = false, MM not used");
      return mmLotsIfNoMM;
   }
   
   symbol = correctSymbol(symbol);
   instrument = getInstrument(symbol);
   
   double openPrice = price > 0 ? price : (orderType.isLong() ? sqGetAsk(symbol) : sqGetBid(symbol));
   double LotSize=0;

   if(RiskInPercent < 0 ) {
      print("Computing Money Management - Incorrect RiskInPercent size, it must be above 0");
      return 0;
   }
   
   double PointValue = getPointValue(instrument);    
   double Smallest_Lot = instrument.getMinTradeAmount() / 100000;
   double Largest_Lot = instrument.getMaxTradeAmount()  / 100000;
   double LotStep = instrument.getTradeAmountIncrement()  / 100000;

   //Maximum amount of money to risk 
   double moneyToRisk = context.getAccount().getBalance() * RiskInPercent / 100;
		                                                                                                                                                       
   //Maximum drawdown of this order if we buy 1 lot 
   double oneLotSLDrawdown = PointValue * Math.abs(openPrice - sl);
		
   if(oneLotSLDrawdown > 0) {
	  LotSize = roundDown(moneyToRisk / oneLotSLDrawdown, Decimals);
   }
   else {
	  LotSize = 0;
   }

   //--- MAXLOT and MINLOT management

   print("Computing Money Management - Smallest_Lot: ", Smallest_Lot, ", Largest_Lot: ", Largest_Lot,", Computed LotSize: ", LotSize);
   print("Max money to risk: ", moneyToRisk, ", SL:", sl, ", One lot drawdown: ", oneLotSLDrawdown, ", Point value: ", PointValue);

   if(LotSize <= 0) {
      print("Calculated LotSize is <= 0. Using LotsIfNoMM value: ", LotsIfNoMM);
			LotSize = LotsIfNoMM;
	 }
   
   if(LotSize > MaximumLots) {
      print("LotSize is too big. LotSize set to maximal allowed value (MaximumLots): ", MaximumLots);
      LotSize = MaximumLots;
   }

   //--------------------------------------------

   if (LotSize < Smallest_Lot) {
      print("Calculated LotSize is too small. Minimal allowed lot size from the broker is: ", Smallest_Lot, ". Please, increase your risk or set fixed LotSize.");
      LotSize = 0;
   }
   else if (LotSize > Largest_Lot) {
      print("LotSize is too big. LotSize set to maximal allowed market value: ", Largest_Lot);
      LotSize = Largest_Lot;
   }

   return LotSize;
}