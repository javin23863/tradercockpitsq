<#-- here we get MoveSL 2 BE value from order settings and put it to a global variable -->
<#if hasSetParam(block, "#MoveSL2BE.MoveSL2BE#") >

        Move SL to BE = <@printRangeLevelMethod block "#MoveSL2BE.MoveSL2BE#" "key" /><#if hasSetParam(block, "#MoveSL2BE.SL2BEAddPips#") >, add <@printRangeLevelMethod block "#MoveSL2BE.SL2BEAddPips#" "key" /> pips to BE</#if>;
</#if>
