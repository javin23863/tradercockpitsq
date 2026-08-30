         // StopLoss & ProfitTarget
<#if hasSetParam(block, "#StopLoss.StopLoss#") || hasSetParam(block, "#ProfitTarget.ProfitTarget#")>
         sqSetSLandPT(_ticket, <@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" directionParamName priceParamName "true" />, <@printSLPTMethod block orderType "PT" "#ProfitTarget.ProfitTarget#" directionParamName priceParamName "true" />);
</#if>