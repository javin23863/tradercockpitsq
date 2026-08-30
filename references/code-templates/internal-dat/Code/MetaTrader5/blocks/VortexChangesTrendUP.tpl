(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "0" />) > sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "0" />))
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "1" />) < sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "1" />))