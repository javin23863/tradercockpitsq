
<#if hasSetParam(block, "#ExitAfterBars.ExitAfterBars#") >
         // ExitAfterBars initialization
         sqSetExitAfterXBars(_ticket, <@printParam block "#ExitAfterBars.ExitAfterBars#" "key" />);
</#if>