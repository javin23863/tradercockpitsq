<@compress_single_line>
iCustom(<@printInput block />, "SqSchaffTrendCycle", <@printParam block "#StochPeriod#" />,<@printParam block "#FastPeriod#" />,<@printParam block "#SlowPeriod#" />,0, <@printShift block "1" />) < <@printParam block "#Level#" />
&&
iCustom(<@printInput block />, "SqSchaffTrendCycle", <@printParam block "#StochPeriod#" />,<@printParam block "#FastPeriod#" />,<@printParam block "#SlowPeriod#" />,0, <@printShift block "0" />) > <@printParam block "#Level#" />
</@compress_single_line>