
<#-- here we get MoveSL 2 BE value from order settings and put it to a global variable -->
<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#") >
         // MoveSL2BE initialization
         sqSetMoveSL2BE(_ticket, "<@escape_string><@printRangeLevelMethod block "#MoveSL2BE.MoveSL2BE#" "key" /></@escape_string>", <@printRangeLevelMethodType block "#MoveSL2BE.MoveSL2BE#" "key" />);
</#if>
<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#") && hasSetParam(block, "#MoveSL2BE.SL2BEAddPips#") >
         sqSetSL2BEAddPips(_ticket, "<@escape_string><@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" "key" /></@escape_string>");
</#if>