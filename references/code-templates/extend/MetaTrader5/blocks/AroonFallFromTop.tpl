(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "0" /> + 1, true) == 100 && 
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 1, <@printShift block "0" />, true) < 100 &&
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, 0, <@printShift block "0" />, true) < 70)