      // Move SL to
      if(sqSelectOrder(<@printMagicNumber block />, <@printSymbol block />, <@printParam block "#Direction#" />, <@printComment block />)) {
<#if childWithKeyExists(block, "#StopLoss#") == true>
         sqOrderModifySL(OrderTicket(), <@printRangeLevelMethod block "#StopLoss#" "price" />, <@printRangeLevelMethodType block "#StopLoss#" "price" />);
<#else>
         sqOrderModifySL(OrderTicket(), <@printRangeLevelMethod block "#StopLoss.StopLoss#" "price" />, <@printRangeLevelMethodType block "#StopLoss.StopLoss#" "price" />);  
</#if>
      }