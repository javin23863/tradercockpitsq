	<#if usesATM()>
      double openPrice = <@printPrice block "STOP" "#Price#" />;     
      double sl = <@printSLPTMethod block "LIMIT" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "#Symbol#" "true" />;
      double pt = <@printSLPTMethod block "LIMIT" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "#Symbol#" "true" />;
      double size = <@printSizeMethod block "LIMIT" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />;
      
      double orderSize = 0;
      double sizeRemaining = size;
      boolean openingAllowed = false;
      
      <@printHedgingATMOrders block "LIMIT" />
   <#else>
      order = sqOpenOrder(<@printOrderType block "LIMIT" "#Direction#" />, "<@printParam block "#Symbol#" />", <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />, <@printPrice block "STOP" "#Price#" />, <@printMagicNumber block />, <@printComment block />, 0, <@printParam block "#ReplaceExisting#" />, <@printParam block "#AllowDuplicateTrades#" />, null, false);

      if(order != null) {
		 ticket = order.getLabel();
	  
         // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#if hasSetParam(block, "#BarsValid#") >
         sqSetOrderExpiration(ticket, <@printParam block "#BarsValid#" />);
         </#if>

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "LIMIT" "#Direction#" "#Price#" "#Symbol#" />
         </#list>
      }
   </#if>
