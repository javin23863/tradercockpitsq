//+------------------------------------------------------------------+
//|                                          SQ_TickDataExportEA.mq4 |
//|                                                                  |
//|                           EA to export tick data from MetaTrader |
//|                              Tick data are exported to directory |
//|              /MetaTrader directory/tester/files/XXX_TickData.csv |
//+------------------------------------------------------------------+

#property copyright "Copyright © 2018 StrategyQuant"
#property link      "http://www.StrategyQuant.com"

int previousVolume = 0;
int handle;

int init() {

   handle = FileOpen(StringConcatenate(Symbol(),"_TickData.csv"), FILE_WRITE, ",");
   if(handle>0) {
      FileWrite(handle, "Time","Ask","Bid","Volume");
   }
   
   return(0);
}

//+------------------------------------------------------------------+

int start() {
   int volume = Volume[0] - previousVolume;
   if(volume <= 0) {
      volume = Volume[0];
   }
   previousVolume = Volume[0];
   
   if(handle>0) {
      FileWrite(handle, TimeToStr(TimeCurrent(), TIME_DATE|TIME_SECONDS), Ask, Bid, volume);
   }

   return(0);
}

int deinit() {
   FileClose(handle);
   return(0);
} 


