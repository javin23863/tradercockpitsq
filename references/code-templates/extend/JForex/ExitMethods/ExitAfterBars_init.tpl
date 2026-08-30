<#if hasSetParam(block, "#ExitAfterBars.ExitAfterBars#") >
         // ExitAfterBars initialization
         sqSetGlobalVariable(ticket, "ExitAfterBars", <@printParam block "#ExitAfterBars.ExitAfterBars#" "key" />);
</#if>