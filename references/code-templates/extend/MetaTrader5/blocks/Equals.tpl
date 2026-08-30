<#if isBooleanBlock(block)>
(<@printBlockChild block "#Left#" "0" true /> == <@printBlockChild block "#Right#" "0" true />)
<#else>
(NormalizeDouble((double) <@printBlockChild block "#Left#" "0" true />, 6) == NormalizeDouble((double) <@printBlockChild block "#Right#" "0" true />, 6))
</#if>
