         // StopLoss & ProfitTarget
<#if hasSetParam(block, "#StopLoss.StopLoss#") || hasSetParam(block, "#ProfitTarget.ProfitTarget#")>
         sqSetSLandPT(order, <@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" directionParamName priceParamName symbolParamName "true" />, <@printSLPTMethod block orderType "PT" "#ProfitTarget.ProfitTarget#" directionParamName priceParamName symbolParamName "true" />);
</#if>