<#if hasSetParam(block, "#StopLoss.StopLoss#")>
	// StopLoss
<#if getOrderDirection(block, "#Direction#") = 1>
	IntLongSL = <@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" directionParamName priceParamName symbolParamName "true" />;
	IntLongSL = SQ_CorrectMinMaxSLPT(IntLongSL, MinimumSL, MaximumSL, true);
	If IntLongSL <> 0 Then begin
		Sell("LongSL") next bar at IntLongSL stop;
		LongSLPlaced = true;
	end Else IntLongSL = 0;
<#else>
	IntShortSL = <@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" directionParamName priceParamName symbolParamName "true" />;
	IntShortSL = SQ_CorrectMinMaxSLPT(IntShortSL, MinimumSL, MaximumSL, true);
	If IntShortSL <> 0 Then begin
		BuyToCover("ShortSL") next bar at IntShortSL stop;
		ShortSLPlaced = true;
	end Else IntShortSL = 0;
</#if> 

</#if>