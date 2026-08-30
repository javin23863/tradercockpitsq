<#assign isFirst=true>
<@compress_single_line>
(
<#list block.* as block>
<#if !isFirst>__BR__|| <#else></#if> <@printBlock block />
<#assign isFirst=false>
</#list>
)
</@compress_single_line>


