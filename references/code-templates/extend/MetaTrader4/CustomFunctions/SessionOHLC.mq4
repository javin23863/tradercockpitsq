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
   return iCustom(correctedSymbol, tf, "SqSessionOHLC", type, startHours, startMinutes, endHours, endMinutes, daysAgo, 0, 0);
}