//+------------------------------------------------------------------+
//|                                                        SqVWAP.mq4|
//|                            Copyright © @2021 StrategyQuant s.r.o.|
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property  copyright "Copyright © @2021 StrategyQuant s.r.o."
#property  link      "http://www.strategyquant.com"

//---- indicator settings
#property  indicator_chart_window;
#property  indicator_buffers 1
#property  indicator_color1  Red
#property  indicator_width1  1

//----
extern int VWAPPeriod=10;

//---- buffers
double IndiBuffer[];

//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
int init()
  {

   IndicatorBuffers(1);
   SetIndexStyle(0,DRAW_LINE);
   SetIndexDrawBegin(0,VWAPPeriod);
   IndicatorDigits(MarketInfo(Symbol(),MODE_DIGITS)+2);
   SetIndexBuffer(0,IndiBuffer,INDICATOR_DATA);
   IndicatorShortName("SqVWAP("+VWAPPeriod+")");

   return(0);
  }

//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
int start()
  {
  	

   int i, limit;
   int counted_bars=IndicatorCounted();
   if(counted_bars>0)
      counted_bars--;
   limit=Bars-counted_bars;

   for(i=limit; i>=0; i--)
     {
      double ohlcAvg = 0;
      double vol =0;
      double __ohlcvTotal = 0;
      double __volumeTotal = 0;

      for(int p = 0; p< VWAPPeriod; p++){
           
      ohlcAvg = (Open[i+p]+High[i+p]+Low[i+p]+Close[i+p])/4;
      vol = Volume[i+p];
           
         __ohlcvTotal = __ohlcvTotal + (ohlcAvg*vol);
			__volumeTotal =__volumeTotal + vol;
      
      }
      

      if(__volumeTotal!=0)double vwap = __ohlcvTotal/__volumeTotal;
      

      IndiBuffer[i] = vwap;


     }
    
   return(0);
  }

//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
