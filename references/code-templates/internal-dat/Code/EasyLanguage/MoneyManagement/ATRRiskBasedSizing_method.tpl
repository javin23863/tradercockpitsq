<@compress_single_line>
SQ_ATRRiskBasedSizing(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>,  
mmRiskPercent,
mmATRPeriod,
mmATRMultiplier,
mmDecimals,
mmLotsIfNoMM,
mmMaxLots,
InitialCapital,
mmMultiplier
)
</@compress_single_line>