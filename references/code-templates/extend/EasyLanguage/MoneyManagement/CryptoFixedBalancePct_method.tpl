<@compress_single_line>
SQ_CryptoFixedPctBalance(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>,  
mmRiskPercent,
mmDecimals,
InitialCapital,
mmMultiplier    
)
</@compress_single_line>