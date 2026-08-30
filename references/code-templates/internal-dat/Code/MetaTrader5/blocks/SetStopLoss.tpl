      // Move SL 
      if(sqSelectPosition(<@printMagicNumber block />, <@printSymbol block />, <@printParam block "#Direction#" />, <@printComment block />)) {
<#if childWithKeyExists(block, "#StopLoss#") == true>
         sqSetSLPT(PositionGetInteger(POSITION_TICKET), sqFixMarketPrice(<@printRangeLevelMethod block "#StopLoss#" "price" />, PositionGetString(POSITION_SYMBOL)), 0);
<#else>
         sqSetSLPT(PositionGetInteger(POSITION_TICKET), sqFixMarketPrice(<@printRangeLevelMethod block "#StopLoss.StopLoss#" "price" />, PositionGetString(POSITION_SYMBOL)), 0);
</#if> 
      }