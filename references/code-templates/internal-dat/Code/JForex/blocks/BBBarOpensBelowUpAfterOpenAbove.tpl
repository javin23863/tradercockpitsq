<@compress_single_line>
((sqGetValue(<@printInput block true />, OPEN, <@printShift block "1" />) > sqBands(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 0, <@printComputedFromParam block />, 0, <@printShift block "1" />))
 && 
(sqGetValue(<@printInput block true />, OPEN, <@printShift block shift />) < sqBands(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 0, <@printComputedFromParam block />, 0, <@printShift block "1" />))
)
</@compress_single_line>