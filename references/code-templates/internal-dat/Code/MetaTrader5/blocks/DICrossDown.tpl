((sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "1" />) > sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 2, <@printShift block "1" />))
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "0" />) < sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 2, <@printShift block "0" />)))