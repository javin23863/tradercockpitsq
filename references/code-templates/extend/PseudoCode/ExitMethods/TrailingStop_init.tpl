<#if hasSetParam(block, "#TrailingStop.TrailingStop#") >

        Trailing Stop = <@printRangeLevelMethod block "#TrailingStop.TrailingStop#" "key" /><#if hasTrailingActivation(block) >, TS Activation at <@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" "key" /></#if>;
</#if>