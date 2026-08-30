<@compress_single_line>
(sqOpen(<@printInput block true />, <@printShift block "1" />) > sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 2, <@printShift block "1" />, true) && sqOpen(<@printInput block true />, 0) < sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 2, <@printShift block "1" />, true))
</@compress_single_line>