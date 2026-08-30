//+-----------------------------------------------------------------------------------------+
//|                                 IndicatorExportEA.mq4                                   |
//|                                                                                         |
//|                    EA to export indicator values from MetaTrader                        |
//|              /MetaTrader directory/tester/files/******.csv                              |
//|  ...\Users\..userlogin..\AppData\Roaming\MetaQuotes\Terminal\Common\Files\******.csv    | //C:\Users\PC0022\AppData\Roaming\MetaQuotes\Terminal\Common\Files
//+-----------------------------------------------------------------------------------------+

#property copyright "Copyright © 2025 StrategyQuant"
#property link      "http://www.StrategyQuant.com"

string currentTime = "";
string lastTime = "";

bool start = true;
#property strict

struct ExportItem { string name; int fh; int buffer; };
ExportItem exports[];
datetime lastBar=0;

int OpenCsv(string fn){
     string path = fn;
   FileDelete(path);
   int h = FileOpen(path, FILE_WRITE|FILE_TXT|FILE_ANSI|FILE_COMMON, ";");
   if(h==INVALID_HANDLE) Print("Open fail: ", path, " err=", GetLastError());
   return h;
}

int OnInit(){
   ArrayResize(exports, 64);
   int i=0;
   /*exports[i].name   = "ADX_14_Main.csv"; exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 0; i++;
   exports[i].name   = "ADX_14_DI-.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 1; i++;
   exports[i].name   = "ADX_14_DI+.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 2; i++;
   exports[i].name   = "ATR_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 3; i++;
   exports[i].name   = "Aroon_14_up.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 4; i++;
   exports[i].name   = "Aroon_14_down.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 5; i++;
   exports[i].name   = "AvgVolume_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 6; i++;
   exports[i].name   = "Awesome_Oscillator.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 7; i++;
      
   exports[i].name   = "BearsPower_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 8; i++;
   exports[i].name   = "BullsPower_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 9; i++;
   exports[i].name   = "BBWidthRatio_20_0_2_Open.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 10; i++;
   exports[i].name   = "BB_20_0_2_0_Upper.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 11; i++;
   exports[i].name   = "BB_20_0_2_0_Lower.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 12; i++;
   exports[i].name   = "BBRange_16_2_2_Low.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 13; i++;
   exports[i].name   = "DeMarker_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 14; i++;
   exports[i].name   = "Fibo_1_0.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 15; i++;  
   exports[i].name   = "Fibo_1_61.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 16; i++; 
   exports[i].name   = "Fibo_1_72.5.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 17; i++; 
   exports[i].name   = "Fibo_2_61.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 18; i++; 
   exports[i].name   = "Fibo_3_61.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 19; i++; 
   exports[i].name   = "Fibo_5_-161.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 20; i++; 
   exports[i].name   = "Fibo_6_-161.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 21; i++; 
   exports[i].name   = "Fibo_7_-161.8.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 22; i++; 
   
   exports[i].name   = "FRACTAL_Up_3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 23; i++; 
   exports[i].name   = "FRACTAL_Down_3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 24; i++; 
   exports[i].name   = "GANNHILO_20.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 25; i++; 
   exports[i].name   = "HA_Open.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 26; i++; 
   exports[i].name   = "HA_Close.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 27; i++; 
   
   exports[i].name   = "HIGHEST_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 28; i++; 
   exports[i].name   = "HIGHESTINDEX_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 29; i++; 
   exports[i].name   = "LOWEST_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 30; i++; 
   exports[i].name   = "LOWESTINDEX_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 31; i++; 
   
   exports[i].name   = "CCI_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 32; i++; 
   exports[i].name   = "CCI_Typical_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 33; i++; 
   exports[i].name   = "CCI_OPEN_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 34; i++; 
   
   exports[i].name   = "HullMA_20.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 35; i++; 
   exports[i].name   = "Ichimoku_9_26_52_KijunSen.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 36; i++; 
   exports[i].name   = "Ichimoku_9_26_52_SenkouSpanA.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 37; i++; 
   exports[i].name   = "Ichimoku_9_26_52_SenkouSpanB.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 38; i++; 
   exports[i].name   = "Ichimoku_9_26_52_TenkanSen.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 39; i++; 
   exports[i].name   = "Ichimoku_21_33_28_TenkanSen.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 40; i++; 
   
   exports[i].name   = "KeltnerChannel_16_0.2_Upper.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 41; i++; 
   exports[i].name   = "KeltnerChannel_16_0.2_Lower.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 42; i++; 
   
   exports[i].name   = "LinearRegression_14_Low.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 43; i++; 
   exports[i].name   = "LWMA_14_Close.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 44; i++; 
   
   exports[i].name   = "MACD_Close_12_26_9_Main.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 45; i++; 
   exports[i].name   = "MACD_Close_12_26_9_Signal.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 46; i++; 
   exports[i].name   = "Momentum_Close_60.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 47; i++; 
   exports[i].name   = "MTATR_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 48; i++; 

   exports[i].name   = "OSMA_Close_12_26_9.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 49; i++; 
   exports[i].name   = "ParabolicSAR_0.02_0.2.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 50; i++; 
   
   exports[i].name   = "Pivots_0_0_0_P.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 51; i++; 
   exports[i].name   = "Pivots_0_0_0_R1.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 52; i++; 
   exports[i].name   = "Pivots_0_0_0_R2.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 53; i++; 
   exports[i].name   = "Pivots_0_0_0_R3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 54; i++; 
   exports[i].name   = "Pivots_0_0_0_S1.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 55; i++; 
   exports[i].name   = "Pivots_0_0_0_S2.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 56; i++; 
   exports[i].name   = "Pivots_0_0_0_S3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 57; i++; 
   exports[i].name   = "Pivots_8_20_0_P.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 58; i++; 
   
   exports[i].name   = "QQE_14_5_7_Value1.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 59; i++; 
   exports[i].name   = "QQE_14_5_7_Value2.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 60; i++; 
   exports[i].name   = "RSI_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 61; i++; 

   exports[i].name   = "SMA_Close_60.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 62; i++;
   exports[i].name   = "SMMA_14_Close.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 63; i++;*/
   
   //-------------------------------------------------------------------------------------------------------------------
   /*exports[i].name   = "StdDev_Close_20.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 0; i++;
   
   exports[i].name   = "STOCH_20_10_45_EMA_HL_FastK.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 1; i++;
   exports[i].name   = "STOCH_20_10_45_EMA_HL_SLOWD.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 2; i++;

   exports[i].name   = "TEMA_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 3; i++;
   exports[i].name   = "WilliamsPR_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 4; i++;
         
   exports[i].name   = "Fractal_Up_3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 5; i++;
   exports[i].name   = "Fractal_Down_3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 6; i++;
   exports[i].name   = "GannHiLo_20.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 7; i++;    
   
   exports[i].name   = "KAMA_10_2_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 8; i++; 
   exports[i].name   = "KER_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 9; i++; 
   exports[i].name   = "KELTNER_20_1.5_UPPER.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 10; i++; 
   exports[i].name   = "KELTNER_20_1.5_LOWER.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 11; i++; 
   exports[i].name   = "LaguerreRSI_0.5.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 12; i++; 

   exports[i].name   = "LinearRegression_CLOSE_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 13; i++; 
   exports[i].name   = "LinearRegression_Close_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 14; i++; 
   exports[i].name   = "LinearRegression_High_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 15; i++; 
   exports[i].name   = "LinearRegression_Low_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 16; i++; 
   
   exports[i].name   = "LinearRegression_MP_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 17; i++; 
   exports[i].name   = "LinearRegression_Open_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 18; i++; 
   exports[i].name   = "LinearRegression_TP_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 19; i++; 
   exports[i].name   = "LinearRegression_WC_30.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 20; i++; 

   exports[i].name   = "SMA_3_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 21; i++; 
   exports[i].name   = "SMA_10_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 22; i++; 
   exports[i].name   = "SMA_30_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 23; i++; 
   exports[i].name   = "SMA_60_TypicalPrice.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 24; i++; 
   exports[i].name   = "SMA_60_WeightedClose.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 25; i++; 
   
   exports[i].name   = "SMA_200_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 26; i++; 
   exports[i].name   = "SMA_200_HIGH.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 27; i++; 
   exports[i].name   = "SMA_200_MEDIAN.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 28; i++; 
   exports[i].name   = "SMA_200_WeightedClose.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 29; i++; 
   exports[i].name   = "SMA_500_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 30; i++; 

   exports[i].name   = "EMA_3_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 31; i++; 
   exports[i].name   = "EMA_10_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 32; i++; 
   exports[i].name   = "EMA_14_MEDIAN.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 33; i++; 
   exports[i].name   = "EMA_30_MedianPrice.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 34; i++; 
   exports[i].name   = "EMA_Close_60.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 35; i++; 
   
   exports[i].name   = "EMA_60_TypicalPrice.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 36; i++; 
   exports[i].name   = "EMA_60_WeightedClose.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 37; i++; 
   exports[i].name   = "EMA_200_CLOSE.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 38; i++; 
   exports[i].name   = "EMA_200_MedianPrice.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 39; i++; 
   exports[i].name   = "EMA_200_WeightedClose.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 40; i++; 

   exports[i].name   = "MOMENTUM_CLOSE_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 41; i++; 
   exports[i].name   = "MOMENTUM_HIGH_50.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 42; i++; 
   exports[i].name   = "MOMENTUM_WeightedC_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 43; i++; 
   
   exports[i].name   = "SMMA_CLOSE_5.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 44; i++; 
   exports[i].name   = "SMMA_CLOSE_15.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 45; i++; 
   exports[i].name   = "SMMA_CLOSE_50.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 46; i++; 
   exports[i].name   = "SMMA_CLOSE_100.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 47; i++; 
 
   exports[i].name   = "LWMA_New_CLOSE_50.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 48; i++; 
   exports[i].name   = "LWMA_New_CLOSE_100.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 49; i++; 
   exports[i].name   = "LWMA_New_CLOSE_200.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 50; i++; 
   exports[i].name   = "LWMA_New_CLOSE_500.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 51; i++; 
   
   exports[i].name   = "OSMA_MP_12_26_9.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 52; i++; 
   exports[i].name   = "OSMA_WC_12_26_9.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 53; i++; 
   
   exports[i].name   = "QQE_14_5_4.236_Value2.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 54; i++; 
   
   exports[i].name   = "ROC_24.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 55; i++; 
   exports[i].name   = "Reflex_24.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 56; i++; 
   exports[i].name   = "SchaffTrendCycle_10_20_50.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 57; i++; 
   
   exports[i].name   = "SRPercRank_BAS_120_12.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 58; i++; 
   exports[i].name   = "SRPercRank_ATR_120_12.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 59; i++; 
   
   exports[i].name   = "STDDEV_Close_14.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 60; i++; 
   
   exports[i].name   = "STOCHASTIC_9_3_3_0_0_FASTK.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 61; i++; 
   exports[i].name   = "STOCHASTIC_9_3_3_1_1_FASTK.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 62; i++; 
   exports[i].name   = "STOCHASTIC_9_13_3_3_1_SLOWD.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 63; i++; */
   //-------------------------------------------------------------------------------------------------------------------
   exports[i].name   = "STOCHASTIC_19_21_10_2_0_SLOWD.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 0; i++; 
   
   exports[i].name   = "SuperTrend_BAS_24_3.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 1; i++; 
   exports[i].name   = "TrueRange.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 2; i++; 
   exports[i].name   = "UlcerIndex_DownUI_24.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 3; i++; 
   exports[i].name   = "UlcerIndex_UPUI_24.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 4; i++; 
   
   exports[i].name   = "Vortex_VIPlus_12.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 5; i++;
   exports[i].name   = "Vortex_VIMinus_12.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 6; i++; 
   
   exports[i].name   = "VWAP_10.csv";  exports[i].fh = OpenCsv(exports[i].name); exports[i].buffer = 7; i++; 
   
   
   return(INIT_SUCCEEDED);
}

