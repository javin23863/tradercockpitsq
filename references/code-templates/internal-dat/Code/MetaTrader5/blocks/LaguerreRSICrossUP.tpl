<@compress_single_line>
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "0" />) > <@printParam block "#Level#" />)
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printShift block "1" />) < <@printParam block "#Level#" />)
</@compress_single_line>
