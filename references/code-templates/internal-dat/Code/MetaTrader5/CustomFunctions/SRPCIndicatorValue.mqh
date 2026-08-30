//+------------------------------------------------------------------+

double getSRPCIndicatorValue(int indyHandle, int bufferIndex, int shift){
   double buffer[];
   
   if(CopyBuffer(indyHandle, bufferIndex, shift, 1, buffer) < 0) { 
      //--- if the copying fails, tell the error code 
      PrintFormat("Failed to copy data from the indicator, error code %d", GetLastError()); 
      //--- quit with zero result - it means that the indicator is considered as not calculated 
      IndicatorLoadedWithoutError = false;
      return(0); 
   } 
   
   double val = buffer[0];
   return val;
}