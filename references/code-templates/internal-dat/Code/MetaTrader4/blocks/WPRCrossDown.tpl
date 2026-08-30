((NormalizeDouble(iWPR(<@printInput block />, <@printParam block "#Period#" />, <@printShift block "1" />), 6) > <@printParam block "#Level#" />)
&&
(NormalizeDouble(iWPR(<@printInput block />, <@printParam block "#Period#" />, <@printShift block "0" />), 6) < <@printParam block "#Level#" />))