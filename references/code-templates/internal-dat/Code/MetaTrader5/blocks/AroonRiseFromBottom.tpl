(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "0" /> + 1, true) <= 8 && 
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "0" />, true) > 8 &&
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "0" />, true) > 30)
