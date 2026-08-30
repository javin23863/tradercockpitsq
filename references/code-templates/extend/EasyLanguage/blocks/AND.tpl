<@compress_single_line>
<#assign isFirst=true>
(
<#list block.* as block>
<#if !isFirst>__BR__and__NBSP1__<#else></#if> <@printBlock block />
<#assign isFirst=false>
</#list>
)
</@compress_single_line>