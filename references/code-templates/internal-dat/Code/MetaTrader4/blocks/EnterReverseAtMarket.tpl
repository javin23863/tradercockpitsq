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
          // Enter/Reverse, so first close opposite order if exists
      <#if usesATM()>
          while(sqSelectOrder(<@printMagicNumber block />, <@printSymbol block />, -1*<@printParam block "#Direction#" />, <@printComment block />)) {
             sqClosePosition(<@printCloseSizeMethod block "#Size#" />, // size: <@printParam block "#Size#" />
                          <@printMagicNumber block />, // magic number
                          <@printSymbol block />, // symbol
                          -1*<@printParam block "#Direction#" />, // direction
                          <@printComment block /> // comment
             );
          }
      <#else>
          if(sqSelectOrder(<@printMagicNumber block />, <@printSymbol block />, -1*<@printParam block "#Direction#" />, <@printComment block />)) {
             sqClosePosition(<@printCloseSizeMethod block "#Size#" />, // size: <@printParam block "#Size#" />
                          <@printMagicNumber block />, // magic number
                          <@printSymbol block />, // symbol
                          -1*<@printParam block "#Direction#" />, // direction
                          <@printComment block /> // comment
             );
          }
      </#if>
      <#if usesATM()>
          double openPrice = 0;     
          double sl = <@printSLPTMethod block "MARKET" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "true" />;
          double pt = <@printSLPTMethod block "MARKET" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "true" />;
          double size = <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />;
            
          double orderSize = 0;
          double sizeRemaining = size;
          bool openingAllowed = false;
         
          <@printHedgingATMOrders block "MARKET" />
      <#else>
          // open new position
          _ticket = sqOpenOrder(<@printOrderType block "MARKET" "#Direction#" />, <@printSymbol block />, <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />, 0, <@printMagicNumber block />, <@printComment block />, 0, false, <@printParam block "#AllowDuplicateTrades#" />, CLR_NONE);
    
          if(_ticket > 0 && OrderSelect(_ticket, SELECT_BY_TICKET)) {
             // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
             <#list ExitMethodsList as exitMethod>
                <@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" />
             </#list>
          }
      </#if>
      }
      } // end direction check