void OnDeinit(const int reason){
   for(int k=0;k<ArraySize(exports);k++) if(exports[k].fh!=INVALID_HANDLE) FileClose(exports[k].fh);
}

void OnTick(){
   // write once per CLOSED bar
   datetime t1 = iTime(NULL,0,1);
   if(t1==0 || t1==lastBar) return;
   lastBar = t1;

   double o=iOpen(NULL,0,1), h=iHigh(NULL,0,1), l=iLow(NULL,0,1),
          c=iClose(NULL,0,1), v=iVolume(NULL,0,1);
   string ts = TimeToString(t1, TIME_DATE|TIME_MINUTES|TIME_SECONDS);

   double adxMain =                          iCustom(_Symbol, _Period,"SqADX",14,PRICE_CLOSE,0,1);
   double adxDIp  =                          iCustom(_Symbol, _Period,"SqADX",14,PRICE_CLOSE,1,1);
   double adxDI_  =                          iCustom(_Symbol, _Period,"SqADX",14,PRICE_CLOSE,2,1);
   double aroonUp  =                         iCustom(_Symbol, _Period, "SqAroon", 14, false, false, 0, 1);
   double aroonDown  =                       iCustom(_Symbol, _Period, "SqAroon", 14, false, false, 1, 1);
   double atr  =                             iCustom(_Symbol, _Period, "SqATR", 14, 0, 1);
   double AvgVolume  =                       iCustom(_Symbol, _Period, "SqAvgVolume", 14, 0, 1);
   double AwesomeOscillator  =               iAO(_Symbol, _Period, 1);
   
   double BearsPower  =                      iBearsPower(_Symbol, _Period, 14, PRICE_CLOSE, 1);
   double BullsPower  =                      iBullsPower(_Symbol, _Period, 14, PRICE_CLOSE, 1);
   double BBWidthRatio  =                    iCustom(_Symbol, _Period, "SqBBWidthRatio", 20, 2.0, PRICE_OPEN, 0, 1);
   double BB_20_0_2_Upper  =                 iBands(_Symbol, _Period, 20, 2.0, 0, PRICE_CLOSE, MODE_UPPER, 1);
   double BB_20_0_2_Lower  =                 iBands(_Symbol, _Period, 20, 2.0, 0, PRICE_CLOSE, MODE_LOWER, 1);
   double BBRange_16_2_2_Low  =              iBands(_Symbol, _Period, 16, 2.2, 0, PRICE_LOW, MODE_UPPER, 1) - iBands(_Symbol,_Period, 16, 2.2, 0, PRICE_LOW, MODE_LOWER, 1);
   double DeMarker_14  =                     iDeMarker(_Symbol, _Period, 14, 1);
   double Fibo_1_0  =                        iCustom(_Symbol, _Period, "SqFibo", 1, 0, 0, 0, 1);
   double Fibo_1_61_8_HighLowDay  =          iCustom(_Symbol, _Period, "SqFibo", 1, 61.8, 0, 0, 1);
   double Fibo_1_72_5_HighLowDay  =          iCustom(_Symbol, _Period, "SqFibo", 1, 72.5, 0, 0, 1);
   double Fibo_2_61_8_HighLowWeek  =         iCustom(_Symbol, _Period, "SqFibo", 2, 61.8, 0, 0, 1);
   double Fibo_2_61_8__HighLowMonth =        iCustom(_Symbol, _Period, "SqFibo", 3, 61.8, 0, 0, 1);
   double Fibo_5_m161_8_OpenCloseDay   =     iCustom(_Symbol,_Period, "SqFibo", 5, -161.8, 0, 0, 1);
   double Fibo_5_m161_8_OpenCloseWeek  =     iCustom(_Symbol,_Period, "SqFibo", 6, -161.8, 0, 0, 1);
   double Fibo_5_m161_8_OpenCloseMonth  =    iCustom(_Symbol,_Period, "SqFibo", 7, -161.8, 0, 0, 1); 
   
   double FRACTAL_Up_3  =                    iCustom(_Symbol,_Period, "SqFractal", 3, 0, 1);  
   double FRACTAL_Down_3  =                  iCustom(_Symbol,_Period, "SqFractal", 3, 0, 1); 
   double GANNHILO_20  =                     iCustom(_Symbol,_Period,"SqGannHiLo",20,0,1);   
   double HA_Open  =                         iCustom(_Symbol,_Period, "SqHeikenAshi", 0,0,0,0, 2, 1);  
   double HA_Close  =                        iCustom(_Symbol,_Period, "SqHeikenAshi", 0,0,0,0, 3, 1); 
   
   double HIGHEST_14  =                      iCustom(_Symbol,_Period, "SqHighest", 14, PRICE_CLOSE, 0, 1); 
   double HIGHESTINDEX_14  =                 iCustom(_Symbol,_Period, "SqHighestIndex", 14, PRICE_CLOSE, 0, 1); 
   double LOWEST_14  =                       iCustom(_Symbol,_Period, "SqLowest", 14, PRICE_CLOSE, 0, 1);
   double LOWESTINDEX_14  =                  iCustom(_Symbol,_Period, "SqLowestIndex", 14, PRICE_CLOSE, 0, 1); 
   
   double CCI_Close_14  =                    iCCI(_Symbol,_Period, 14, PRICE_CLOSE, 1); 
   double CCI_Typical_14  =                  iCCI(_Symbol,_Period, 14, PRICE_TYPICAL, 1); 
   double CCI_OPEN_14  =                     iCCI(_Symbol,_Period, 14, PRICE_OPEN, 1); 
           
   double HULLMA_20  =                       iCustom(_Symbol,_Period, "SqHullMovingAverage", 20, PRICE_CLOSE, 0, 1); 
   
   double Ichimoku_9_26_52_KijunSen  =       iCustom(_Symbol,_Period, "SqIchimoku", 9, 26, 52, 1, 1); 
   double Ichimoku_9_26_52_SenkouSpanA  =    iCustom(_Symbol,_Period, "SqIchimoku", 9, 26, 52, 2, 1); 
   double Ichimoku_9_26_52_SenkouSpanB  =    iCustom(_Symbol,_Period, "SqIchimoku", 9, 26, 52, 3, 1); 
   double Ichimoku_9_26_52_TenkanSen  =      iCustom(_Symbol,_Period, "SqIchimoku", 9, 26, 52, 0, 1); 
   double Ichimoku_21_33_28_TenkanSen  =     iCustom(_Symbol,_Period, "SqIchimoku", 21, 33, 28, 0, 1);  
   
   double KeltnerChannel_16_0_2_Upper  =     iCustom(_Symbol,_Period, "SqKeltnerChannel", 16, 0.2, 0, 1);            //Upper 
   double KeltnerChannel_16_0_2_Lower  =     iCustom(_Symbol,_Period, "SqKeltnerChannel", 16, 0.2, 1, 1);            //Lower
   
   double LinearRegression_14_Low  =         iCustom(_Symbol,_Period, "SqLinReg", 14, PRICE_LOW, 0, 1); 
   double LWMA_14_Close  =                   iMA(_Symbol,_Period, 14, 0, MODE_LWMA, PRICE_CLOSE, 1); 
   
   double MACD_Close_12_26_9_Main  =         iMACD(_Symbol,_Period, 12, 26, 9, PRICE_CLOSE, MODE_MAIN, 1); 
   double MACD_Close_12_26_9_Signal  =       iMACD(_Symbol,_Period, 12, 26, 9, PRICE_CLOSE, MODE_SIGNAL, 1); 
   double Momentum_Close_60  =               iMomentum(_Symbol,_Period, 60, PRICE_CLOSE, 1);
   double MTATR_14  =                        iATR(_Symbol,_Period, 14, 1); 

   double OSMA_Close_12_26_9  =              iOsMA(_Symbol,_Period, 12, 26, 9, PRICE_CLOSE, 1);
   double ParabolicSAR_0_02_0_2  =           iCustom(_Symbol,_Period, "SqParabolicSAR", 0.02, 0.2, 0, 1); 
   
   double Pivots_0_0_0_P  =                  iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);    //P
   double Pivots_0_0_0_R1  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 1, 1);    //R1
   double Pivots_0_0_0_R2  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 2, 1);    //R2
   double Pivots_0_0_0_R3  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 3, 1);    //R3
   
   double Pivots_0_0_0_S1  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 4, 1);    //S1
   double Pivots_0_0_0_S2  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 5, 1);    //S2 
   double Pivots_0_0_0_S3  =                 iCustom(_Symbol,_Period, "SqPivots", 0, 0, 0, 0, 0, 0, 0, 0, 6, 1);    //S3
   double Pivots_8_20_0_P  =                 iCustom(_Symbol,_Period, "SqPivots", 8, 20, 0, 0, 0, 0, 0, 0, 0, 1);    //P
   
   double QQE_14_5_7_Value1  =               iCustom(_Symbol,_Period, "SqQQE", 14, 5, 7, 0, 1);           //Value1
   double QQE_14_5_7_Value2  =               iCustom(_Symbol,_Period, "SqQQE", 14, 5, 7, 1, 1);           //Value2
   double RSI_Close_14  =                    iRSI(_Symbol,_Period, 14, PRICE_CLOSE, 1);
   
   double SMA_Close_60  =                    iMA(_Symbol,_Period, 60, 0, MODE_SMA, PRICE_CLOSE, 1);
   double SMMA_14_Close  =                   iMA(_Symbol,_Period, 14, 0, MODE_SMMA, PRICE_CLOSE, 1); 
   double StdDev_Close_20  =                 iStdDev(_Symbol,_Period, 20, 0, MODE_SMA, PRICE_CLOSE, 1);
   double STOCH_20_10_45_EMA_HL_FastK  =     iCustom(_Symbol,_Period, "SqStochastic", 20, 10, 45, MODE_EMA, 0, MODE_MAIN, 1);
   double STOCH_20_10_45_EMA_HL_SLOWD  =     iCustom(_Symbol,_Period, "SqStochastic", 20, 10, 45, MODE_EMA, 0, MODE_SIGNAL, 1);
   double TEMA_Close_14  =                   iCustom(_Symbol,_Period, "SqTEMA", 14, PRICE_CLOSE, 0, 1);
   double WilliamsPR_14  =                   iWPR(_Symbol,_Period, 14, 1);
 
   double Fractal_Up_3  =                    iCustom(_Symbol,_Period,"SqFractal",3,0,1);
   double Fractal_Down_3  =                  iCustom(_Symbol,_Period,"SqFractal",3,1,1);
   double GannHiLo_20  =                     iCustom(_Symbol,_Period,"SqGannHiLo",20,0,1);
 
   double KAMA_10_2_30  =                    iCustom(_Symbol,_Period, "SqKAMA", 10, 2, 30, 0, 1);
   double KER_30  =                          iCustom(_Symbol,_Period, "SqEfficiencyRatio",30, 0, 1);
   double KELTNER_20_1_5_UPPER  =            iCustom(_Symbol,_Period, "SqKeltnerChannel", 20, 1.5, 0, 1);
   double KELTNER_20_1_5_LOWER  =            iCustom(_Symbol,_Period, "SqKeltnerChannel", 20, 1.5, 1, 1);
   double LaguerreRSI_0_5  =                 iCustom(_Symbol,_Period, "SqLaguerreRSI",0.5, 0, 1);    
  
   double LinearRegression_CLOSE_14  =       iCustom(_Symbol,_Period,"SqLinReg",14,PRICE_CLOSE, 0, 1); 
   double LinearRegression_Close_30  =       iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_CLOSE, 0, 1); 
   double LinearRegression_High_30  =        iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_HIGH, 0, 1); 
   double LinearRegression_Low_30  =         iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_LOW, 0, 1); 
   
   double LinearRegression_MP_30  =          iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_MEDIAN, 0, 1); 
   double LinearRegression_Open_30  =        iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_OPEN, 0, 1); 
   double LinearRegression_TP_30  =          iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_TYPICAL, 0, 1); 
   double LinearRegression_WC_30  =          iCustom(_Symbol,_Period,"SqLinReg",30,PRICE_WEIGHTED, 0, 1); 
  
   double SMA_3_CLOSE  =                     iMA(_Symbol,_Period,3,0,MODE_SMA,PRICE_CLOSE, 1); 
   double SMA_10_CLOSE  =                    iMA(_Symbol,_Period,10,0,MODE_SMA,PRICE_CLOSE, 1); 
   double SMA_30_CLOSE  =                    iMA(_Symbol,_Period,30,0,MODE_SMA,PRICE_CLOSE, 1); 
   double SMA_60_TypicalPrice  =             iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_TYPICAL, 1); 
   double SMA_60_WeightedClose  =            iMA(_Symbol,_Period,60,0,MODE_SMA,PRICE_WEIGHTED, 1); 
   
   double SMA_200_CLOSE  =                   iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_CLOSE, 1); 
   double SMA_200_HIGH  =                    iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_HIGH, 1); 
   double SMA_200_MEDIAN  =                  iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_MEDIAN, 1); 
   double SMA_200_WeightedClose  =           iMA(_Symbol,_Period,200,0,MODE_SMA,PRICE_WEIGHTED, 1); 
   double SMA_500_CLOSE  =                   iMA(_Symbol,_Period,500,0,MODE_SMA,PRICE_CLOSE, 1); 

   double EMA_3_CLOSE  =                     iMA(_Symbol,_Period,3,0,MODE_EMA,PRICE_CLOSE, 1); 
   double EMA_10_CLOSE  =                    iMA(_Symbol,_Period,10,0,MODE_EMA,PRICE_CLOSE, 1); 
   double EMA_14_MEDIAN  =                   iMA(_Symbol,_Period,14,0,MODE_EMA,PRICE_MEDIAN, 1); 
   double EMA_30_MedianPrice  =              iMA(_Symbol,_Period,30,0,MODE_EMA,PRICE_MEDIAN, 1); 
   double EMA_Close_60  =                    iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_CLOSE, 1); 
   
   double EMA_60_TypicalPrice  =             iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_TYPICAL, 1); 
   double EMA_60_WeightedClose  =            iMA(_Symbol,_Period,60,0,MODE_EMA,PRICE_WEIGHTED, 1); 
   double EMA_200_CLOSE  =                   iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_CLOSE, 1); 
   double EMA_200_MedianPrice  =             iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_MEDIAN, 1); 
   double EMA_200_WeightedClose  =           iMA(_Symbol,_Period,200,0,MODE_EMA,PRICE_WEIGHTED, 1); 
   
   double MOMENTUM_CLOSE_14  =               iMomentum(_Symbol,_Period,14,PRICE_CLOSE, 1); 
   double MOMENTUM_HIGH_50  =                iMomentum(_Symbol,_Period,50,PRICE_HIGH, 1); 
   double MOMENTUM_WeightedC_14  =           iMomentum(_Symbol,_Period,14,PRICE_WEIGHTED, 1); 
   
   double SMMA_CLOSE_5  =                    iMA(_Symbol,_Period,5,0,MODE_SMMA,PRICE_CLOSE, 1); 
   double SMMA_CLOSE_15  =                   iMA(_Symbol,_Period,15,0,MODE_SMMA,PRICE_CLOSE, 1); 
   double SMMA_CLOSE_50  =                   iMA(_Symbol,_Period,50,0,MODE_SMMA,PRICE_CLOSE, 1); 
   double SMMA_CLOSE_100  =                  iMA(_Symbol,_Period,100,0,MODE_SMMA,PRICE_CLOSE, 1); 

   double LWMA_New_CLOSE_50  =               iMA(_Symbol,_Period,50,0,MODE_LWMA,PRICE_CLOSE, 1);
   double LWMA_New_CLOSE_100  =              iMA(_Symbol,_Period,100,0,MODE_LWMA,PRICE_CLOSE, 1);
   double LWMA_New_CLOSE_200  =              iMA(_Symbol,_Period,200,0,MODE_LWMA,PRICE_CLOSE, 1);
   double LWMA_New_CLOSE_500  =              iMA(_Symbol,_Period,500,0,MODE_LWMA,PRICE_CLOSE, 1);
   
   double OSMA_MP_12_26_9  =                 iOsMA(_Symbol,_Period,12,26,9,PRICE_MEDIAN, 1);
   double OSMA_WC_12_26_9  =                 iOsMA(_Symbol,_Period,12,26,9,PRICE_WEIGHTED, 1);
   
   double QQE_14_5_4_236_Value2  =           iCustom(_Symbol,_Period,"SqQQE",14,5,4.236, 1, 1);
   
   double ROC_24  =                          iCustom(_Symbol,_Period,"SqROC",24, 0, 1);
   double Reflex_24  =                       iCustom(_Symbol,_Period,"SqReflex",24,24, 0, 1);
   double SchaffTrendCycle_10_20_50  =       iCustom(_Symbol,_Period,"SqSchaffTrendCycle",10,20,50,24, 0, 1);
   
   double SRPercRank_BAS_120_12  =           iCustom(_Symbol,_Period,"SqSRPercentRank",1,120,12,24, 0, 1);
   double SRPercRank_ATR_120_12  =           iCustom(_Symbol,_Period,"SqSRPercentRank",2,120,12,24, 0, 1);
   
   double STDDEV_Close_14  =                 iStdDev(_Symbol,_Period,14,0,MODE_SMA,PRICE_CLOSE, 1); 
   
   double STOCHASTIC_9_3_3_0_0_FASTK  =      iCustom(_Symbol,_Period, "SqStochastic", 9, 3, 3, MODE_SMA, STO_LOWHIGH, 0, 1); 
   double STOCHASTIC_9_3_3_1_1_FASTK  =      iCustom(_Symbol,_Period, "SqStochastic", 9, 3, 3, MODE_EMA, STO_CLOSECLOSE, 0, 1); 
   double STOCHASTIC_9_13_3_3_1_SLOWD  =     iCustom(_Symbol,_Period, "SqStochastic", 9, 13, 3, MODE_LWMA, STO_CLOSECLOSE, 1, 1); 
   double STOCHASTIC_19_21_10_2_0_SLOWD  =   iCustom(_Symbol,_Period, "SqStochastic", 19, 21, 10, MODE_SMMA, STO_LOWHIGH, 1, 1); 
   
   double SuperTrend_BAS_24_3  =             iCustom(_Symbol,_Period,"SqSuperTrend",1,24,3.0, 0, 1); 
   double TrueRange  =                       iCustom(_Symbol,_Period,"SqTrueRange", 0, 1); 
   double UlcerIndex_DownUI_24  =            iCustom(_Symbol,_Period,"SqUlcerIndex",2,24, 0, 1); 
   double UlcerIndex_UPUI_24  =              iCustom(_Symbol,_Period,"SqUlcerIndex",1,24, 0, 1);   

   double Vortex_VIPlus_12  =                iCustom(_Symbol,_Period,"SqVortex",12, 0, 1);
   double Vortex_VIMinus_12  =               iCustom(_Symbol,_Period,"SqVortex",12, 1, 1);
   double VWAP_10  =                         iCustom(_Symbol,_Period,"SqVWAP",10, 0, 1);




   /*FileWrite(exports[0].fh, ts,o,h,l,c,v,adxMain); FileFlush(exports[0].fh);
   FileWrite(exports[1].fh, ts,o,h,l,c,v,adxDI_ ); FileFlush(exports[1].fh);
   FileWrite(exports[2].fh, ts,o,h,l,c,v,adxDIp ); FileFlush(exports[2].fh);
   FileWrite(exports[3].fh, ts,o,h,l,c,v,atr ); FileFlush(exports[3].fh);
   FileWrite(exports[4].fh, ts,o,h,l,c,v,aroonUp ); FileFlush(exports[4].fh);
   FileWrite(exports[5].fh, ts,o,h,l,c,v,aroonDown ); FileFlush(exports[5].fh);
   FileWrite(exports[6].fh, ts,o,h,l,c,v,AvgVolume ); FileFlush(exports[6].fh);
   FileWrite(exports[7].fh, ts,o,h,l,c,v,AwesomeOscillator ); FileFlush(exports[7].fh);
   
   FileWrite(exports[8].fh, ts,o,h,l,c,v,BearsPower ); FileFlush(exports[8].fh);
   FileWrite(exports[9].fh, ts,o,h,l,c,v,BullsPower ); FileFlush(exports[9].fh);
   FileWrite(exports[10].fh, ts,o,h,l,c,v,BBWidthRatio ); FileFlush(exports[10].fh);
   FileWrite(exports[11].fh, ts,o,h,l,c,v,BB_20_0_2_Upper); FileFlush(exports[11].fh);
   FileWrite(exports[12].fh, ts,o,h,l,c,v,BB_20_0_2_Lower); FileFlush(exports[12].fh);
   FileWrite(exports[13].fh, ts,o,h,l,c,v,BBRange_16_2_2_Low ); FileFlush(exports[13].fh);
   FileWrite(exports[14].fh, ts,o,h,l,c,v,DeMarker_14); FileFlush(exports[14].fh);
   FileWrite(exports[15].fh, ts,o,h,l,c,v,Fibo_1_0); FileFlush(exports[15].fh);
   FileWrite(exports[16].fh, ts,o,h,l,c,v,Fibo_1_61_8_HighLowDay); FileFlush(exports[16].fh);
   FileWrite(exports[17].fh, ts,o,h,l,c,v,Fibo_1_72_5_HighLowDay); FileFlush(exports[17].fh);
   FileWrite(exports[18].fh, ts,o,h,l,c,v,Fibo_2_61_8_HighLowWeek); FileFlush(exports[17].fh);
   FileWrite(exports[19].fh, ts,o,h,l,c,v,Fibo_2_61_8__HighLowMonth); FileFlush(exports[19].fh);
   FileWrite(exports[20].fh, ts,o,h,l,c,v,Fibo_5_m161_8_OpenCloseDay); FileFlush(exports[20].fh);
   FileWrite(exports[21].fh, ts,o,h,l,c,v,Fibo_5_m161_8_OpenCloseWeek); FileFlush(exports[21].fh);
   FileWrite(exports[22].fh, ts,o,h,l,c,v,Fibo_5_m161_8_OpenCloseMonth); FileFlush(exports[22].fh);
   
   FileWrite(exports[23].fh, ts,o,h,l,c,v,FRACTAL_Up_3); FileFlush(exports[23].fh);
   FileWrite(exports[24].fh, ts,o,h,l,c,v,FRACTAL_Down_3); FileFlush(exports[24].fh);
   FileWrite(exports[25].fh, ts,o,h,l,c,v,GANNHILO_20); FileFlush(exports[25].fh);
   FileWrite(exports[26].fh, ts,o,h,l,c,v,HA_Open); FileFlush(exports[26].fh);
   FileWrite(exports[27].fh, ts,o,h,l,c,v,HA_Close); FileFlush(exports[27].fh);
   
   FileWrite(exports[28].fh, ts,o,h,l,c,v,HIGHEST_14); FileFlush(exports[28].fh);
   FileWrite(exports[29].fh, ts,o,h,l,c,v,HIGHESTINDEX_14); FileFlush(exports[29].fh);
   FileWrite(exports[30].fh, ts,o,h,l,c,v,LOWEST_14); FileFlush(exports[30].fh);
   FileWrite(exports[31].fh, ts,o,h,l,c,v,LOWESTINDEX_14); FileFlush(exports[31].fh);
   
   FileWrite(exports[32].fh, ts,o,h,l,c,v,CCI_Close_14); FileFlush(exports[32].fh);
   FileWrite(exports[33].fh, ts,o,h,l,c,v,CCI_Typical_14); FileFlush(exports[33].fh);
   FileWrite(exports[34].fh, ts,o,h,l,c,v,CCI_OPEN_14); FileFlush(exports[34].fh);  
   
   
   FileWrite(exports[35].fh, ts,o,h,l,c,v,HULLMA_20); FileFlush(exports[35].fh); 
   FileWrite(exports[36].fh, ts,o,h,l,c,v,Ichimoku_9_26_52_KijunSen); FileFlush(exports[36].fh); 
   FileWrite(exports[37].fh, ts,o,h,l,c,v,Ichimoku_9_26_52_SenkouSpanA); FileFlush(exports[37].fh);  
   FileWrite(exports[38].fh, ts,o,h,l,c,v,Ichimoku_9_26_52_SenkouSpanB); FileFlush(exports[38].fh); 
   FileWrite(exports[39].fh, ts,o,h,l,c,v,Ichimoku_9_26_52_TenkanSen); FileFlush(exports[39].fh); 
   FileWrite(exports[40].fh, ts,o,h,l,c,v,Ichimoku_21_33_28_TenkanSen); FileFlush(exports[40].fh); 
   
   FileWrite(exports[41].fh, ts,o,h,l,c,v,KeltnerChannel_16_0_2_Upper); FileFlush(exports[41].fh); 
   FileWrite(exports[42].fh, ts,o,h,l,c,v,KeltnerChannel_16_0_2_Lower); FileFlush(exports[42].fh); 
   
   FileWrite(exports[43].fh, ts,o,h,l,c,v,LinearRegression_14_Low); FileFlush(exports[43].fh); 
   FileWrite(exports[44].fh, ts,o,h,l,c,v,LWMA_14_Close); FileFlush(exports[44].fh); 
   
   FileWrite(exports[45].fh, ts,o,h,l,c,v,MACD_Close_12_26_9_Main); FileFlush(exports[45].fh); 
   FileWrite(exports[46].fh, ts,o,h,l,c,v,MACD_Close_12_26_9_Signal); FileFlush(exports[46].fh); 
   FileWrite(exports[47].fh, ts,o,h,l,c,v,Momentum_Close_60); FileFlush(exports[47].fh); 
   FileWrite(exports[48].fh, ts,o,h,l,c,v,MTATR_14); FileFlush(exports[48].fh); 

   FileWrite(exports[49].fh, ts,o,h,l,c,v,OSMA_Close_12_26_9); FileFlush(exports[49].fh);
   FileWrite(exports[50].fh, ts,o,h,l,c,v,ParabolicSAR_0_02_0_2); FileFlush(exports[50].fh);
   
   FileWrite(exports[51].fh, ts,o,h,l,c,v,Pivots_0_0_0_P); FileFlush(exports[51].fh);
   FileWrite(exports[52].fh, ts,o,h,l,c,v,Pivots_0_0_0_R1); FileFlush(exports[52].fh);
   FileWrite(exports[53].fh, ts,o,h,l,c,v,Pivots_0_0_0_R2); FileFlush(exports[53].fh);
   FileWrite(exports[54].fh, ts,o,h,l,c,v,Pivots_0_0_0_R3); FileFlush(exports[54].fh);
   
   FileWrite(exports[55].fh, ts,o,h,l,c,v,Pivots_0_0_0_S1); FileFlush(exports[55].fh);
   FileWrite(exports[56].fh, ts,o,h,l,c,v,Pivots_0_0_0_S2); FileFlush(exports[56].fh);
   FileWrite(exports[57].fh, ts,o,h,l,c,v,Pivots_0_0_0_S3); FileFlush(exports[57].fh);
   FileWrite(exports[58].fh, ts,o,h,l,c,v,Pivots_8_20_0_P); FileFlush(exports[58].fh);
   
   FileWrite(exports[59].fh, ts,o,h,l,c,v,QQE_14_5_7_Value1); FileFlush(exports[59].fh);
   FileWrite(exports[60].fh, ts,o,h,l,c,v,QQE_14_5_7_Value2); FileFlush(exports[60].fh);
   FileWrite(exports[61].fh, ts,o,h,l,c,v,RSI_Close_14); FileFlush(exports[61].fh);
   
   FileWrite(exports[62].fh, ts,o,h,l,c,v,SMA_Close_60); FileFlush(exports[62].fh);
   FileWrite(exports[63].fh, ts,o,h,l,c,v,SMMA_14_Close); FileFlush(exports[63].fh);*/
   //----------------------------------------------------------------------------------------
   /*FileWrite(exports[0].fh, ts,o,h,l,c,v,StdDev_Close_20); FileFlush(exports[0].fh);
   
   FileWrite(exports[1].fh, ts,o,h,l,c,v,STOCH_20_10_45_EMA_HL_FastK); FileFlush(exports[1].fh);
   FileWrite(exports[2].fh, ts,o,h,l,c,v,STOCH_20_10_45_EMA_HL_SLOWD); FileFlush(exports[2].fh);
   
   FileWrite(exports[3].fh, ts,o,h,l,c,v,TEMA_Close_14); FileFlush(exports[3].fh);
   FileWrite(exports[4].fh, ts,o,h,l,c,v,WilliamsPR_14); FileFlush(exports[4].fh);
  
   FileWrite(exports[5].fh, ts,o,h,l,c,v,Fractal_Up_3); FileFlush(exports[5].fh);
   FileWrite(exports[6].fh, ts,o,h,l,c,v,Fractal_Down_3); FileFlush(exports[6].fh);
   FileWrite(exports[7].fh, ts,o,h,l,c,v,GannHiLo_20); FileFlush(exports[7].fh);
   
   FileWrite(exports[8].fh, ts,o,h,l,c,v,KAMA_10_2_30); FileFlush(exports[8].fh);
   FileWrite(exports[9].fh, ts,o,h,l,c,v,KER_30); FileFlush(exports[9].fh);
   FileWrite(exports[10].fh, ts,o,h,l,c,v,KELTNER_20_1_5_UPPER); FileFlush(exports[10].fh);
   FileWrite(exports[11].fh, ts,o,h,l,c,v,KELTNER_20_1_5_LOWER); FileFlush(exports[11].fh);
   FileWrite(exports[12].fh, ts,o,h,l,c,v,LaguerreRSI_0_5); FileFlush(exports[12].fh);
  
   FileWrite(exports[13].fh, ts,o,h,l,c,v,LinearRegression_CLOSE_14); FileFlush(exports[13].fh);
   FileWrite(exports[14].fh, ts,o,h,l,c,v,LinearRegression_Close_30); FileFlush(exports[14].fh);
   FileWrite(exports[15].fh, ts,o,h,l,c,v,LinearRegression_High_30); FileFlush(exports[15].fh);
   FileWrite(exports[16].fh, ts,o,h,l,c,v,LinearRegression_Low_30); FileFlush(exports[16].fh);
   
   FileWrite(exports[17].fh, ts,o,h,l,c,v,LinearRegression_MP_30); FileFlush(exports[17].fh);
   FileWrite(exports[18].fh, ts,o,h,l,c,v,LinearRegression_Open_30); FileFlush(exports[18].fh);
   FileWrite(exports[19].fh, ts,o,h,l,c,v,LinearRegression_TP_30); FileFlush(exports[19].fh);
   FileWrite(exports[20].fh, ts,o,h,l,c,v,LinearRegression_WC_30); FileFlush(exports[20].fh);    
      
   FileWrite(exports[21].fh, ts,o,h,l,c,v,SMA_3_CLOSE); FileFlush(exports[21].fh);  
   FileWrite(exports[22].fh, ts,o,h,l,c,v,SMA_10_CLOSE); FileFlush(exports[22].fh);  
   FileWrite(exports[23].fh, ts,o,h,l,c,v,SMA_30_CLOSE); FileFlush(exports[23].fh);  
   FileWrite(exports[24].fh, ts,o,h,l,c,v,SMA_60_TypicalPrice); FileFlush(exports[24].fh);  
   FileWrite(exports[25].fh, ts,o,h,l,c,v,SMA_60_WeightedClose); FileFlush(exports[25].fh);  
   
   FileWrite(exports[26].fh, ts,o,h,l,c,v,SMA_200_CLOSE); FileFlush(exports[26].fh);  
   FileWrite(exports[27].fh, ts,o,h,l,c,v,SMA_200_HIGH); FileFlush(exports[27].fh);  
   FileWrite(exports[28].fh, ts,o,h,l,c,v,SMA_200_MEDIAN); FileFlush(exports[28].fh);  
   FileWrite(exports[29].fh, ts,o,h,l,c,v,SMA_200_WeightedClose); FileFlush(exports[29].fh);  
   FileWrite(exports[30].fh, ts,o,h,l,c,v,SMA_500_CLOSE); FileFlush(exports[30].fh);  

   FileWrite(exports[31].fh, ts,o,h,l,c,v,EMA_3_CLOSE); FileFlush(exports[31].fh); 
   FileWrite(exports[32].fh, ts,o,h,l,c,v,EMA_10_CLOSE); FileFlush(exports[32].fh); 
   FileWrite(exports[33].fh, ts,o,h,l,c,v,EMA_14_MEDIAN); FileFlush(exports[33].fh); 
   FileWrite(exports[34].fh, ts,o,h,l,c,v,EMA_30_MedianPrice); FileFlush(exports[34].fh); 
   FileWrite(exports[35].fh, ts,o,h,l,c,v,EMA_Close_60); FileFlush(exports[35].fh); 
   
   FileWrite(exports[36].fh, ts,o,h,l,c,v,EMA_60_TypicalPrice); FileFlush(exports[36].fh); 
   FileWrite(exports[37].fh, ts,o,h,l,c,v,EMA_60_WeightedClose); FileFlush(exports[37].fh); 
   FileWrite(exports[38].fh, ts,o,h,l,c,v,EMA_200_CLOSE); FileFlush(exports[38].fh); 
   FileWrite(exports[39].fh, ts,o,h,l,c,v,EMA_200_MedianPrice); FileFlush(exports[39].fh); 
   FileWrite(exports[40].fh, ts,o,h,l,c,v,EMA_200_WeightedClose); FileFlush(exports[40].fh); 

   FileWrite(exports[41].fh, ts,o,h,l,c,v,MOMENTUM_CLOSE_14); FileFlush(exports[41].fh); 
   FileWrite(exports[42].fh, ts,o,h,l,c,v,MOMENTUM_HIGH_50); FileFlush(exports[42].fh); 
   FileWrite(exports[43].fh, ts,o,h,l,c,v,MOMENTUM_WeightedC_14); FileFlush(exports[43].fh); 
   
   FileWrite(exports[44].fh, ts,o,h,l,c,v,SMMA_CLOSE_5); FileFlush(exports[44].fh); 
   FileWrite(exports[45].fh, ts,o,h,l,c,v,SMMA_CLOSE_15); FileFlush(exports[45].fh); 
   FileWrite(exports[46].fh, ts,o,h,l,c,v,SMMA_CLOSE_50); FileFlush(exports[46].fh); 
   FileWrite(exports[47].fh, ts,o,h,l,c,v,SMMA_CLOSE_100); FileFlush(exports[47].fh);
   
   FileWrite(exports[48].fh, ts,o,h,l,c,v,LWMA_New_CLOSE_50); FileFlush(exports[48].fh);
   FileWrite(exports[49].fh, ts,o,h,l,c,v,LWMA_New_CLOSE_100); FileFlush(exports[49].fh);
   FileWrite(exports[50].fh, ts,o,h,l,c,v,LWMA_New_CLOSE_200); FileFlush(exports[50].fh);
   FileWrite(exports[51].fh, ts,o,h,l,c,v,LWMA_New_CLOSE_500); FileFlush(exports[51].fh);
   
   FileWrite(exports[52].fh, ts,o,h,l,c,v,OSMA_MP_12_26_9); FileFlush(exports[52].fh);
   FileWrite(exports[53].fh, ts,o,h,l,c,v,OSMA_WC_12_26_9); FileFlush(exports[53].fh);
   
   FileWrite(exports[54].fh, ts,o,h,l,c,v,QQE_14_5_4_236_Value2); FileFlush(exports[54].fh);
   
   FileWrite(exports[55].fh, ts,o,h,l,c,v,ROC_24); FileFlush(exports[55].fh);
   FileWrite(exports[56].fh, ts,o,h,l,c,v,Reflex_24); FileFlush(exports[56].fh);
   FileWrite(exports[57].fh, ts,o,h,l,c,v,SchaffTrendCycle_10_20_50); FileFlush(exports[57].fh);
   
   FileWrite(exports[58].fh, ts,o,h,l,c,v,SRPercRank_BAS_120_12); FileFlush(exports[58].fh);
   FileWrite(exports[59].fh, ts,o,h,l,c,v,SRPercRank_ATR_120_12); FileFlush(exports[59].fh);
   
   FileWrite(exports[60].fh, ts,o,h,l,c,v,STDDEV_Close_14); FileFlush(exports[60].fh); 
   
   FileWrite(exports[61].fh, ts,o,h,l,c,v,STOCHASTIC_9_3_3_0_0_FASTK); FileFlush(exports[61].fh); 
   FileWrite(exports[62].fh, ts,o,h,l,c,v,STOCHASTIC_9_3_3_1_1_FASTK); FileFlush(exports[62].fh); 
   FileWrite(exports[63].fh, ts,o,h,l,c,v,STOCHASTIC_9_13_3_3_1_SLOWD); FileFlush(exports[63].fh); */
   //----------------------------------------------------------------------------------------

   FileWrite(exports[0].fh, ts,o,h,l,c,v,STOCHASTIC_19_21_10_2_0_SLOWD); FileFlush(exports[0].fh); 
   
   FileWrite(exports[1].fh, ts,o,h,l,c,v,SuperTrend_BAS_24_3); FileFlush(exports[1].fh); 
   FileWrite(exports[2].fh, ts,o,h,l,c,v,TrueRange); FileFlush(exports[2].fh); 
   FileWrite(exports[3].fh, ts,o,h,l,c,v,UlcerIndex_DownUI_24); FileFlush(exports[3].fh); 
   FileWrite(exports[4].fh, ts,o,h,l,c,v,UlcerIndex_UPUI_24); FileFlush(exports[4].fh); 
   
   FileWrite(exports[5].fh, ts,o,h,l,c,v,Vortex_VIPlus_12); FileFlush(exports[5].fh);
   FileWrite(exports[6].fh, ts,o,h,l,c,v,Vortex_VIMinus_12); FileFlush(exports[6].fh);
   FileWrite(exports[7].fh, ts,o,h,l,c,v,VWAP_10); FileFlush(exports[7].fh);

}


