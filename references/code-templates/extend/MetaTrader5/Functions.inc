<#-------------------------------------------------------------->

<#macro replaceBr>
<#local captured><#nested></#local>
${ captured?replace("__BR__", "\n      ") }
</#macro>