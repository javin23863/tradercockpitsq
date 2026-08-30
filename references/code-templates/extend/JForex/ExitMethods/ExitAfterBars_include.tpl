
void sqManageExitAfterXBars(IOrder order) throws JFException {
   ticket = order.getLabel();

   int exitBars = sqGetGlobalIntVariable(ticket, "ExitAfterBars");
   if(exitBars > 0) {
      if (sqGetOpenBarsForOrder(order, exitBars+10) >= exitBars) {
         print("Exit After ", exitBars, "bars - closing order with ticket: ", ticket);
         sqClosePositionAtMarket(order, 0, "Exit after "+exitBars+" bars");
      }
   }
}