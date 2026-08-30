<@compress_single_line>
SQ_CryptoFixedAmount(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>,  
mmRiskedMoney,
mmDecimals,
InitialCapital,
mmMultiplier
)
</@compress_single_line>