<#if hasSetParam(block, "#ExitAfterBars.ExitAfterBars#") >
         // ExitAfterBars initialization
         sqSetGlobalVariable(_ticket, "ExitAfterBars", <@printParam block "#ExitAfterBars.ExitAfterBars#" "key" />);
</#if>