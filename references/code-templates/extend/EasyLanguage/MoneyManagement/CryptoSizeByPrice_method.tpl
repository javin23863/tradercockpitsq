<@compress_single_line>
SQ_CryptoSizeByPrice(
<#if orderType == "MARKET">Close<#else>IntPriceLevel<@printOrderDirection block directionParamName "Long" "Short" /></#if>, 
mmUseAccountBalance,
mmMaxSize,
mmDecimals,
InitialCapital 
) * mmMultiplier
</@compress_single_line>