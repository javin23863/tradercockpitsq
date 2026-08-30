<#if hasSetParam(block, "#ProfitTarget.ProfitTarget#")>
	// ProfitTarget init
	IntPT = <@printSLPTMethod block orderType "PT" "#ProfitTarget.ProfitTarget#" directionParamName priceParamName symbolParamName "true" />;
	IntPT = SQ_CorrectMinMaxSLPT(IntPT, MinimumPT, MaximumPT, false);
	If IntPT < 0 Then IntPT = 0;
<#else>
	// ProfitTarget init
	IntPT = 0;
</#if>