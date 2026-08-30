      // Move PT to
      if(sqSelectOrder(<@printMagicNumber block />, <@printSymbol block />, <@printParam block "#Direction#" />, <@printComment block />)) {
<#if childWithKeyExists(block, "#ProfitTarget#") == true>
         sqOrderModifyPT(OrderTicket(), <@printRangeLevelMethod block "#ProfitTarget#" "price" />, <@printRangeLevelMethodType block "#ProfitTarget#" "price" />);
<#else>
         sqOrderModifyPT(OrderTicket(), <@printRangeLevelMethod block "#ProfitTarget.ProfitTarget#" "price" />, <@printRangeLevelMethodType block "#ProfitTarget.ProfitTarget#" "price" />);
</#if>
      }