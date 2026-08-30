

double sqMMRiskFixedAccountPct(string symbol, int orderType, double price, double sl, double RiskInPercent, int decimals, int StopLossPips, double LotsIfNoMM, double MaximumLots, double multiplier, double sizeStep) {
   Verbose("Computing Money Management for order -  Risk fixed % of account");

   if(UseMoneyManagement == false) {
      Verbose("Use Money Management = false, MM not used");
      return (mmLotsIfNoMM);
   }
      
   symbol = correctSymbol(symbol);
   
   double openPrice = price > 0 ? price : (orderType == OP_BUY ? sqGetAsk(symbol) : sqGetBid(symbol));
   double LotSize = 0;

   if(RiskInPercent < 0 ) {
      Verbose("Computing Money Management - Incorrect RiskInPercent size, it must be above 0");
      return(0);
   }
   
   double PointValue = MarketInfo(symbol, MODE_TICKVALUE) / MarketInfo(symbol, MODE_TICKSIZE);   
   double Smallest_Lot = MarketInfo(symbol, MODE_MINLOT);
   double Largest_Lot = MarketInfo(symbol, MODE_MAXLOT);    
   double LotStep = MarketInfo(symbol, MODE_LOTSTEP);
   
   if(sl == 0){
      sl = openPrice - StopLossPips * sqGetPointCoef(symbol);
   }
   
   sl = NormalizeDouble(sl, (int) MarketInfo(symbol, MODE_DIGITS));
   
   //Maximum amount of money to risk 
   double moneyToRisk = NormalizeDouble(AccountEquity() * RiskInPercent / 100, 7);
		
   //Maximum drawdown of this order if we buy 1 lot 
   double oneLotSLDrawdown = NormalizeDouble(PointValue * MathAbs(openPrice - sl), 7);

   // Currency conversion: check if the instrument's profit currency differs from the account currency
   string quote_currency = SymbolInfoString(symbol, SYMBOL_CURRENCY_PROFIT);
   string account_currency = AccountInfoString(ACCOUNT_CURRENCY);
   
   if(quote_currency != "" && account_currency != "" && quote_currency != account_currency) {
      double conversionRate = 0;
      bool converted = sqGetCurrencyConversionRate(quote_currency, account_currency, conversionRate);
      if(converted && conversionRate > 0) {
         Verbose("Currency conversion applied: ", quote_currency, " -> ", account_currency, ", rate: ", DoubleToStr(conversionRate));
         oneLotSLDrawdown *= conversionRate;
      }
      else {
         Verbose("WARNING: Could not find conversion rate from ", quote_currency, " to ", account_currency, ". Lot size may be incorrect.");
      }
   }

   if(oneLotSLDrawdown > 0) {
	  LotSize = moneyToRisk / oneLotSLDrawdown;
   }
   else {
	  LotSize = 0;
   }

    //Order size multiplier
    LotSize = LotSize * multiplier;
	
    // round computed trade size 
	LotSize = roundDown(LotSize, sizeStep, decimals);

   //--- MAXLOT and MINLOT management

   Verbose("MODE_TICKSIZE: ", DoubleToStr(MarketInfo(symbol, MODE_TICKSIZE)), ", MODE_TICKVALUE: ", DoubleToStr(MarketInfo(symbol, MODE_TICKVALUE)), ", MODE_POINT: ", DoubleToStr(MarketInfo(symbol, MODE_POINT)));
   Verbose("Computing Money Management - Smallest_Lot: ", DoubleToStr(Smallest_Lot), ", Largest_Lot: ", DoubleToStr(Largest_Lot),", Computed LotSize: ", DoubleToStr(LotSize));
   Verbose("Max money to risk: ", DoubleToStr(moneyToRisk), ", SL:", DoubleToStr(sl), ", One lot drawdown: ", DoubleToStr(oneLotSLDrawdown), ", Point value: ", DoubleToStr(PointValue));

   if(LotSize <= 0) {
      Verbose("Calculated LotSize is <= 0. Using LotsIfNoMM value: ", DoubleToStr(LotsIfNoMM), ")");
			LotSize = LotsIfNoMM;
	 }
   
   if(LotSize > MaximumLots) {
      Verbose("LotSize is too big. LotSize set to maximal allowed value (MaximumLots): ", DoubleToStr(MaximumLots));
      LotSize = MaximumLots;
   }

   //--------------------------------------------

   if (LotSize < Smallest_Lot) {
      Verbose("Calculated LotSize is too small. Minimal allowed lot size from the broker is: ", DoubleToStr(Smallest_Lot), ". Please, increase your risk or set fixed LotSize.");
      LotSize = 0;
   }
   else if (LotSize > Largest_Lot) {
      Verbose("LotSize is too big. LotSize set to maximal allowed market value: ", DoubleToStr(Largest_Lot));
      LotSize = Largest_Lot;
   }

   return (LotSize);
}

//----------------------------------------------------------------------------
// Currency conversion helper for MT4
//----------------------------------------------------------------------------
bool sqGetCurrencyConversionRate(string fromCurrency, string toCurrency, double &rate) {
   // Try direct pair: fromCurrency + toCurrency (e.g. JPYUSD)
   string pair = fromCurrency + toCurrency;
   double bid = MarketInfo(pair, MODE_BID);
   double ask = MarketInfo(pair, MODE_ASK);
   if(bid > 0 && ask > 0) {
      rate = (bid + ask) / 2.0;
      Verbose("Conversion Symbol found: ", pair);
      return true;
   }
   
   // Try inverse pair: toCurrency + fromCurrency (e.g. USDJPY)
   pair = toCurrency + fromCurrency;
   bid = MarketInfo(pair, MODE_BID);
   ask = MarketInfo(pair, MODE_ASK);
   if(bid > 0 && ask > 0) {
      rate = 1.0 / ((bid + ask) / 2.0);
      Verbose("Conversion Symbol found (inverse): ", pair);
      return true;
   }
   
   // Try with various separators and suffixes by searching all symbols
   int total = SymbolsTotal(true);
   for(int i = 0; i < total; i++) {
      string sym = SymbolName(i, true);
      if(StringFind(sym, fromCurrency + toCurrency) != -1) {
         bid = MarketInfo(sym, MODE_BID);
         ask = MarketInfo(sym, MODE_ASK);
         if(bid > 0 && ask > 0) {
            rate = (bid + ask) / 2.0;
            Verbose("Conversion Symbol found: ", sym);
            return true;
         }
      }
      if(StringFind(sym, toCurrency + fromCurrency) != -1) {
         bid = MarketInfo(sym, MODE_BID);
         ask = MarketInfo(sym, MODE_ASK);
         if(bid > 0 && ask > 0) {
            rate = 1.0 / ((bid + ask) / 2.0);
            Verbose("Conversion Symbol found (inverse): ", sym);
            return true;
         }
      }
   }
   
   rate = 1.0;
   Verbose("Conversion Symbol not found for ", fromCurrency, toCurrency);
   return false;
}
