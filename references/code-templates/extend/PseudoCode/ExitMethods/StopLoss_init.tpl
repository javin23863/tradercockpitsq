<#if hasSetParam(block, "#StopLoss.StopLoss#") >
        Stop Loss <@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" directionParamName priceParamName symbolParamName />;
</#if>