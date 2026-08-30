

double sqMMFixedAmount(string symbol, ENUM_ORDER_TYPE orderType, double price, double sl, double RiskedMoney, int decimals, double LotsIfNoMM, double MaximumLots, double multiplier, double sizeStep) {
   Verbose("Computing Money Management for order - Fixed amount");
   
   if(UseMoneyManagement == false) {
      Verbose("Use Money Management = false, MM not used");
      return (mmLotsIfNoMM);
   }
      
   string correctedSymbol = correctSymbol(symbol);
   sl = NormalizeDouble(sl, (int) SymbolInfoInteger(correctedSymbol, SYMBOL_DIGITS));
   
   double openPrice = price > 0 ? price : SymbolInfoDouble(correctedSymbol, isLongOrder(orderType) ? SYMBOL_ASK : SYMBOL_BID);
   double LotSize=0;

   if(RiskedMoney <= 0 ) {
      Verbose("Computing Money Management - Incorrect RiskedMoney value, it must be above 0");
      return(0);
   }
   
   string quote_currency = SymbolInfoString(correctedSymbol, SYMBOL_CURRENCY_PROFIT);     
   string account_currency = AccountInfoString(ACCOUNT_CURRENCY);
   string SymbolCurrencyMargin = SymbolInfoString(correctedSymbol, SYMBOL_CURRENCY_MARGIN);
   double contract_size = SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_CONTRACT_SIZE);
   
   double PointValue = SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_VALUE) / SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_SIZE); 
   double Smallest_Lot = SymbolInfoDouble(correctedSymbol, SYMBOL_VOLUME_MIN);
   double Largest_Lot = SymbolInfoDouble(correctedSymbol, SYMBOL_VOLUME_MAX);    
   double LotStep = SymbolInfoDouble(correctedSymbol, SYMBOL_VOLUME_STEP);
   double dbTickValue  = SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_VALUE); 
   double dbTickSize   = SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_SIZE); 
   double dbPointSize  = SymbolInfoDouble(correctedSymbol, SYMBOL_POINT);          
   double dbPointValue = dbTickValue * dbPointSize / dbTickSize;     
                  
   int LotStepdecimals = CountDigits(LotStep, 8);
   int TickSizedecimals = CountDigits(SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_SIZE), 8);

   if (decimals > LotStepdecimals)                                                      
   {
      Verbose("Decimals is greater than Symbol digits value. Using Symbol digits value: ", DoubleToString(LotStepdecimals, 0), ")");
      decimals = LotStepdecimals;
   } 
   if (LotsIfNoMM < Smallest_Lot)
   {
      Verbose("LotsIfNoMM is less than Symbol Volume Min value. Using Symbol Volume Min value: ", DoubleToString(Smallest_Lot), ")");
      LotsIfNoMM = Smallest_Lot;
   }
   if (LotsIfNoMM > Largest_Lot)
   {
      Verbose("LotsIfNoMM is greater than Symbol Volume Max value. Using Symbol Volume Max value: ", DoubleToString(Largest_Lot), ")");
      LotsIfNoMM = Largest_Lot;
   } 
   if (MaximumLots < Smallest_Lot)
   {
      Verbose("MaximumLots is less than Symbol Volume Min value. Using Symbol Volume Min value: ", DoubleToString(Smallest_Lot), ")");
      MaximumLots = Smallest_Lot;
   }
   if (MaximumLots > Largest_Lot)
   {
      Verbose("MaximumLots is greater than Symbol Volume Max value. Using Symbol Volume Max value: ", DoubleToString(Largest_Lot), ")");
      MaximumLots = Largest_Lot;
   }
   	
    if (!MMinit){
      Verbose("SYMBOL_TRADE_TICK_VALUE: ", DoubleToString(dbTickValue));
      Verbose("SYMBOL_TRADE_TICK_VALUE_PROFIT: ", DoubleToString(SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_VALUE_PROFIT)));
      Verbose("SYMBOL_TRADE_TICK_VALUE_LOSS: ", DoubleToString(SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_VALUE_LOSS)));
      Verbose("SYMBOL_TRADE_TICK_SIZE : ", DoubleToString(SymbolInfoDouble(correctedSymbol, SYMBOL_TRADE_TICK_SIZE)));
      Verbose("SYMBOL_POINT: ", DoubleToString(dbPointSize));
      Verbose("dbPointValue: ", DoubleToString(dbPointValue));
      Verbose("Contract size: ", DoubleToString(NormalizeDouble(contract_size, 0), 0));
      Verbose("quote_currency: ", quote_currency);
      Verbose("SymbolCurrencyMargin: ", SymbolCurrencyMargin);
      Verbose("account_currency: ", account_currency);
   } 
   
   double freeMargin = AccountInfoDouble(ACCOUNT_MARGIN_FREE);
   Verbose("Free Margin: ", DoubleToString(NormalizeDouble(freeMargin, 2), 2), " ", account_currency);
   
   int CalcMode = (int)SymbolInfoInteger(correctedSymbol, SYMBOL_TRADE_CALC_MODE);
   bool PotentiallyRequiringConversion = false;                          
   string CalcMode_name;
   string PointValueCurrency = "";
   string PointValue_conversion_pair = "";
   bool PointValue_conversion_inverse = false;
   double PointValue_conversion_ratio = 0;
   
   switch(CalcMode)
   {
       case SYMBOL_CALC_MODE_FOREX:              
         CalcMode_name = "Forex mode"; 
         PointValueCurrency = account_currency;
         break;
       case SYMBOL_CALC_MODE_FOREX_NO_LEVERAGE:  
         CalcMode_name = "Forex No Leverage mode"; 
         PointValueCurrency = account_currency;
         break;
       case SYMBOL_CALC_MODE_FUTURES:            
         CalcMode_name = "Futures mode"; 
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;   
       case SYMBOL_CALC_MODE_CFD:                
         CalcMode_name = "CFD mode";                                                                                  
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break; 
       case SYMBOL_CALC_MODE_CFDINDEX:           
         CalcMode_name = "CFD Index mode";                                                                            
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         quote_conversion_ratio = 1;
         break;        
       case SYMBOL_CALC_MODE_CFDLEVERAGE:        
         CalcMode_name = "CFD Leverage mode";                                                                             
         PotentiallyRequiringConversion = true;
         
         PointValueCurrency = quote_currency;
         if(quote_currency != account_currency){
         if (GetConversionRate(account_currency, quote_currency, PointValue_conversion_pair, PointValue_conversion_inverse, PointValue_conversion_ratio)){
            Verbose("Conversion rate: ", DoubleToString(NormalizeDouble(PointValue_conversion_ratio, 5), 5));
            PointValue *= PointValue_conversion_ratio;
            Verbose("PointValue: ", DoubleToString(NormalizeDouble(PointValue, 4), 4), " ",  account_currency, " with conversion");
         }}
         break;           
       case SYMBOL_CALC_MODE_EXCH_STOCKS:        
         CalcMode_name = "Exchange Stocks mode";                                                                         
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;                
       case SYMBOL_CALC_MODE_EXCH_STOCKS_MOEX:   
         CalcMode_name = "Exchange Stocks Moex mode";                                                                     
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break; 
       case SYMBOL_CALC_MODE_EXCH_FUTURES:       
         CalcMode_name = "Exchange Futures mode"; 
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;
       case SYMBOL_CALC_MODE_EXCH_FUTURES_FORTS: 
         CalcMode_name = "FORTS Futures mode"; 
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;
       case SYMBOL_CALC_MODE_EXCH_BONDS:         
         CalcMode_name = "Exchange Bonds mode";                                                                       
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;                
       case SYMBOL_CALC_MODE_EXCH_BONDS_MOEX:    
         CalcMode_name = "Exchange MOEX Bonds mode";                                                                
         PotentiallyRequiringConversion = true;
         PointValueCurrency = quote_currency;
         break;  
       case SYMBOL_CALC_MODE_SERV_COLLATERAL:    
         CalcMode_name = "Collateral mode"; 
         break;
       default:                                  
         CalcMode_name = "Unknown"; 
         break;
   }
   if (!MMinit)
   {
      Verbose("The name of the broker = ",AccountInfoString(ACCOUNT_COMPANY));
      Verbose("Calculation Mode : ", CalcMode_name);
      MMinit = true;
   }
     
    //Maximum drawdown of this order if we buy 1 lot 
   double oneLotSLDrawdown = NormalizeDouble(PointValue * MathAbs(openPrice - sl), 7);
   
   if(PotentiallyRequiringConversion && quote_currency != account_currency)                                                                                                                  
   {  
         quote_conversion_ratio = 1;
         bool ret = GetConversionRate(quote_currency, account_currency, quote_conversion_pair, quote_conversion_inverse, quote_conversion_ratio);
         if (ret){
            Verbose("Conversion rate: ", DoubleToString(NormalizeDouble(quote_conversion_ratio, 5), 5));
            oneLotSLDrawdown *= quote_conversion_ratio;
            Verbose("oneLotSLDrawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 4), 4), " ",  account_currency, " with conversion");
         }
   }	
   else
   {
      Verbose("oneLotSLDrawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ",  account_currency);         
   }
   
   if(oneLotSLDrawdown > 0) 
   {
	  LotSize = RiskedMoney / oneLotSLDrawdown;
   }
   else 
   {
	  LotSize = 0;
   }

    //Order size multiplier
    LotSize = LotSize * multiplier;
	
    //round computed trade size 
	LotSize = roundDown(LotSize, sizeStep, decimals, symbol);
                                                
   //--- MAXLOT and MINLOT management 
   Verbose("Computing Money Management - Smallest_Lot: ", DoubleToString(NormalizeDouble(Smallest_Lot, 2), 2), ", Largest_Lot: ", DoubleToString(NormalizeDouble(Largest_Lot, 2), 2), ", LotStep: ", DoubleToString(NormalizeDouble(LotStep, 4), 4), ", contract_size: ", DoubleToString(NormalizeDouble(contract_size, 0), 0), ", Unfiltered Computed Lot Size: ", DoubleToString(NormalizeDouble(LotSize, LotStepdecimals), LotStepdecimals));
   if(LotSize > 0 && LotSize <= MaximumLots && LotSize >= Smallest_Lot && LotSize <= Largest_Lot) {
     string temp = ", Price: " + DoubleToString(NormalizeDouble(openPrice, TickSizedecimals), TickSizedecimals)  + " " + quote_currency + ", SL:" + DoubleToString(NormalizeDouble(sl, TickSizedecimals), TickSizedecimals)  + " " + quote_currency;
     Verbose("Money to risk: ", DoubleToString(RiskedMoney, 2), " ", account_currency, ", Max 1 lot trade drawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ", account_currency, temp, ", Point value: ",  DoubleToString(NormalizeDouble(PointValue, 2), 2), " ",  PointValueCurrency);       
   }
   else
   {
      Verbose("Initial Money to risk : ", DoubleToString(RiskedMoney, 2), " ", account_currency);
   }

   if(LotSize <= 0) {
      //round computed trade size 
      LotsIfNoMM = roundDown(LotsIfNoMM, sizeStep, decimals, symbol);
      Verbose("Calculated Lot Size is <= 0 Using LotsIfNoMM value: ", DoubleToString(NormalizeDouble(LotsIfNoMM, LotStepdecimals), LotStepdecimals), ")");
      LotSize = LotsIfNoMM;
      double moneyToRisk = LotSize * oneLotSLDrawdown;
      Verbose("Filtered Computed Lot Size: ", DoubleToString(NormalizeDouble(LotSize, LotStepdecimals), LotStepdecimals));
      string temp = ", Price: " + DoubleToString(NormalizeDouble(openPrice, TickSizedecimals), TickSizedecimals)  + " " + quote_currency + ", SL:" + DoubleToString(NormalizeDouble(sl, TickSizedecimals), TickSizedecimals)  + " " + quote_currency;
      Verbose("Money to risk: ", DoubleToString(moneyToRisk, 2), " ", account_currency,  ", Max 1 lot trade drawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ", account_currency, temp, ", Point value: ",  DoubleToString(NormalizeDouble(PointValue, 2), 2), " ",  PointValueCurrency);     
	}
   
   if(LotSize > MaximumLots) {
      //round computed trade size 
      MaximumLots = roundDown(MaximumLots, sizeStep, decimals, symbol);
      Verbose("Lot Size is too big. Lot Size set to maximal allowed value (MaximumLots): ", DoubleToString(NormalizeDouble(MaximumLots, LotStepdecimals), LotStepdecimals));
      LotSize = MaximumLots;
      double moneyToRisk = LotSize * oneLotSLDrawdown;
      Verbose("Filtered Computed Lot Size: ", DoubleToString(NormalizeDouble(LotSize, LotStepdecimals), LotStepdecimals));
      string temp = ", Price: " + DoubleToString(NormalizeDouble(openPrice, TickSizedecimals), TickSizedecimals)  + " " + quote_currency + ", SL:" + DoubleToString(NormalizeDouble(sl, TickSizedecimals), TickSizedecimals)  + " " + quote_currency;
      Verbose("Money to risk: ", DoubleToString(moneyToRisk, 2), " ", account_currency, ", Max 1 lot trade drawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ", account_currency, temp, ", Point value: ",  DoubleToString(NormalizeDouble(PointValue, 2), 2), " ",  PointValueCurrency);       
   }

   //--------------------------------------------

   if (LotSize < Smallest_Lot) {
      //round computed trade size 
      Smallest_Lot = roundDown(Smallest_Lot, sizeStep, decimals, symbol);
      Verbose("Calculated Lot Size is too small. Minimal allowed lot size from the broker is: ", DoubleToString(NormalizeDouble(Smallest_Lot, LotStepdecimals), LotStepdecimals), ". Please, increase your risk or set fixed LotSize.");
      LotSize = Smallest_Lot;
      double moneyToRisk = LotSize * oneLotSLDrawdown;
      Verbose("Filtered Computed Lot Size: ", DoubleToString(NormalizeDouble(LotSize, LotStepdecimals), LotStepdecimals));
      string temp = ", Price: " + DoubleToString(NormalizeDouble(openPrice, TickSizedecimals), TickSizedecimals)  + " " + quote_currency + ", SL:" + DoubleToString(NormalizeDouble(sl, TickSizedecimals), TickSizedecimals)  + " " + quote_currency;
      Verbose("Money to risk: ", DoubleToString(moneyToRisk, 2), " ", account_currency, ", Max 1 lot trade drawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ", account_currency, temp, ", Point value: ",  DoubleToString(NormalizeDouble(PointValue, 2), 2), " ",  PointValueCurrency);       
   }
   else if (LotSize > Largest_Lot) {
      //round computed trade size 
      Largest_Lot = roundDown(Largest_Lot, sizeStep, decimals, symbol);
      Verbose("Lot Size is too big. Lot Size set to maximal allowed market value: ", DoubleToString(NormalizeDouble(Largest_Lot, LotStepdecimals), LotStepdecimals));
      LotSize = Largest_Lot;
      double moneyToRisk = LotSize * oneLotSLDrawdown;
      Verbose("Filtered Computed Lot Size: ", DoubleToString(NormalizeDouble(LotSize, LotStepdecimals), LotStepdecimals));
      string temp = ", Price: " + DoubleToString(NormalizeDouble(openPrice, TickSizedecimals), TickSizedecimals)  + " " + quote_currency + ", SL:" + DoubleToString(NormalizeDouble(sl, TickSizedecimals), TickSizedecimals)  + " " + quote_currency;
      Verbose("Money to risk: ", DoubleToString(moneyToRisk, 2), " ", account_currency, ", Max 1 lot trade drawdown: ", DoubleToString(NormalizeDouble(oneLotSLDrawdown, 2), 2), " ", account_currency, temp, ", Point value: ",  DoubleToString(NormalizeDouble(PointValue, 2), 2), " ",  PointValueCurrency);       
   }

   return (LotSize);
}


