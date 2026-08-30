      // Enter at Limit
      <#assign dirStr>
      <@printParam block "#Direction#" "" />
      </#assign>
      <#if dirStr?trim == "1">
      if(LongEnabled) {
      <#elseif dirStr?trim == "-1">
      if(ShortEnabled) {
      </#if>
      openPrice = sqFixMarketPrice(<@printPrice block "STOP" "#Price#" />, <@printSymbol block />);     
      sl = sqFixMarketPrice(<@printSLPTMethod block "LIMIT" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" />, <@printSymbol block />);
      pt = sqFixMarketPrice(<@printSLPTMethod block "LIMIT" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" />, <@printSymbol block />);
      size = <@printSizeMethod block "LIMIT" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />;
      
   <#if usesATM()>
      if(isNettingMode()){
         _ticket = openPosition(
            <@printOrderType block "LIMIT" "#Direction#" />, // Order type
            <@printSymbol block />, // Symbol
            size, // Size
            openPrice, // Price
            sl, // Stop Loss
            0, // Profit Target   
            correctSlippage(sqMaxEntrySlippage, <@printSymbol block />), // Max deviation
            <@printComment block />, // Comment
            <@printMagicNumber block />, // MagicNumber
            ExpirationTime, // Expiration time
            <@printParam block "#ReplaceExisting#" />, // Replace existing order (if it exists)
            <@printParam block "#AllowDuplicateTrades#" />  // Allow duplicate trades
         );

         if(_ticket > 0) {
            // order was successfuly placed, set or initialize all its exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
            <#if hasSetParam(block, "#BarsValid#") >
            sqSetOrderExpiration(_ticket, <@printParam block "#BarsValid#" />);
            </#if>

            <#list ExitMethodsList as exitMethod>
               <@printExitMethodInit exitMethod block "LIMIT" "#Direction#" "#Price#" />
            </#list>

            ulong exitTicket = 0;
            double exitSize = 0;
            double sizeRemaining = size;

            <@printNettingATMExits block "LIMIT" />
         }
      }
      else {
         double orderSize = 0;
         double sizeRemaining = size;
         bool openingAllowed = false;
         
         <@printHedgingATMOrders block "LIMIT" />
      }
   <#else>
      _ticket = openPosition(
         <@printOrderType block "LIMIT" "#Direction#" />, // Order type
         <@printSymbol block />, // Symbol
         size, // Size
         openPrice, // Price
         sl, // Stop Loss
         pt, // Profit Target   
         correctSlippage(sqMaxEntrySlippage, <@printSymbol block />), // Max deviation
         <@printComment block />, // Comment
         <@printMagicNumber block />, // MagicNumber
         ExpirationTime, // Expiration time
         <@printParam block "#ReplaceExisting#" />, // Replace existing order (if it exists)
         <@printParam block "#AllowDuplicateTrades#" />  // Allow duplicate trades
      );

      if(_ticket > 0) {
         // order was successfuly placed, set or initialize all its exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#if hasSetParam(block, "#BarsValid#") >
         sqSetOrderExpiration(_ticket, <@printParam block "#BarsValid#" />);
         </#if>

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "LIMIT" "#Direction#" "#Price#" />
         </#list>
      }
   </#if>
      } // end direction check
