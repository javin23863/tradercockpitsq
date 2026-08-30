<@compress_single_line>
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "0" />) < <@printParam block "#Level#" />)
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "1" />) > <@printParam block "#Level#" />)
</@compress_single_line>