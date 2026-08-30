<@compress_single_line>
(sqGetValue(<@printInput block true />, PRICE_OPEN, <@printShift block "1" />) < sqLinReg(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#ComputedFrom#" />, <@printShift block "1" />)
&&
sqGetValue(<@printInput block true />, PRICE_OPEN, <@printShift block shift />) > sqLinReg(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#ComputedFrom#" />, <@printShift block "1" />))
</@compress_single_line>