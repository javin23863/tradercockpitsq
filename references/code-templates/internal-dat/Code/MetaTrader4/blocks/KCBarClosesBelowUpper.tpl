<@compress_single_line>
(sqGetValue(<@printInput block true />, PRICE_CLOSE, <@printShift block shift />) < sqKeltnerChannel(<@printInput block />, <@printParam block "#Period#" />, <@printParam block "#Deviation#" />, 0, <@printShift block shift />))
</@compress_single_line>