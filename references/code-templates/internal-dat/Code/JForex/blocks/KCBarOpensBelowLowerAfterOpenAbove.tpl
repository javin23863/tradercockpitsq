<@compress_single_line>
(sqGetValue(<@printInput block true />, OPEN, <@printShift block "1" />) > sqKeltnerChannel(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 1, <@printShift block "1" />)
&&
sqGetValue(<@printInput block true />, OPEN, <@printShift block shift />) < sqKeltnerChannel(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 1, <@printShift block "1" />))
</@compress_single_line>