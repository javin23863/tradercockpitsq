
void sqCheckSLPT(ulong ticket, double sl, double pt){
   if(sl == 0 && pt == 0) return;
   
   double orderSL = 0;
   double orderPT = 0;
   
   bool found = false;
   
   if(PositionSelectByTicket(ticket)) {
      orderSL = PositionGetDouble(POSITION_SL);
      orderPT = PositionGetDouble(POSITION_TP);
      found = true;
   }
   else if(OrderSelect(ticket)){
      orderSL = OrderGetDouble(ORDER_SL);
      orderPT = OrderGetDouble(ORDER_TP);
      found = true;
   }
   else {
      //Check current position (used in netting mode)
      HistorySelect(startTime, TimeCurrent());
      
      if(HistoryDealSelect(ticket)){
          long positionTicket = HistoryDealGetInteger(ticket, DEAL_POSITION_ID);
          
          if(PositionSelectByTicket(positionTicket)){
              ENUM_POSITION_TYPE positionType = (ENUM_POSITION_TYPE) PositionGetInteger(POSITION_TYPE);
              ENUM_DEAL_TYPE dealType = (ENUM_DEAL_TYPE) HistoryDealGetInteger(ticket, DEAL_TYPE);
              
              if((positionType == POSITION_TYPE_BUY && dealType == DEAL_TYPE_BUY) || 
                  (positionType == POSITION_TYPE_SELL && dealType == DEAL_TYPE_SELL)
              ){
                  orderSL = PositionGetDouble(POSITION_SL);
                  orderPT = PositionGetDouble(POSITION_TP);
                  found = true;
              }
          }
      }
   }
          
   if(!found){
      Print(StringFormat("No order or position with ticket %d found", IntegerToString(ticket)));
      return;
   }
   
   if(orderSL != sl || orderPT != pt){
      Print(StringFormat("SL or PT of order %d not set correctly. Order SL: %f (should be %f), Order PT: %f (should be %f). Modifying order...", ticket, orderSL, sl, orderPT, pt));
      
      sqSetSLPT(ticket, sl, pt);
   }
}

//+------------------------------------------------------------------+

void sqSetSLPT(ulong ticket, double sl, double pt){
   if(sl == 0 && pt == 0) return;
   
   ZeroMemory(mrequest);
   
   double openPrice = 0;
   bool isPosition = false;
   
   if(PositionSelectByTicket(ticket)) {
      isPosition = true;
      
      mrequest.position = ticket;
      mrequest.action = TRADE_ACTION_SLTP;
      mrequest.sl = sl > 0 ? sl : PositionGetDouble(POSITION_SL);
      mrequest.tp = pt > 0 ? pt : PositionGetDouble(POSITION_TP);
      
      openPrice = PositionGetDouble(POSITION_PRICE_OPEN);
   }
   else if(OrderSelect(ticket)){
      mrequest.order = ticket;
      mrequest.action = TRADE_ACTION_MODIFY;
      mrequest.price = OrderGetDouble(ORDER_PRICE_OPEN);
      mrequest.sl = sl > 0 ? sl : OrderGetDouble(ORDER_SL);
      mrequest.tp = pt > 0 ? pt : OrderGetDouble(ORDER_TP);
      
      openPrice = OrderGetDouble(ORDER_PRICE_OPEN);
   }
   else {
      //Check current position (used in netting mode)
      HistorySelect(startTime, TimeCurrent());
      
      if(HistoryDealSelect(ticket)){
          long positionTicket = HistoryDealGetInteger(ticket, DEAL_POSITION_ID);
          
          if(PositionSelectByTicket(positionTicket)){
              ENUM_POSITION_TYPE positionType = (ENUM_POSITION_TYPE) PositionGetInteger(POSITION_TYPE);
              ENUM_DEAL_TYPE dealType = (ENUM_DEAL_TYPE) HistoryDealGetInteger(ticket, DEAL_TYPE);
              
              if((positionType == POSITION_TYPE_BUY && dealType == DEAL_TYPE_BUY) || 
                  (positionType == POSITION_TYPE_SELL && dealType == DEAL_TYPE_SELL)
              ){
                  isPosition = true;
      
                  mrequest.position = ticket;
                  mrequest.action = TRADE_ACTION_SLTP;
                  mrequest.sl = sl > 0 ? sl : PositionGetDouble(POSITION_SL);
                  mrequest.tp = pt > 0 ? pt : PositionGetDouble(POSITION_TP);
                  
                  openPrice = PositionGetDouble(POSITION_PRICE_OPEN);
              }
          }
      }
          
      if(!isPosition){
         Print(StringFormat("No order or position with ticket %d found", IntegerToString(ticket)));
         return;
      }
   }
   
   if(!isPosition && mrequest.sl == openPrice) {
      Print("SL is as same as order price, cannot set it, so we'll delete the order!");
      if(!closeOrder(ticket)) {
         Print("Warning! Cannot delete order and SL/PT was not set! Error: ", IntegerToString(GetLastError()));
      }

      return;
   }
   
   //--- setting request
   mrequest.symbol = isPosition ? PositionGetString(POSITION_SYMBOL) : OrderGetString(ORDER_SYMBOL);
   mrequest.magic = isPosition ? PositionGetInteger(POSITION_MAGIC) : OrderGetInteger(ORDER_MAGIC);   
   mrequest.sl = sqFixMarketPrice(mrequest.sl, mrequest.symbol);   
   mrequest.tp = sqFixMarketPrice(mrequest.tp, mrequest.symbol);
   
   //--- action and return the result
   if(!OrderSend(mrequest, mresult)){
      Print("Cannot set order SL/PT. Error: ", IntegerToString(GetLastError()));
      
      if(sl > 0){
          if(isPosition){
              if(!sqClosePositionAtMarket(ticket)){
                  Print("Cannot close position and SL is not set! Error: ", IntegerToString(GetLastError()));
              }
          }  
          else {
              if(!closeOrder(ticket)){
                  Print("Cannot close order and SL is not set! Error: ", IntegerToString(GetLastError()));
              }
          }
      }
   }
}

double getOrderOpenPrice(ulong ticket, double requestedPrice){
   if(OrderSelect(ticket)){
      return OrderGetDouble(ORDER_PRICE_OPEN);
   }
   else if(PositionSelectByTicket(ticket)){
      return PositionGetDouble(POSITION_PRICE_OPEN);
   }
   else return requestedPrice;
}
