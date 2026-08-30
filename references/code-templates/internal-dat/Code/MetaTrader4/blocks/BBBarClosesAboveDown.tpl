<@compress_single_line>
(sqGetValue(<@printInput block true />, PRICE_CLOSE, <@printShift block shift />) > sqBands(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 0, <@printComputedFromParam block />, 1, <@printShift block shift />))
</@compress_single_line>