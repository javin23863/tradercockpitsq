   <#assign dirStr>
   	<@printParam block "#Direction#" "" />
   </#assign>
   <#if dirStr?trim == "1">
   if(LongEnabled) {
   <#elseif dirStr?trim == "-1">
   if(ShortEnabled) {
   </#if>
   <#if usesATM()>
      double openPrice = <@printPrice block "STOP" "#Price#" />;     
      double sl = <@printSLPTMethod block "LIMIT" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "true" />;
      double pt = <@printSLPTMethod block "LIMIT" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "true" />;
      double size = <@printSizeMethod block "LIMIT" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />;
      
      double orderSize = 0;
      double sizeRemaining = size;
      bool openingAllowed = false;
      
      <@printHedgingATMOrders block "LIMIT" />
   <#else>
      _ticket = sqOpenOrder(<@printOrderType block "LIMIT" "#Direction#" />, <@printSymbol block />, <@printSizeMethod block "LIMIT" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />, <@printPrice block "STOP" "#Price#" />, <@printMagicNumber block />, <@printComment block />, 0, <@printParam block "#ReplaceExisting#" />, <@printParam block "#AllowDuplicateTrades#" />, CLR_NONE);

      if(_ticket > 0 && OrderSelect(_ticket, SELECT_BY_TICKET)) {
         // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#if hasSetParam(block, "#BarsValid#") >
         sqSetOrderExpiration(_ticket, <@printParam block "#BarsValid#" />);
         </#if>

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "LIMIT" "#Direction#" "#Price#" />
         </#list>
      }
   </#if>
   } // end direction check
