((sqStdDev(<@printInput block />, <@printParam block "#Period#" />, 0, MODE_SMA, <@printComputedFromParam block />, <@printShift block "1" />) > <@printParam block "#Level#" />)
&&
(sqStdDev(<@printInput block />, <@printParam block "#Period#" />, 0, MODE_SMA, <@printComputedFromParam block />, <@printShift block "0" />) < <@printParam block "#Level#" />))