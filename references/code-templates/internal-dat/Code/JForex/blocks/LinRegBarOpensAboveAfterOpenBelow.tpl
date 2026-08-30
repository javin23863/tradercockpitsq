<@compress_single_line>
(sqGetValue(<@printInput block true />, OPEN, <@printShift block "1" />) < sqLinReg(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />)
&&
sqGetValue(<@printInput block true />, OPEN, <@printShift block shift />) > sqLinReg(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />))
</@compress_single_line>