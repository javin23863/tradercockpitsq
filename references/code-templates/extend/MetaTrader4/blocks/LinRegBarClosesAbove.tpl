<@compress_single_line>
(sqGetValue(<@printInput block true />, PRICE_CLOSE, <@printShift block shift />) > sqLinReg(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#ComputedFrom#" />, <@printShift block shift />))
</@compress_single_line>