//----------------------------------------------------------------------------
int CountDigits(double val, int maxPrecision = 8)
{
   int digits = 0;
   while(NormalizeDouble(val,digits) != NormalizeDouble(val, maxPrecision))  digits++;
   return digits;
}

//----------------------------------------------------------------------------
bool GetConversionRate(string from_currency, string to_currency, string &conversion_pair, bool &conversion_inverse, double &conversion_ratio)
{
   bool withoutConversionPair = false;
   if (conversion_pair == "") {
      withoutConversionPair = true;
      conversion_pair = GetSymbolWithExtension(from_currency, to_currency);
   }
   if(withoutConversionPair && conversion_pair != "") 
   {
      conversion_inverse = false;
      Verbose("Conversion Symbol found : ", conversion_pair);
   }

   withoutConversionPair = false;
   if (conversion_pair == "") {
      withoutConversionPair = true;
      conversion_pair = GetSymbolWithExtension(to_currency, from_currency);
   }
   if(withoutConversionPair && conversion_pair != "") 
   {
      conversion_inverse = true;
      Verbose("Conversion Symbol found : ", conversion_pair);
   }
   
   if(conversion_pair != "")
   {
      if (!conversion_inverse)
      {
         conversion_ratio = (SymbolInfoDouble(conversion_pair, SYMBOL_BID) + SymbolInfoDouble(conversion_pair, SYMBOL_ASK)) / 2;
         return true;
      }
      else 
      {
         if ((SymbolInfoDouble(conversion_pair, SYMBOL_BID) + SymbolInfoDouble(conversion_pair, SYMBOL_ASK)) / 2 != 0)
         {
            conversion_ratio =  1.0 / ((SymbolInfoDouble(conversion_pair, SYMBOL_BID) + SymbolInfoDouble(conversion_pair, SYMBOL_ASK)) / 2);
            return true;
         }
         else
         {
            conversion_ratio = 1.0;
            return false;
         }
      }
   }
   else
   {
      conversion_pair = "NA";
      Verbose("Conversion Symbol not found : ");
      conversion_ratio = 1.0;
      return false;
   }
}
//----------------------------------------------------------------------------
//----------------------------------------------------------------------------
// Function to get the correct symbol with an extension
string GetSymbolWithExtension(string base_currency, string quote_currency)
{
   string conversion_pair = base_currency + quote_currency;
   if (SymbolSelect(conversion_pair, true)) return conversion_pair;     

   // Now search through all symbols available on the server
   int total_symbols = SymbolsTotal(true);  
   for(int i = 0; i < total_symbols; i++)
   {
      string symbol_name = SymbolName(i, true);
     
      if(StringFind(symbol_name, conversion_pair) != -1)               
      {
         if(SymbolSelect(symbol_name, true))                          
         {
            return symbol_name;                                        
         }
      }
   }
   
   conversion_pair = base_currency + "/" + quote_currency;
   if (SymbolSelect(conversion_pair, true)) return conversion_pair;    

   // Now search through all symbols available on the server
   for(int i = 0; i < total_symbols; i++)
   {
      string symbol_name = SymbolName(i, true);
     
      if(StringFind(symbol_name, conversion_pair) != -1)              
      {
         if(SymbolSelect(symbol_name, true))                         
         {
            return symbol_name;                                       
         }
      }
   }
   
   conversion_pair = base_currency + "-" + quote_currency;
   if (SymbolSelect(conversion_pair, true)) return conversion_pair;    

   // Now search through all symbols available on the server
   for(int i = 0; i < total_symbols; i++)
   {
      string symbol_name = SymbolName(i, true);
     
      if(StringFind(symbol_name, conversion_pair) != -1)               
      {
         if(SymbolSelect(symbol_name, true))                            
         {
            return symbol_name;                                        
         }
      }
   }
   
   return ""; 
}
