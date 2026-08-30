      // Move PT to
      if(sqSelectPosition(<@printMagicNumber block />, <@printSymbol block />, <@printParam block "#Direction#" />, <@printComment block />)) { 
<#if childWithKeyExists(block, "#ProfitTarget#") == true>
         sqSetSLPT(PositionGetInteger(POSITION_TICKET), 0, sqFixMarketPrice(<@printRangeLevelMethod block "#ProfitTarget#" "price" />, PositionGetString(POSITION_SYMBOL)));
<#else>
         sqSetSLPT(PositionGetInteger(POSITION_TICKET), 0, sqFixMarketPrice(<@printRangeLevelMethod block "#ProfitTarget.ProfitTarget#" "price" />, PositionGetString(POSITION_SYMBOL)));
</#if>      
      }