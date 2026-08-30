<@compress_single_line>
<#if hasZeroValue(block, "#Level#")>
(sqIsLowerThanZero(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "0" />)))
<#else>
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "0" />)
<
<@printParam block "#Level#" />)
</#if>
</@compress_single_line>