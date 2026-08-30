<@compress_single_line>
((sqHullMovingAverage(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />) > sqHullMovingAverage(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />))
&& 
(sqHullMovingAverage(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />) < sqHullMovingAverage(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "0" />)))
</@compress_single_line>
