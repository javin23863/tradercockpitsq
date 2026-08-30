<#-------------------------------------------------------------->

<#macro replaceBr>
<#local captured><#nested></#local>
${ captured?replace("__BR__", "\n    ")?replace("__NBSP__", "    ")?replace("__NBSP1__", " ") }
</#macro>

<#-------------------------------------------------------------->

<#macro printTradeExits ruleName>
<#local captured><#nested></#local>
<#if captured?trim?replace("if(MarketPosition > 0) then begin", "")?replace("if(MarketPosition < 0) then begin", "")?replace("end;", "")?replace("^\\s+|\\s+$|\\n|\\r", "", "rm") != "">
//------------------------
// Orders Exits (SL, PT, Trailing) for Rule: ${ruleName} 
//------------------------
${captured?trim}
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro removeShift>
<#local captured><#nested></#local>
${captured?keep_before("[") }
</#macro>

<#-------------------------------------------------------------->

<#function isEntryOrder block >
	<#if block.@key?contains("Enter") >
		<#return true />
	<#else>
		<#return false />
	</#if>	
</#function>

<#-------------------------------------------------------------->

<#macro elPrintCBlock block sourcecode shift><@compress_single_line>
  <#local cblockId = block.@key[0]?substring(7) />
  <@printBlock block.Contents?children[0] />
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro elPrintCBlockName block><@compress_single_line>
  <#local cblockId = block.@name[0] />
  ${cblockId}
</@compress_single_line></#macro>