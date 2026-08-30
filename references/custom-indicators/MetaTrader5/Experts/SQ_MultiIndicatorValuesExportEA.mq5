//+------------------------------------------------------------------+
//|   Exports multiple indicators into separate csv files            |
//|   Each file = one indicator, one line per closed bar             |
//+------------------------------------------------------------------+
#property strict

struct ExportItem {
   string name;      // file name
   int    handle;    // indicator handle
   int    buffer;    // buffer index
   int    fh;        // file handle
};

ExportItem exports[];
datetime lastBar=0;
input double Part2Export = 1; 

// helper macro (inline one-liner)
#define ADD_EXPORT(NAME,HANDLE,BUFFER) \
   exports[i].name   = NAME;           \
   exports[i].handle = HANDLE;         \
   exports[i].buffer = BUFFER;         \
   i++

//+------------------------------------------------------------------+
//| Expert initialization                                            |
//+------------------------------------------------------------------+
int OnInit()
{
   // MT5 allows max 64 open file handles per EA => max 64 exports at once
   ArrayResize(exports, 64);
   int i=0;

   if (Part2Export == 1){

      ADD_EXPORT("ADX_14_Main.csv",   iCustom(_Symbol,_Period,"SqADX",14,PRICE_CLOSE), 0);
      ADD_EXPORT("ADX_14_DI+.csv",    iCustom(_Symbol,_Period,"SqADX",14,PRICE_CLOSE), 1);
      ADD_EXPORT("ADX_14_DI-.csv",    iCustom(_Symbol,_Period,"SqADX",14,PRICE_CLOSE), 2);
      ADD_EXPORT("Aroon_14_up.csv",   iCustom(_Symbol,_Period,"SqAroon",14,false,false), 0);
      ADD_EXPORT("Aroon_14_down.csv", iCustom(_Symbol,_Period,"SqAroon",14,false,false), 1);
      ADD_EXPORT("ATR_14.csv",        iCustom(_Symbol,_Period,"SqATR",14), 0);
      ADD_EXPORT("AvgVolume_14.csv",  iCustom(_Symbol,_Period,"SqAvgVolume",14), 0);
      ADD_EXPORT("Awesome_Oscillator.csv", iAO(_Symbol,_Period), 0);
      ADD_EXPORT("BBWidthRatio_20_2_Open.csv",         iCustom(_Symbol,_Period,"SqBBWidthRatio",20,2.0,PRICE_OPEN), 0);                    
     
      ADD_EXPORT("BB_20_0_2.0_Upper.csv",              iBands(_Symbol,_Period,20, 0, 2.0,PRICE_CLOSE), 1);
      ADD_EXPORT("BB_20_0_2.0_Lower.csv",              iBands(_Symbol,_Period,20, 0, 2.0,PRICE_CLOSE), 2);
      ADD_EXPORT("BearsPower_Close_14.csv", iBearsPower(_Symbol,_Period,14), 0);
      ADD_EXPORT("BullsPower_Close_14.csv", iBullsPower(_Symbol,_Period,14), 0);
      ADD_EXPORT("CCI_Close_14.csv",    iCCI(_Symbol,_Period,14,PRICE_CLOSE), 0);
      ADD_EXPORT("CCI_Typical_14.csv",  iCCI(_Symbol,_Period,14,PRICE_TYPICAL), 0);
      ADD_EXPORT("DeMarker_14.csv",     iDeMarker(_Symbol,_Period,14), 0);
      ADD_EXPORT("FIBO_1_0.csv",        iCustom(_Symbol,_Period,"SqFibo",1,0, 0.0,0,0), 0);
      ADD_EXPORT("Fibo_1_61.8.csv",     iCustom(_Symbol,_Period,"SqFibo",1,0,61.8, 0, 0), 0);
      ADD_EXPORT("Fibo_1_72.5.csv",     iCustom(_Symbol,_Period,"SqFibo",1,0,72.5, 0, 0), 0);
      ADD_EXPORT("Fibo_2_61.8.csv",     iCustom(_Symbol,_Period,"SqFibo",2,0,61.8, 0, 0), 0);
      ADD_EXPORT("Fibo_3_61.8.csv",     iCustom(_Symbol,_Period,"SqFibo",3,0,61.8, 0, 0), 0);
      ADD_EXPORT("Fibo_5_-161.8.csv",   iCustom(_Symbol,_Period,"SqFibo",5,0,-161.8, 0, 0), 0);
      ADD_EXPORT("Fibo_6_-161.8.csv",   iCustom(_Symbol,_Period,"SqFibo",6,0,-161.8, 0, 0), 0);
      ADD_EXPORT("Fibo_7_-161.8.csv",   iCustom(_Symbol,_Period,"SqFibo",7,0,-161.8, 0, 0), 0);
      ADD_EXPORT("HA_Close.csv",        iCustom(_Symbol,_Period,"SqHeikenAshi",0,0,0,0), 3);
      ADD_EXPORT("HA_Open.csv",         iCustom(_Symbol,_Period,"SqHeikenAshi",0,0,0,0), 0);
      ADD_EXPORT("Ichimoku_9_26_52_KijunSen.csv",    iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 1);
      ADD_EXPORT("Ichimoku_9_26_52_SenkouSpanA.csv", iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 2);
      ADD_EXPORT("Ichimoku_9_26_52_SenkouSpanB.csv", iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 3);
      ADD_EXPORT("Ichimoku_9_26_52_TenkanSen.csv",   iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 0);
      ADD_EXPORT("Ichimoku_21_33_28_TenkanSen.csv",  iCustom(_Symbol,_Period,"SqIchimoku",21,33,28), 0);
      ADD_EXPORT("KeltnerChannel_16_0.2_Upper.csv", iCustom(_Symbol,_Period,"SqKeltnerChannel",16,0.2), 0);
      ADD_EXPORT("KeltnerChannel_16_0.2_Lower.csv", iCustom(_Symbol,_Period,"SqKeltnerChannel",16,0.2), 1);
      ADD_EXPORT("LinearRegression_14_Low.csv", iCustom(_Symbol,_Period,"SqLinReg",14,PRICE_LOW), 0);
      ADD_EXPORT("LWMA_14_Close.csv",          iMA(_Symbol,_Period,14,0,MODE_LWMA,PRICE_CLOSE), 0);
      ADD_EXPORT("MACD_Close_12_26_9_Main.csv",   iMACD(_Symbol,_Period,12,26,9,PRICE_CLOSE), 0);
      ADD_EXPORT("MACD_Close_12_26_9_Signal.csv", iMACD(_Symbol,_Period,12,26,9,PRICE_CLOSE), 1);
      ADD_EXPORT("Momentum_Close_60.csv",      iMomentum(_Symbol,_Period,60,PRICE_CLOSE), 0);
      ADD_EXPORT("MTATR_14.csv",               iATR(_Symbol,_Period,14), 0);
      ADD_EXPORT("MTKeltnerChannel_16_0.2_Upper.csv", iCustom(_Symbol,_Period,"SqMTKeltnerChannel",16,0.2), 0);
      ADD_EXPORT("MTKeltnerChannel_16_0.2_Lower.csv", iCustom(_Symbol,_Period,"SqMTKeltnerChannel",16,0.2), 1);
      ADD_EXPORT("OSMA_Close_12_26_9.csv",     iOsMA(_Symbol,_Period,12,26,9,PRICE_CLOSE), 0);
      ADD_EXPORT("ParabolicSAR_0.02_0.2.csv",  iCustom(_Symbol,_Period,"SqParabolicSAR",0.02,0.2), 0);
      ADD_EXPORT("Pivots_0_0_0_P.csv",  iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 0);
      ADD_EXPORT("Pivots_0_0_0_R1.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 1);
      ADD_EXPORT("Pivots_0_0_0_R2.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 2);
      ADD_EXPORT("Pivots_0_0_0_R3.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 3);
      ADD_EXPORT("Pivots_0_0_0_S1.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 4);
      ADD_EXPORT("Pivots_0_0_0_S2.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 5);
      ADD_EXPORT("Pivots_0_0_0_S3.csv", iCustom(_Symbol,_Period,"SqPivots",0,0,0,0,0,0,0,0), 6);
      ADD_EXPORT("Pivots_8_20_0_P.csv", iCustom(_Symbol,_Period,"SqPivots",8,20,0,0,0,0,0,0), 0);
      ADD_EXPORT("QQE_14_5_7_Value1.csv", iCustom(_Symbol,_Period,"SqQQE",14,5,7), 0);
      ADD_EXPORT("QQE_14_5_7_Value2.csv", iCustom(_Symbol,_Period,"SqQQE",14,5,7), 1);
      ADD_EXPORT("RSI_Close_14.csv",     iRSI(_Symbol,_Period,14,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_Close_60.csv",     iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMMA_14_Close.csv",    iMA(_Symbol,_Period,14,0,MODE_SMMA,PRICE_CLOSE), 0);
      ADD_EXPORT("StdDev_Close_20.csv",  iStdDev(_Symbol,_Period,20,0,MODE_SMA,PRICE_CLOSE), 0);
      
      ADD_EXPORT("STOCH_20_10_45_EMA_HL_FastK.csv",     iCustom(_Symbol,_Period, "SqStochastic", 20, 10, 45, MODE_EMA, STO_LOWHIGH), 0);
      ADD_EXPORT("STOCH_20_10_45_EMA_HL_SLOWD.csv",     iCustom(_Symbol,_Period, "SqStochastic", 20, 10, 45, MODE_EMA, STO_LOWHIGH), 1);
   
      ADD_EXPORT("TEMA_Close_14.csv",                   iTEMA(_Symbol,_Period,14,0,PRICE_CLOSE), 0);
      ADD_EXPORT("WilliamsPR_14.csv",                   iWPR(_Symbol,_Period,14), 0);
   }
   //---------------------------------------------------------------------------------------------------------

   if (Part2Export == 2){
      ADD_EXPORT("Fractal_Up_3.csv",                    iCustom(_Symbol,_Period,"SqFractal",3), 0); 
      ADD_EXPORT("Fractal_Down_3.csv",                  iCustom(_Symbol,_Period,"SqFractal",3), 1); 
      
      ADD_EXPORT("GANNHILO_20.csv",                    iCustom(_Symbol,_Period,"SqGannHiLo",20), 0);
      
      ADD_EXPORT("HIGHEST_14.csv",                     iCustom(_Symbol,_Period,"SqHighest",14,PRICE_CLOSE), 0);
      ADD_EXPORT("HIGHESTINDEX_14.csv",                iCustom(_Symbol,_Period,"SqHighestIndex",14,PRICE_CLOSE), 0);
      ADD_EXPORT("LOWEST_14.csv",                      iCustom(_Symbol,_Period,"SqLowest",14,PRICE_CLOSE), 0);
      ADD_EXPORT("LOWESTINDEX_14.csv",                 iCustom(_Symbol,_Period,"SqLowestIndex",14,PRICE_CLOSE), 0);
      
      ADD_EXPORT("CCI_OPEN_14.csv",                    iCCI(_Symbol,_Period,14,PRICE_OPEN), 0);
      ADD_EXPORT("HULLMA_20.csv",                      iCustom(_Symbol,_Period, "SqHullMovingAverage", 20, 2.0, PRICE_CLOSE), 0);   
      
      ADD_EXPORT("ICHIMOKU_9_26_52_TS.csv",            iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 0);
      ADD_EXPORT("ICHIMOKU_9_26_52_KS.csv",            iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 1);
      ADD_EXPORT("ICHIMOKU_9_26_52_SSA.csv",           iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 2);
      ADD_EXPORT("ICHIMOKU_9_26_52_SSB.csv",           iCustom(_Symbol,_Period,"SqIchimoku",9,26,52), 3);
      
      ADD_EXPORT("KAMA_10_2_30.csv",                   iCustom(_Symbol,_Period, "SqKAMA", 10, 2, 30), 0);   
      ADD_EXPORT("KER_30.csv",                         iCustom(_Symbol,_Period,"SqEfficiencyRatio",30), 0); 
      
      ADD_EXPORT("KELTNER_20_1.5_UPPER.csv",           iCustom(_Symbol,_Period,"SqKeltnerChannel",20,1.5), 0);
      ADD_EXPORT("KELTNER_20_1.5_LOWER.csv",           iCustom(_Symbol,_Period,"SqKeltnerChannel",20,1.5), 1);
      
      ADD_EXPORT("LAGUERRERSI_0.5.csv",                iCustom(_Symbol,_Period,"SqLaguerreRSI",0.5), 0);
      
      ADD_EXPORT("LinearRegression_CLOSE_14.csv",      iCustom(_Symbol,_Period,"SqLinReg",14,PRICE_CLOSE), 0);
      ADD_EXPORT("LinearRegression_Close_30.csv",      iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_CLOSE), 0);
      ADD_EXPORT("LinearRegression_High_30.csv",       iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_HIGH), 0);
      ADD_EXPORT("LinearRegression_Low_30.csv",        iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_LOW), 0);
      ADD_EXPORT("LinearRegression_MP_30.csv",         iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_MEDIAN), 0);
      ADD_EXPORT("LinearRegression_Open_30.csv",       iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_OPEN), 0);
      ADD_EXPORT("LinearRegression_TP_30.csv",         iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_TYPICAL), 0);
      ADD_EXPORT("LinearRegression_WC_30.csv",         iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_WEIGHTED), 0);
      
      ADD_EXPORT("MOMENTUM_CLOSE_14.csv",              iMomentum(_Symbol,_Period,14,PRICE_CLOSE), 0);
      ADD_EXPORT("MOMENTUM_HIGH_50.csv",               iMomentum(_Symbol,_Period,50,PRICE_HIGH), 0);
      ADD_EXPORT("MOMENTUM_WeightedC_14.csv",          iMomentum(_Symbol,_Period,14,PRICE_WEIGHTED), 0);
      
      // MovingAverage family (SMA variants)
      ADD_EXPORT("SMA_3_CLOSE.csv",                    iMA(_Symbol,_Period,3,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_10_CLOSE.csv",                   iMA(_Symbol,_Period,10,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_30_CLOSE.csv",                   iMA(_Symbol,_Period,30,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_60_CLOSE.csv",                   iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_60_TypicalPrice.csv",            iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_TYPICAL), 0);
      ADD_EXPORT("SMA_60_WeightedClose.csv",           iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_WEIGHTED), 0);
      ADD_EXPORT("SMA_200_CLOSE.csv",                  iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMA_200_HIGH.csv",                   iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_HIGH), 0);
      ADD_EXPORT("SMA_200_MEDIAN.csv",                 iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_MEDIAN), 0);
      ADD_EXPORT("SMA_200_WeightedClose.csv",          iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_WEIGHTED), 0);
      ADD_EXPORT("SMA_500_CLOSE.csv",                  iMA(_Symbol,_Period,500,0,MODE_SMA,PRICE_CLOSE), 0);
      
      // SMMA extra
      ADD_EXPORT("SMMA_CLOSE_5.csv",                   iMA(_Symbol,_Period,5,0,MODE_SMMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMMA_CLOSE_15.csv",                  iMA(_Symbol,_Period,15,0,MODE_SMMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMMA_CLOSE_50.csv",                  iMA(_Symbol,_Period,50,0,MODE_SMMA,PRICE_CLOSE), 0);
      ADD_EXPORT("SMMA_CLOSE_100.csv",                 iMA(_Symbol,_Period,100,0,MODE_SMMA,PRICE_CLOSE), 0);
      
      // EMA extras
      ADD_EXPORT("EMA_3_CLOSE.csv",                    iMA(_Symbol,_Period,3,0,MODE_EMA,PRICE_CLOSE), 0);
      ADD_EXPORT("EMA_10_CLOSE.csv",                   iMA(_Symbol,_Period,10,0,MODE_EMA,PRICE_CLOSE), 0);
      ADD_EXPORT("EMA_14_MEDIAN.csv",                  iMA(_Symbol,_Period,14,0,MODE_EMA,PRICE_MEDIAN), 0);
      ADD_EXPORT("EMA_30_MedianPrice.csv",             iMA(_Symbol,_Period,30,0,MODE_EMA,PRICE_MEDIAN), 0);
      ADD_EXPORT("EMA_60_CLOSE.csv",                   iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_CLOSE), 0);
      ADD_EXPORT("EMA_60_TypicalPrice.csv",            iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_TYPICAL), 0);
      ADD_EXPORT("EMA_60_WeightedClose.csv",           iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_WEIGHTED), 0);
      ADD_EXPORT("EMA_200_CLOSE.csv",                  iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_CLOSE), 0);
      ADD_EXPORT("EMA_200_MedianPrice.csv",            iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_MEDIAN), 0);
      ADD_EXPORT("EMA_200_WeightedClose.csv",          iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_WEIGHTED), 0);
      
      // LWMA extras
      ADD_EXPORT("LWMA_New_CLOSE_50.csv",              iMA(_Symbol,_Period,50,0,MODE_LWMA,PRICE_CLOSE), 0);
      ADD_EXPORT("LWMA_New_CLOSE_100.csv",             iMA(_Symbol,_Period,100,0,MODE_LWMA,PRICE_CLOSE), 0);
      ADD_EXPORT("LWMA_New_CLOSE_200.csv",             iMA(_Symbol,_Period,200,0,MODE_LWMA,PRICE_CLOSE), 0);
      ADD_EXPORT("LWMA_New_CLOSE_500.csv",             iMA(_Symbol,_Period,500,0,MODE_LWMA,PRICE_CLOSE), 0);
      
      // OSMA variants by applied price
      ADD_EXPORT("OSMA_MP_12_26_9.csv",                iOsMA(_Symbol,_Period,12,26,9,PRICE_MEDIAN), 0);
      ADD_EXPORT("OSMA_WC_12_26_9.csv",                iOsMA(_Symbol,_Period,12,26,9,PRICE_WEIGHTED), 0);
   }
   //---------------------------------------------------------------------------------------------------------
   
   if (Part2Export == 3){
      ADD_EXPORT("QQE_14_5_4.236_Value2.csv",          iCustom(_Symbol,_Period,"SqQQE",14,5,4.236), 1);
      
      ADD_EXPORT("ROC_24.csv",                         iCustom(_Symbol,_Period,"SqROC",24), 0);
      ADD_EXPORT("Reflex_24.csv",                      iCustom(_Symbol,_Period,"SqReflex",24), 0);
      ADD_EXPORT("SchaffTrendCycle_10_20_50.csv",      iCustom(_Symbol,_Period,"SqSchaffTrendCycle",10,20,50), 0);
      
      ADD_EXPORT("SRPercRank_BAS_120_12.csv",          iCustom(_Symbol,_Period,"SqSRPercentRank",1,120,12), 0);                                  
      ADD_EXPORT("SRPercRank_ATR_120_12.csv",          iCustom(_Symbol,_Period,"SqSRPercentRank",2,120,12), 0);
      
      ADD_EXPORT("STDDEV_Close_14.csv",                iStdDev(_Symbol,_Period,14,0,MODE_SMA,PRICE_CLOSE), 0);
      
      // Stochastic variants
      ADD_EXPORT("STOCHASTIC_9_3_3_0_0_FASTK.csv",     iCustom(_Symbol,_Period, "SqStochastic", 9, 3, 3, MODE_SMA, STO_LOWHIGH), 0);
      ADD_EXPORT("STOCHASTIC_9_3_3_1_1_FASTK.csv",     iCustom(_Symbol,_Period, "SqStochastic", 9, 3, 3, MODE_EMA, STO_CLOSECLOSE), 0);
      ADD_EXPORT("STOCHASTIC_9_13_3_3_0_SLOWD.csv",    iCustom(_Symbol,_Period, "SqStochastic", 9, 13, 3, MODE_LWMA, STO_LOWHIGH), 1);
      ADD_EXPORT("STOCHASTIC_19_21_10_2_0_SLOWD.csv",  iCustom(_Symbol,_Period, "SqStochastic", 19, 21, 10, MODE_SMMA, STO_LOWHIGH), 1);
   
      ADD_EXPORT("SuperTrend_BAS_24_3.csv",            iCustom(_Symbol,_Period,"SqSuperTrend",1,24,3.0), 0);                         
      ADD_EXPORT("TrueRange.csv",                      iCustom(_Symbol,_Period,"SqTrueRange"), 0);
      
   
      ADD_EXPORT("UlcerIndex_DownUI_24.csv",           iCustom(_Symbol,_Period,"SqUlcerIndex",2,24), 0);                                             // TODO
      ADD_EXPORT("UlcerIndex_UPUI_24.csv",             iCustom(_Symbol,_Period,"SqUlcerIndex",1,24), 0);
      
      ADD_EXPORT("Vortex_VIPlus_12.csv",               iCustom(_Symbol,_Period,"SqVortex",12), 0);
      ADD_EXPORT("Vortex_VIMinus_12.csv",              iCustom(_Symbol,_Period,"SqVortex",12), 1);
      
      ADD_EXPORT("VWAP_10.csv",                        iCustom(_Symbol,_Period,"SqVWAP",10), 0);
      ADD_EXPORT("WilliamsPR_14.csv",                  iWPR(_Symbol,_Period,14), 0);
      
      ADD_EXPORT("WaveTrendMain.csv",                  iCustom(_Symbol,_Period,"SqWaveTrend",10, 21), 0);
      ADD_EXPORT("WaveTrendSignal.csv",                iCustom(_Symbol,_Period,"SqWaveTrend",10, 21), 1);
   }
   // --- open files (truncate) ---
   for(int k=0;k<i;k++)
   {
      string fname = exports[k].name;
      FileDelete(fname); // ensure clean start
      exports[k].fh = FileOpen(fname, FILE_WRITE|FILE_TXT|FILE_ANSI, ";");
      if(exports[k].fh==INVALID_HANDLE){
         Print("Failed to open file: ",fname," err=",GetLastError());
         return INIT_FAILED;
      }
   }

   return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization                                          |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   for(int k=0;k<ArraySize(exports);k++)
   {
      if(exports[k].handle!=INVALID_HANDLE) IndicatorRelease(exports[k].handle);
      if(exports[k].fh!=INVALID_HANDLE) FileClose(exports[k].fh);
   }
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
{
   // get last CLOSED bar (shift=1)
   MqlRates rates[];
   if(CopyRates(_Symbol,_Period,1,1,rates)!=1) return;

   if(rates[0].time==lastBar) return; // already written
   lastBar = rates[0].time;

   string ts = TimeToString(rates[0].time,TIME_DATE|TIME_MINUTES|TIME_SECONDS);
   
   double o = FixPrice(_Symbol, rates[0].open); 
   double h = FixPrice(_Symbol, rates[0].high);
   double l = FixPrice(_Symbol, rates[0].low);
   double c = FixPrice(_Symbol, rates[0].close);
   long   v = rates[0].tick_volume;

   // loop through exports
   for(int k=0;k<ArraySize(exports);k++)
   {
      if(exports[k].handle==INVALID_HANDLE || exports[k].fh==INVALID_HANDLE) continue;

      double val[];
      if(CopyBuffer(exports[k].handle,exports[k].buffer,1,1,val)==1)
      {
         FileWrite(exports[k].fh,
                   ts,
                   PriceToStr(_Symbol, o), PriceToStr(_Symbol, h), PriceToStr(_Symbol, l), PriceToStr(_Symbol, c), v,
                   val[0]);          
         FileFlush(exports[k].fh);
      }
   }
}
double roundValue(double value){
    return NormalizeDouble(value + 0.0000000001, 6);
}
double FixPrice(const string sym, double price)
{
   double tick = SymbolInfoDouble(sym, SYMBOL_TRADE_TICK_SIZE);
   if(tick <= 0.0) tick = SymbolInfoDouble(sym, SYMBOL_POINT);
   return MathRound(price / tick) * tick;
}
string PriceToStr(const string sym, double price)
{
   int digits = (int)SymbolInfoInteger(sym, SYMBOL_DIGITS);
   return DoubleToString(price, digits);
}

// or if you want hard rounding (not only formatting)
double RoundToDigits(const string sym, double price)
{
   int digits = (int)SymbolInfoInteger(sym, SYMBOL_DIGITS);
   return NormalizeDouble(price, digits);
}
//output to C:\Users\yyyyyyyy\AppData\Roaming\MetaQuotes\Tester\xxxxxxx\Agent-127.0.0.1-3000\MQL5\Files