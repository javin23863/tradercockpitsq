if(MarketPosition <@printOrderDirection block "#Direction#" ">" "<" /> 0) then begin
<#if isEntryOrder(block) >

	<@printOrderDirection block "#Direction#" "Long" "Short" />SLPlaced = false;

	If BarsSinceEntry(0) = 0 then begin
		Int<@printOrderDirection block "#Direction#" "Long" "Short" />SL = 0;
		Int<@printOrderDirection block "#Direction#" "Long" "Short" />TS = 0;
		Int<@printOrderDirection block "#Direction#" "Long" "Short" />BE = 0;
	end;

 	<@printExitMethodInit "StopLoss" block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
	<@printExitMethodInit "ProfitTarget" block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
<#if usesATM()>
	RemainingSize = NumberOfShares;
	<@printATMExits block />
<#else>
	<#list ExitMethodsList as exitMethod>
	<@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
	</#list>
		
	If IntPT <> 0 then begin 
		<@printOrderDirection block "#Direction#" "Sell" "BuyToCover" /> next bar at IntPT limit;
	end;
		
	if <@printOrderDirection block "#Direction#" "Long" "Short" />SLPlaced = false and Int<@printOrderDirection block "#Direction#" "Long" "Short" />SL > 0 then begin
		// no SL activated this bar, use default one - handling if there is no SL, only Trailing stop or Move2BE
		<@printOrderDirection block "#Direction#" "Sell" "BuyToCover" /> next bar at Int<@printOrderDirection block "#Direction#" "Long" "Short" />SL stop;
	end;	
</#if>
</#if>
end;
