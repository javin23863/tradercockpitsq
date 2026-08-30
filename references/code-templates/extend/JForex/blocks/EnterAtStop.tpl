   <#if usesATM()>
      double openPrice = <@printPrice block "STOP" "#Price#" />;     
      double sl = <@printSLPTMethod block "STOP" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "#Symbol#" "false" />;
      double pt = <@printSLPTMethod block "STOP" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "#Symbol#" "false" />;
      double size = <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />;
      
      double orderSize = 0;
      double sizeRemaining = size;
      boolean openingAllowed = false;

      <@printHedgingATMOrders block "STOP" />
   <#else>
      order = sqOpenOrder(<@printOrderType block "STOP" "#Direction#" />, "<@printParam block "#Symbol#" />", <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />, <@printPrice block "STOP" "#Price#" />, <@printMagicNumber block />, <@printComment block />, 0, <@printParam block "#ReplaceExisting#" />, <@printParam block "#AllowDuplicateTrades#" />, null, false);

      if(order != null) {
		 ticket = order.getLabel();
	  
         // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
         <#if hasSetParam(block, "#BarsValid#") >
         sqSetOrderExpiration(ticket, <@printParam block "#BarsValid#" />);
         </#if>

         <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "STOP" "#Direction#" "#Price#" "#Symbol#" />
         </#list>
      }
   </#if>

