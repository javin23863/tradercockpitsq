<@compress_single_line>
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "0" />) > sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "1" />))
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "2" />) > sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "1" />))
</@compress_single_line>