

double sqMMRiskFixedBalancePct(string symbol, int orderType, double price, double sl, double RiskInPercent, double LotsIfNoMM, double MaximumLots, double BaseCurrencyExchangeRate) {
   Verbose("Computing Money Management for order -  Risk fixed % of account balance");
   double slSize = sqConvertToPips(symbol, MathAbs(price - sl));
   if(slSize <= 0) {
      Verbose("Computing Money Management - Stop Loss is zero, using Lots if no MM: ", LotsIfNoMM);
      return(LotsIfNoMM);
   }
Verbose("Price: ", price, ", SL: ", sl, ", SLSize: ", slSize, ", RiskInPercent: ", RiskInPercent);


   double LotSize=0;

   if(RiskInPercent < 0 ) {
      Verbose("Computing Money Management - Incorrect RiskInPercent size, it must be above 0");
      return(0);
   }

   double riskPerTrade = (BaseCurrencyExchangeRate * AccountBalance() *  (RiskInPercent / 100.0));
   if(slSize <= 0) {
      Verbose("Computing Money Management - Incorrect StopLossPips size, it must be above 0");
      return(0);
   }
   Verbose("Risk per trade: ", riskPerTrade);

   double TickSize = MarketInfo(Symbol(),MODE_TICKSIZE);
   double TickValue = MarketInfo(Symbol(),MODE_TICKVALUE);
   double PointValue = MarketInfo(Symbol(),MODE_POINT);
   double LotStep = MarketInfo(Symbol(),MODE_LOTSTEP);

   if(Digits == 1 || Digits == 3 || Digits == 5) {
      slSize = 10 * slSize; // conversion from pips to points
   }

   Verbose("Computing Money Management - SL: ", slSize, ", Account Balance: ", AccountBalance(),", Tick value: ", TickValue,", Point: ", PointValue, ", LotStep: ", LotStep,", Tick size: ", TickSize);


   if(slSize>0 && TickValue>0)
   {
      LotSize = TickSize * riskPerTrade / (slSize * TickValue * PointValue );

      int err=GetLastError();
      if(err==4013)
      { //ERR_ZERO_DIVIDE
         Verbose("Err: division by zero: StopLoss:",slSize," TickValue:",TickValue," LotSize:",LotSize);
         return(-1);
      }
   }

   //--- MAXLOT and MINLOT management
   double Smallest_Lot = MarketInfo(Symbol(), MODE_MINLOT);
   double Largest_Lot = MarketInfo(Symbol(), MODE_MAXLOT);

   Verbose("Computing Money Management - Smallest_Lot: ", Smallest_Lot, ", Largest_Lot: ", Largest_Lot,", Computed LotSize: ", LotSize);

   if (LotSize < Smallest_Lot) LotSize = Smallest_Lot;
   if (LotSize > Largest_Lot) LotSize = Largest_Lot;

   if(LotSize > MaximumLots) {
      LotSize = MaximumLots;
   }

   //--------------------------------------------
   //--- LotSize rounded regarding Broker LOTSTEP

   if(LotStep==1) {
      LotSize=NormalizeDouble(LotSize,0);
   }
   if(LotStep==0.1) {
      LotSize=NormalizeDouble(LotSize,1);
   }
   if(LotStep==0.01) {
      LotSize=NormalizeDouble(LotSize,2);
   }
   if(LotStep==0.001) {
      LotSize=NormalizeDouble(LotSize,3);
   }

   //--------------------------------------------

   return (LotSize);
}