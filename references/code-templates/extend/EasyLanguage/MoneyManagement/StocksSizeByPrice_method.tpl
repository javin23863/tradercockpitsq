<@compress_single_line>
SQ_StocksSizeByPrice(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>, 
mmUseAccountBalance,
mmMaxSize,
InitialCapital,
mmMultiplier
)
</@compress_single_line>