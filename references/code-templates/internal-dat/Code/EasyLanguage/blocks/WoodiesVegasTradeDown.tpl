(SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "1" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "4" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "5" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "6" /> > 0
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "4" /> - SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" /> > <@printParam block "#Factor#" />
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "1" /> - SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" /> > <@printParam block "#Factor#" />
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "1" /> > SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" />
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "1" /> > SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" />
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "4" /> > SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" />
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "4" /> > SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" />
__NBSP1__and__NBSP1__
(
SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "1" /> > 200
 or SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" /> > 200
 or SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" /> > 200
 or SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "4" /> > 200
 or SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "5" /> > 200
) 
__NBSP1__and__NBSP1__SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "0" /> < (SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "3" /> + SQ_CCI_R(<@printComputedFromParam block /><@printInput block />, <@printParam block "#Period#" />, 1)<@printShift block "2" />) / 2)