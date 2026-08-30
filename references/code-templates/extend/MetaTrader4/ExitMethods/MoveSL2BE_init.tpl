<#-- here we get MoveSL 2 BE value from order settings and put it to a global variable -->
<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#") >
         // MoveSL2BE initialization
         sqSetGlobalVariable(_ticket, "MoveSL2BE", sqStringHash("<@fix_string><@printRangeLevelMethod block "#MoveSL2BE.MoveSL2BE#" "key" /></@fix_string>"));
         sqSetGlobalVariable(_ticket, "MoveSL2BEType", <@printRangeLevelMethodType block "#MoveSL2BE.MoveSL2BE#" "key" />);
</#if>
<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#") && hasSetParam(block, "#MoveSL2BE.SL2BEAddPips#") >
         sqSetGlobalVariable(_ticket, "SL2BEAddPips", sqStringHash("<@fix_string><@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" "key" /></@fix_string>"));
</#if>