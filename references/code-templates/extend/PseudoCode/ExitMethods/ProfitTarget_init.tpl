<#if hasSetParam(block, "#ProfitTarget.ProfitTarget#") >
        Profit target <@printSLPTMethod block orderType "PT" "#ProfitTarget.ProfitTarget#" directionParamName priceParamName symbolParamName />;
</#if>