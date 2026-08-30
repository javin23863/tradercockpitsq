#property copyright "StrategyQuant"
#property link      ""
#property show_inputs
#property strict

void OnDeinit(const int reason) {

}

void Save(int file_handle, string str){
 FileWriteString(file_handle,str+"\r\n");
 FileFlush(file_handle);
 Print(str);
}

void OnStart(){
   ResetLastError();
   int file_handle=FileOpen("mt4.properties",FILE_WRITE|FILE_TXT);
   Print(file_handle);
   if(file_handle<0)
   {
      Print("Failed to open the file by the absolute path ");
      Print("Error code ",GetLastError());
   }  
    
   for (int i = 0; i < SymbolsTotal(true); i++) {
      string s_symbol=SymbolName(i,true);
      Print("Exporting settings for symbol: "+s_symbol);
      double d_lot_min=MarketInfo(s_symbol,MODE_MINLOT);
      double d_lot_max=MarketInfo(s_symbol,MODE_MAXLOT);
      double d_lot_step=MarketInfo(s_symbol,MODE_LOTSTEP);
      int i_stops_level=(int)MarketInfo(s_symbol,MODE_STOPLEVEL);
      double d_contract_size=MarketInfo(s_symbol,MODE_LOTSIZE);
      double d_tick_value=MarketInfo(s_symbol,MODE_TICKVALUE);
      double d_tick_size=MarketInfo(s_symbol,MODE_TICKSIZE);
      int i_profit_mode=(int)MarketInfo(s_symbol,MODE_PROFITCALCMODE);
      int i_swap_type=(int)MarketInfo(s_symbol,MODE_SWAPTYPE);
      double d_swap_long=MarketInfo(s_symbol,MODE_SWAPLONG);
      double d_swap_short=MarketInfo(s_symbol,MODE_SWAPSHORT);
      int  i_free_margin_mode=AccountFreeMarginMode();
      int i_margin_mode=(int)MarketInfo(s_symbol,MODE_MARGINCALCMODE);
      int i_margin_stopout=AccountStopoutLevel();
      int i_margin_stopout_mode=AccountStopoutMode();
      double d_margin_initial=MarketInfo(s_symbol,MODE_MARGININIT);
      double d_margin_maintenance=MarketInfo(s_symbol,MODE_MARGINMAINTENANCE);
      double d_margin_hedged=MarketInfo(s_symbol,MODE_MARGINHEDGED);
      int i_freeze_level=(int)MarketInfo(s_symbol,MODE_FREEZELEVEL);
      int i_spread = (int)MarketInfo(s_symbol,MODE_SPREAD);
            
      Save(file_handle, s_symbol+"_SPREAD="+i_spread);
      Save(file_handle, s_symbol+"_LOT_MIN="+d_lot_min);
      Save(file_handle, s_symbol+"_LOT_MAX="+d_lot_max);
      Save(file_handle, s_symbol+"_LOT_STEP="+d_lot_step);
      Save(file_handle, s_symbol+"_STOPS_LEVEL="+i_stops_level);
      Save(file_handle, s_symbol+"_CONTRACT_SIZE="+d_contract_size);
      Save(file_handle, s_symbol+"_TICK_VALUE="+d_tick_value);
      Save(file_handle, s_symbol+"_TICK_SIZE="+d_tick_size);
      Save(file_handle, s_symbol+"_PROFIT_MODE="+i_profit_mode);
      Save(file_handle, s_symbol+"_SWAP_TYPE="+i_swap_type);
      Save(file_handle, s_symbol+"_SWAP_LONG="+d_swap_long);
      Save(file_handle, s_symbol+"_SWAP_SHORT="+d_swap_short);
      
      Save(file_handle, s_symbol+"_FREE_MARGIN_MODE="+i_free_margin_mode);
      Save(file_handle, s_symbol+"_MARGIN_MODE="+i_margin_mode);
      Save(file_handle, s_symbol+"_MARGIN_STOPOUT="+i_margin_stopout);
      Save(file_handle, s_symbol+"_MARGIN_STOPOUT_MODE="+i_margin_stopout_mode);
      Save(file_handle, s_symbol+"_MARGIN_INITIAL="+d_margin_initial);
      Save(file_handle, s_symbol+"_MARGIN_MAINTENANCE="+d_margin_maintenance);
      Save(file_handle, s_symbol+"_MARGIN_HEDGED="+d_margin_hedged);
      Save(file_handle, s_symbol+"_FREEZE_LEVEL="+(int)MarketInfo(s_symbol,MODE_FREEZELEVEL));
            
      Save(file_handle, s_symbol+"_MODE_BID="+(int)MarketInfo(s_symbol,MODE_BID));     
      Save(file_handle, s_symbol+"_MODE_MARGINREQUIRED="+(int)MarketInfo(s_symbol,MODE_MARGINREQUIRED));    
      Save(file_handle, s_symbol+"_MODE_MARGINCALCMODE="+(int)MarketInfo(s_symbol,MODE_MARGINCALCMODE));     
      Save(file_handle, s_symbol+"_MODE_POINT="+MarketInfo(s_symbol,MODE_POINT));        
      Save(file_handle, s_symbol+"_MODE_DIGITS="+MarketInfo(s_symbol,MODE_DIGITS));            
      Save(file_handle, s_symbol+"_CURRENCY="+SymbolInfoString(s_symbol,SYMBOL_CURRENCY_BASE));    
             
      FileWriteString(file_handle,"\r\n");
   }
   FileClose(file_handle);
} 