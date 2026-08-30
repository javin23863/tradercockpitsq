
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") >
         // TrailingStop initialization
         sqSetGlobalVariable(_ticket, "TrailingStop", sqStringHash("<@fix_string><@printRangeLevelMethod block "#TrailingStop.TrailingStop#" "key" /></@fix_string>"));
         sqSetGlobalVariable(_ticket, "TrailingStopType", <@printRangeLevelMethodType block "#TrailingStop.TrailingStop#" "key" />);
</#if>
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") && hasSetParam(block, "#TrailingStop.TrailingActivation#") >
         sqSetGlobalVariable(_ticket, "TrailingActivation", sqStringHash("<@fix_string><@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" "key" /></@fix_string>"));
</#if>