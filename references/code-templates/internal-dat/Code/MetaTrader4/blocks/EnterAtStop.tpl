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
      double sl = <@printSLPTMethod block "STOP" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "false" />;
      double pt = <@printSLPTMethod block "STOP" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "false" />;
      double size = <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />;
      
      double orderSize = 0;
      double sizeRemaining = size;
      bool openingAllowed = false;

      <@printHedgingATMOrders block "STOP" />
   <#else>
      _ticket = sqOpenOrder(<@printOrderType block "STOP" "#Direction#" />, <@printSymbol block />, <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" />, <@printPrice block "STOP" "#Price#" />, <@printMagicNumber block />, <@printComment block />, 0, <@printParam block "#ReplaceExisting#" />, <@printParam block "#AllowDuplicateTrades#" />, CLR_NONE);

      if(_ticket > 0 && OrderSelect(_ticket, SELECT_BY_TICKET)) {
         // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#if hasSetParam(block, "#BarsValid#") >
         sqSetOrderExpiration(_ticket, <@printParam block "#BarsValid#" />);
         </#if>

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "STOP" "#Direction#" "#Price#" />
         </#list>
      }
   </#if>
   } // end direction check