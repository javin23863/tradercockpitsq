double SessionOpen(string symbol, int tf, int startHours, int startMinutes, int daysAgo){
   return getSessionPrice(symbol, tf, 1, startHours, startMinutes, startHours, startMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionHigh(string symbol, int tf, int startHours, int startMinutes, int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(symbol, tf, 2, startHours, startMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionLow(string symbol, int tf, int startHours, int startMinutes, int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(symbol, tf, 3, startHours, startMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double SessionClose(string symbol, int tf, int endHours, int endMinutes, int daysAgo){
   return getSessionPrice(symbol, tf, 4, endHours, endMinutes, endHours, endMinutes, daysAgo);
}

//+------------------------------------------------------------------+

double getSessionPrice(string symbol, int tf, int type, int startHours, int startMinutes, int endHours, int endMinutes, int daysAgo){
   string correctedSymbol = correctSymbol(symbol);
   string handleKey = correctedSymbol + "_" + IntegerToString(tf) + "_" + IntegerToString(startHours) + IntegerToString(startMinutes) + "_" + IntegerToString(endHours) + IntegerToString(endMinutes) + "_" + IntegerToString(type) + "_" + IntegerToString(daysAgo); 
   
   if(!sessionOHLCHandles.ContainsKey(handleKey)){
      int handle = iCustom(correctedSymbol, (ENUM_TIMEFRAMES) tf, "SqSessionOHLC", type, startHours, startMinutes, endHours, endMinutes, daysAgo);
      sessionOHLCHandles.Add(handleKey, handle);
   }

   int indyHandle; 
   double buffer[];

   sessionOHLCHandles.TryGetValue(handleKey, indyHandle);

   if(CopyBuffer(indyHandle, 0, 0, 1, buffer) < 0) { 
      PrintFormat("Failed to copy data from the sessionOHLC indicator with key %s, error code %d", handleKey, GetLastError());  
      IndicatorLoadedWithoutError = false;
      return 0;
   } 

   return buffer[0];
}