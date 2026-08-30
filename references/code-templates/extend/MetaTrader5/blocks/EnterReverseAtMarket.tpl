      <#assign dirStr>
      <@printParam block "#Direction#" "" />
      </#assign>
      <#if dirStr?trim == "1">
      if(LongEnabled) {
      <#elseif dirStr?trim == "-1">
      if(ShortEnabled) {
      </#if>
      openingOrdersAllowed = openingOrdersAllowed && sqHandleTradingOptions();  
      if(openingOrdersAllowed){
          // Enter/Reverse, so first close existing opposite position and pending orders
         <#if usesATM()>
          while(sqSelectPosition(<@printMagicNumber block />, <@printSymbol block />, <@printOppositeDirection block />, <@printComment block />)) {
             sqClosePositionAtMarket(PositionGetInteger(POSITION_TICKET));
          }
         <#else>
          if(sqSelectPosition(<@printMagicNumber block />, <@printSymbol block />, <@printOppositeDirection block />, <@printComment block />)) {
             sqClosePositionAtMarket(PositionGetInteger(POSITION_TICKET));
          }
         </#if>
          openPrice = 0;  //Open at current market price
          sl = sqFixMarketPrice(<@printSLPTMethod block "MARKET" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" />, <@printSymbol block />);
          pt = sqFixMarketPrice(<@printSLPTMethod block "MARKET" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" />, <@printSymbol block />);
          size = <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />;
      
   <#if usesATM()>
      if(isNettingMode()){
         _ticket = openPosition(
            <@printOrderType block "MARKET" "#Direction#" />, // Order type
            <@printSymbol block />, // Symbol
            size, // Size
            openPrice, // Price
            sl, // Stop Loss
            0, // Profit Target   
            correctSlippage(sqMaxEntrySlippage, <@printSymbol block />), // Max deviation
            <@printComment block />, // Comment
            <@printMagicNumber block />, // MagicNumber
            ExpirationTime, // Expiration time
            false, // Replace existing (only for pending orders)
            <@printParam block "#AllowDuplicateTrades#" />  // Allow duplicate trades
         );

         if(_ticket > 0) {
            // order was successfuly opened, set or initialize all its exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)

            <#list ExitMethodsList as exitMethod>
               <@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" />
            </#list>

            ulong exitTicket = 0;
            double exitSize = 0;
            double sizeRemaining = size;

            <@printNettingATMExits block "MARKET" />
         }
      }
      else {
         double orderSize = 0;
         double sizeRemaining = size;
         bool openingAllowed = false;
         
         <@printHedgingATMOrders block "MARKET" />
      }
   <#else>
      _ticket = openPosition(
         <@printOrderType block "MARKET" "#Direction#" />, // Order type
         <@printSymbol block />, // Symbol
         size, // Size
         openPrice, // Price
         sl, // Stop Loss
         pt, // Profit Target   
         correctSlippage(sqMaxEntrySlippage, <@printSymbol block />), // Max deviation
         <@printComment block />, // Comment
         <@printMagicNumber block />, // MagicNumber
         ExpirationTime, // Expiration time
         false, // Replace existing (only for pending orders)
         <@printParam block "#AllowDuplicateTrades#" />  // Allow duplicate trades
      );

      if(_ticket > 0) {
         // order was successfuly opened, set or initialize all its exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" />
         </#list>
      }
   </#if>
      }
      } // end direction check