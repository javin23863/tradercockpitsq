<@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Direction#">
		<#if param?number == 1>
			Sell("ClosePositionLong") next bar at market;
		<#elseif param?number == -1>
			BuyToCover("ClosePositionShort") next bar at market;
		<#else>
			Sell("ClosePositionLong") next bar at market;
			BuyToCover("ClosePositionShort") next bar at market;
		</#if>
   </#if>
</#list>
</@compress_single_line>