
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") >
         // TrailingStop initialization
         sqSetTrailingStop(_ticket, "<@escape_string><@printRangeLevelMethod block "#TrailingStop.TrailingStop#" "key" /></@escape_string>", <@printRangeLevelMethodType block "#TrailingStop.TrailingStop#" "key" />);
</#if>
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") && hasSetParam(block, "#TrailingStop.TrailingActivation#") >
         sqSetTSActivation(_ticket, "<@escape_string><@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" "key" /></@escape_string>");
</#if>