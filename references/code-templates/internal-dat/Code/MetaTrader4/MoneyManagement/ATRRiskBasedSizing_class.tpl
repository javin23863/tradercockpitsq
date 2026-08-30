double sqMMATRRiskBasedSizing(string symbol, int orderType, double price, double sl, double RiskInPercent, int ATRPeriod, double ATRMultiplier, int decimals, double LotsIfNoMM, double MaximumLots, double multiplier, double sizeStep) {
   Verbose("Computing Money Management - ATR Risk-Based Sizing");

   if(UseMoneyManagement == false) {
      Verbose("Use Money Management = false, MM not used");
      return (mmLotsIfNoMM);
   }
      
   symbol = correctSymbol(symbol);
   
   double openPrice = price > 0 ? price : (orderType == OP_BUY ? sqGetAsk(symbol) : sqGetBid(symbol));
   double LotSize = 0;

   if(RiskInPercent < 0) {
      Verbose("Computing Money Management - Incorrect RiskInPercent size, it must be above 0");
      return(0);
   }
   
   // Get ATR value
   double atrValue = iATR(symbol, 0, ATRPeriod, 1);
   
   if(atrValue <= 0) {
      Verbose("ATR value is invalid or 0, using LotsIfNoMM");
      return(LotsIfNoMM);
   }
   
   double TickSize = MarketInfo(symbol, MODE_TICKSIZE);
   double PointValue = MarketInfo(symbol, MODE_TICKVALUE) / TickSize;   
   double Smallest_Lot = MarketInfo(symbol, MODE_MINLOT);
   double Largest_Lot = MarketInfo(symbol, MODE_MAXLOT);    
   double LotStep = MarketInfo(symbol, MODE_LOTSTEP);
   
   // Calculate stop distance in ticks (ATR × ATRMult) ÷ tickSize
   double stopDistanceTicks = (atrValue * ATRMultiplier) / TickSize;
   
   // Stop loss in money
   double slInMoney = NormalizeDouble(stopDistanceTicks * TickSize * PointValue, 7);
   
   // Maximum amount of money to risk 
   double moneyToRisk = NormalizeDouble(AccountEquity() * RiskInPercent / 100, 7);
   
   if(slInMoney > 0) {
      LotSize = moneyToRisk / slInMoney;
   }
   else {
      LotSize = 0;
   }

   // Order size multiplier
   LotSize = LotSize * multiplier;
   
   // Round computed trade size 
   LotSize = roundDown(LotSize, sizeStep, decimals);

   //--- MAXLOT and MINLOT management

   Verbose("MODE_TICKSIZE: ", DoubleToStr(TickSize), ", MODE_TICKVALUE: ", DoubleToStr(MarketInfo(symbol, MODE_TICKVALUE)), ", ATR: ", DoubleToStr(atrValue));
   Verbose("Computing Money Management - Smallest_Lot: ", DoubleToStr(Smallest_Lot), ", Largest_Lot: ", DoubleToStr(Largest_Lot), ", Computed LotSize: ", DoubleToStr(LotSize));
   Verbose("Max money to risk: ", DoubleToStr(moneyToRisk), ", ATR Stop Distance (ticks): ", DoubleToStr(stopDistanceTicks), ", SL in money: ", DoubleToStr(slInMoney), ", Point value: ", DoubleToStr(PointValue));

   if(LotSize <= 0) {
      Verbose("Calculated LotSize is <= 0. Using LotsIfNoMM value: ", DoubleToStr(LotsIfNoMM));
      LotSize = LotsIfNoMM;
   }
   
   if(LotSize > MaximumLots) {
      Verbose("LotSize is too big. LotSize set to maximal allowed value (MaximumLots): ", DoubleToStr(MaximumLots));
      LotSize = MaximumLots;
   }

   //--------------------------------------------

   if(LotSize < Smallest_Lot) {
      Verbose("Calculated LotSize is too small. Minimal allowed lot size from the broker is: ", DoubleToStr(Smallest_Lot), ". Please, increase your risk or set fixed LotSize.");
      LotSize = 0;
   }
   else if(LotSize > Largest_Lot) {
      Verbose("LotSize is too big. LotSize set to maximal allowed market value: ", DoubleToStr(Largest_Lot));
      LotSize = Largest_Lot;
   }

   return (LotSize);
}