        (
        /* trend calculation */
        sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />) < 0)
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />) < 0)
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />) < 0)
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "4" />) < 0)
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "5" />) < 0)
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "6" />) < 0)
        /* factor calculation */
        && ((sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />) - sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />))><@printParam block "#Factor#" />)
        && ((sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />) - sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "4" />))><@printParam block "#Factor#" />)        
        /* basket UP interpretation basketUP = (cci1<cci2) && (cci3>cci4) && (cci4<cci2) && (cci1<cci3) */
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />) < sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />))
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />) > sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "4" />))
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "4" />) < sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />))
        && (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />) < sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />))
        /* extremce confition area > 200 */
        &&
        ((sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "1" />) < -200)
        || (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />) < -200)
        || (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />) < -200)
        || (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "4" />) < -200)
        || (sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "5" />) < -200)
        
        ) /* (((cci3+cci2)/2)) */
        && sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "0" />) > ((sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "2" />)+sqCCI(<@printInput block />, <@printParam block "#Period#" />, <@printComputedFromParam block />, <@printShift block "3" />))/2)
        