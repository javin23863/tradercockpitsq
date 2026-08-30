      // Move PT to
	  order = sqGetOrder(<@printMagicNumber block />, "<@printParam block "#Symbol#" />", <@printParam block "#Direction#" />, <@printComment block />);

      if(order!=null) {
<#if childWithKeyExists(block, "#ProfitTarget#") == true>
         sqOrderModifyPT(order, <@printRangeLevelMethod block "#ProfitTarget#" "price" />, <@printRangeLevelMethodType block "#ProfitTarget#" "price" />);
<#else>
         sqOrderModifyPT(order, <@printRangeLevelMethod block "#ProfitTarget.ProfitTarget#" "price" />, <@printRangeLevelMethodType block "#ProfitTarget.ProfitTarget#" "price" />);
</#if>
      }