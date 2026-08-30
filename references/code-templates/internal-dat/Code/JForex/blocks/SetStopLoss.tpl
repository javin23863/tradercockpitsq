      // Move SL to
      order = sqGetOrder(<@printMagicNumber block />, "<@printParam block "#Symbol#" />", <@printParam block "#Direction#" />, <@printComment block />);
	  
	  if(order!=null) {
<#if childWithKeyExists(block, "#StopLoss#") == true>
         sqOrderModifySL(order, <@printRangeLevelMethod block "#StopLoss#" "price" />, <@printRangeLevelMethodType block "#StopLoss#" "price" />);
<#else>
         sqOrderModifySL(order, <@printRangeLevelMethod block "#StopLoss.StopLoss#" "price" />, <@printRangeLevelMethodType block "#StopLoss.StopLoss#" "price" />);  
</#if>
      }