<@compress_single_line>
SQ_RiskFixedPctBalance(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>,  
SQ_CorrectMinMaxSLPT(<@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName symbolParamName "false" />, MinimumSL, MaximumSL, true),
mmRiskPercent,
mmDecimals,
mmLotsIfNoMM,
mmMaxLots,
InitialCapital,
mmMultiplier
)
</@compress_single_line>