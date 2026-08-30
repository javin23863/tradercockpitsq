
void sqManageExitAfterXBars(ulong ticket) {
   if(!PositionSelectByTicket(ticket)){
       Verbose("Exit after bars - Cannot select position with ticket ", IntegerToString(ticket));
       return;
   }

   int exitBars = sqGetExitAfterXBars(ticket);
   if(exitBars > 0) {
      if (sqGetOpenBarsForOrder(exitBars+10, PositionGetInteger(POSITION_TIME)) >= exitBars) {
         Verbose("Exit After ", IntegerToString(exitBars), " bars - closing order with ticket: ", IntegerToString(ticket));
         if(!sqClosePositionAtMarket(ticket)){
            Verbose("Closing position failed, error: ", IntegerToString(GetLastError()), " - ", ErrorDescription(GetLastError()));
         }
      }
   }
}