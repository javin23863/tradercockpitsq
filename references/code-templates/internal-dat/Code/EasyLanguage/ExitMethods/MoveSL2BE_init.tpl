<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#")>

	// Move SL to BE
	IntPriceLevel = <@printRangeLevelMethod block "#MoveSL2BE.MoveSL2BE#" />;
	<#if getOrderDirection(block, "#Direction#") = 1>
	If IntPriceLevel > 0 and (Entryprice <= Round2Fraction(Close - IntPriceLevel)) and (IntLongBE = 0 or IntLongBE < Entryprice + <@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" />) then begin
		IntLongBE = Entryprice + <@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" />;
	end;
	If IntLongBE > 0 and IntLongBE > IntLongSL and Close > IntLongBE then begin
		Sell("LongSL2BE") next bar at IntLongBE stop;
		LongSLPlaced = true;
	end;		
	<#else>
	If IntPriceLevel > 0 and (Entryprice >= Round2Fraction(Close + IntPriceLevel)) and (IntShortBE = 0 or IntShortBE > Entryprice - <@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" />) then begin
		IntShortBE = Entryprice - <@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" />;
	end;
	If IntShortBE > 0 and (IntShortBE < IntShortSL or IntShortSL = 0) and Close < IntLongBE then begin
		BuyToCover("ShortSL2BE") next bar at IntShortBE stop;
		ShortSLPlaced = true;
	end;		
</#if> 
</#if>