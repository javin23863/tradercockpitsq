
void sqManageExitAfterXBars(int ticket) {
   int exitBars = (int) sqGetGlobalVariable(ticket, "ExitAfterBars");
   if(exitBars > 0) {
      if (sqGetOpenBarsForOrder(ticket, exitBars+10) >= exitBars) {
         Verbose("Exit After ", IntegerToString(exitBars), "bars - closing order with ticket: ", IntegerToString(OrderTicket()));
         sqClosePositionAtMarket(OrderLots());
      }
   }
}