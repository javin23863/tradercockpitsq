<@compress_single_line>
(SQ_AdaptiveMovAvg_R(Close,<@printParam block "#FastERPeriod#" />,<@printParam block "#FastShortPeriod#" />,<@printParam block "#FastLongPeriod#" />, 1)<@printShift block "0" />)
>
(SQ_AdaptiveMovAvg_R(Close,<@printParam block "#SlowERPeriod#" />,<@printParam block "#SlowShortPeriod#" />,<@printParam block "#SlowLongPeriod#" />, 1)<@printShift block "0" />)
</@compress_single_line>
