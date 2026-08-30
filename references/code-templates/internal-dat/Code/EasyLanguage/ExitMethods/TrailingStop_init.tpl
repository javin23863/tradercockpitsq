<#if hasSetParam(block, "#TrailingStop.TrailingStop#")>
         
	// Trailing Stop
	IntPriceLevel = <@printRangeLevelMethod block "#TrailingStop.TrailingStop#" />;
	<#if getOrderDirection(block, "#Direction#") = 1>
	If IntPriceLevel > 0 and Close - EntryPrice >= Round2Fraction(<@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" />) and (IntLongTS = 0 or Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close - </#if>IntPriceLevel) > IntLongTS) and Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close - </#if>IntPriceLevel) < CurrentBid then begin
		IntLongTS = Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close - </#if>IntPriceLevel); // remember also trailing stop
	end;
	If IntLongTS > 0 and IntLongTS > IntLongSL then begin
		Sell("LongTrailingStop") next bar at IntLongTS stop;
		LongSLPlaced = true;
	end;		
	<#else>
	If IntPriceLevel > 0 and EntryPrice - Close >= Round2Fraction(<@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" />) and (IntShortTS = 0 or Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close + </#if>IntPriceLevel) < IntShortTS) and Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close + </#if>IntPriceLevel) > CurrentAsk then begin
		IntShortTS = Round2Fraction(<#if !isPriceLevelFormula(block, "#TrailingStop.TrailingStop#")>Close + </#if>IntPriceLevel);
	end;   
	If IntShortTS > 0 and (IntShortTS < IntShortSL or IntShortSL = 0)  then begin
		BuyToCover("ShortTrailingStop") next bar at IntShortTS stop;
		ShortSLPlaced = true;
	end;		
	</#if> 
</#if>
