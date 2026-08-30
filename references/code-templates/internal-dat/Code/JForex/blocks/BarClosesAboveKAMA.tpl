(sqGetValue(<@printInput block true />, CLOSE, <@printShift block "0" />)> sqKama(<@printInput block />,<@printParam block "#ERPeriod#" />, <@printParam block "#ShortPeriod#" />, <@printParam block "#LongPeriod#" />, <@printShift block "0" />)
    &&
    sqGetValue(<@printInput block true />, CLOSE, <@printShift block "1" />)< sqKama(<@printInput block />,<@printParam block "#ERPeriod#" />, <@printParam block "#ShortPeriod#" />, <@printParam block "#LongPeriod#" />, <@printShift block "1" />)
)