
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") >
         // TrailingStop initialization
         sqSetGlobalVariable(ticket, "TrailingStop", sqStringHash("<@fix_string><@printRangeLevelMethod block "#TrailingStop.TrailingStop#" "key" /></@fix_string>"));
         sqSetGlobalVariable(ticket, "TrailingStopType", <@printRangeLevelMethodType block "#TrailingStop.TrailingStop#" "key" />);
</#if>
<#if hasSetParam(block, "#TrailingStop.TrailingStop#") && hasSetParam(block, "#TrailingStop.TrailingActivation#") >
         sqSetGlobalVariable(ticket, "TrailingActivation", sqStringHash("<@fix_string><@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" "key" /></@fix_string>"));
</#if>