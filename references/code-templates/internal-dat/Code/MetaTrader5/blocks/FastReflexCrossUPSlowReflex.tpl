<@compress_single_line>
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block,["#FastPeriod#"],"iCustom","SqReflex"))},<@printShift block "0" />)
>
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block,["#SlowPeriod#"],"iCustom","SqReflex"))},<@printShift block "0" />))
&&
(sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block,["#FastPeriod#"],"iCustom","SqReflex"))},<@printShift block "1" />)
<
sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block,["#SlowPeriod#"],"iCustom","SqReflex"))},<@printShift block "1" />))
</@compress_single_line>