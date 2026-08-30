   <#if usesATM()>
      double openPrice = 0;     
      double sl = <@printSLPTMethod block "MARKET" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "#Symbol#" "true" />;
      double pt = <@printSLPTMethod block "MARKET" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "#Symbol#" "true" />;
      double size = <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />;
      
      double orderSize = 0;
      double sizeRemaining = size;
      boolean openingAllowed = false;
   
      <@printHedgingATMOrders block "MARKET" />
	<#else>
      order = sqOpenOrder(<@printOrderType block "MARKET" "#Direction#" />, "<@printParam block "#Symbol#" />", <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />, 0, <@printMagicNumber block />, <@printComment block />, 0, false, <@printParam block "#AllowDuplicateTrades#" />, null, false);

      if(order != null) {
		 ticket = order.getLabel();
	  
         // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
         </#list>
      }
	</#if>