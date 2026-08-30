	if(OpenOrdersAllowed) then begin         
		IntPriceLevel<@printOrderDirection block "#Direction#" "Long" "Short" /> = Round2Fraction(<@printPrice block "STOP" "#Price#" />);
		
		If not LimitMaxDistanceFromMarket or Round(absvalue(IntPriceLevel<@printOrderDirection block "#Direction#" "Long" "Short" /> - <@printOrderDirection block "#Direction#" "CurrentAsk" "CurrentBid" />) / <@printOrderDirection block "#Direction#" "CurrentAsk" "CurrentBid" /> * 100, 2) <= MaxDistancePct Then begin
			<#if doc.StrategyFile.Strategy.MoneyManagement.@type[0]?? && doc.StrategyFile.Strategy.MoneyManagement.@type != "FixedSize" && doc.StrategyFile.Strategy.MoneyManagement.@type != "MMNone">
			If UseMoneyManagement Then NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" /> 
			Else NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = mmLotsIfNoMM;

			<#else>
			NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> = <@printSizeMethod block "STOP" "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" />;
			</#if>
			If NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> > 0 Then begin
				<@printAllowDuplicateTradesCondition block "#AllowDuplicateTrades#" /> begin
					<@printOrderDirection block "#Direction#" "Buy" "SellShort" /> NumberOfShares<@printOrderDirection block "#Direction#" "Long" "Short" /> shares next bar at IntPriceLevel<@printOrderDirection block "#Direction#" "Long" "Short" /> stop;
			
					If UseInitialStopLoss Then begin
						<@printInitialSL block "STOP" />
						<@printInitialPT block "STOP" />
					end;
				end;
			end;
		end;
	end;
