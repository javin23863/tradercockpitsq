<#if hasSetParam(block, "#ExitAfterBars.ExitAfterBars#") >
	
	// ExitAfterBars
	if (<@printParam block "#ExitAfterBars.ExitAfterBars#" "key" /> > 0 and BarsSinceEntry(0) >= <@printParam block "#ExitAfterBars.ExitAfterBars#" "key" />) then
		<@printOrderDirection block "#Direction#" "Sell" "BuyToCover" />("<@printOrderDirection block "#Direction#" "Long" "Short" />ExitAfterXBars") next bar at market;
</#if>