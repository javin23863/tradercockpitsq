	if(OpenOrdersAllowed) then begin
		<#if doc.StrategyFile.Strategy.MoneyManagement.@type[0]?? && doc.StrategyFile.Strategy.MoneyManagement.@type != "FixedSize" && doc.StrategyFile.Strategy.MoneyManagement.@type != "MMNone">
		If UseMoneyManagement Then NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" /> 
		Else NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = mmLotsIfNoMM;
		
		<#else>
		NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = <@printSizeMethod block "MARKET" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />;
		</#if>
		If NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> > 0 Then begin
			if(MarketPosition <@printOrderDirection block "#Direction#" "<=" ">=" /> 0) then begin
				<@printOrderDirection block "#Direction#" "Buy" "SellShort" />("<@printOrderDirection block "#Direction#" "Long" "Short" />Market") NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> shares next bar at market;
		
				If UseInitialStopLoss Then begin
					<@printInitialSL block "MARKET" />
					<@printInitialPT block "MARKET" />
				end;
			end;
		end;
	end;