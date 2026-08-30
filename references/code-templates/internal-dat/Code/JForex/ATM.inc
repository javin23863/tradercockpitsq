<#-------------------------------------------------------------->

<#function usesATM >
   <#if doc.StrategyFile.ATMs[0]?? && doc.StrategyFile.ATMs.@enable == "true">
      <#if doc.StrategyFile.ATMs.ATM[0]?? && doc.StrategyFile.ATMs.ATM[0].Exits[0]?? && doc.StrategyFile.ATMs.ATM[0].Exits.Exit[0]?? >
          <#return true />
      <#else>
          <#return false />
      </#if>
   <#else>
     <#return false />
   </#if>
</#function>

<#-------------------------------------------------------------->

<#macro printATMDecimals><@compress_single_line>
   <#if doc.StrategyFile.ATMs[0]?? && doc.StrategyFile.ATMs.ATM[0]??>
      ${doc.StrategyFile.ATMs.ATM[0].@sizeDecimals}
   <#else>
      0
   </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMMinSize ><@compress_single_line>
   <#if doc.StrategyFile.ATMs[0]?? && doc.StrategyFile.ATMs.ATM[0]??>
      ${doc.StrategyFile.ATMs.ATM[0].@minimalSize}
   <#else>
      0
   </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMOrderSize Exit originalSize ><@compress_single_line>
    <#local PositionSize = Exit.PositionSizes.PositionSize[0] />

	Math.max(round(
    <#switch PositionSize.@key>
        <#case "SizePercents">
            ${originalSize} / 100.0 * <@printParam PositionSize "Percents" />
            <#break>
        <#case "SizeFixedLots">
            <@printParam PositionSize "Lots" />
            <#break>
        <#default>
            sizeRemaining
    </#switch>
	, <@printATMDecimals />), <@printATMMinSize />)
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMOrderPrice Exit originalOrderType symbol><@compress_single_line>
    <#local isLongOrder = originalOrderType?starts_with("OP_BUY") />
    <#local ExitLevel = Exit.ExitLevels.ExitLevel[0] />

    <#switch ExitLevel.@key>
      <#case "MultipleOfOriginalSL">
        <#if isLongOrder>
            openPrice + (<@printParam ExitLevel "Multiplicator" /> * (openPrice - sl))
        <#else>
            openPrice - (<@printParam ExitLevel "Multiplicator" /> * (sl - openPrice))
        </#if> 
        <#break>

      <#case "MultipleOfOriginalPT">
        <#if isLongOrder>
            openPrice + (<@printParam ExitLevel "Multiplicator" /> * (pt - openPrice))
        <#else>
            openPrice - (<@printParam ExitLevel "Multiplicator" /> * (openPrice - pt))
        </#if>
        <#break>
      
      <#case "FixedProfit">
        <#if getBlockParamValue(ExitLevel, "Type") == "1">
            <#if isLongOrder>
                (openPrice == 0 ? sqGetAsk(${symbol}) : openPrice) + sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            <#else>
                (openPrice == 0 ? sqGetBid(${symbol}) : openPrice) - sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            </#if>
        <#else>
            <#if isLongOrder>
                (openPrice == 0 ? sqGetAsk(${symbol}) : openPrice) + <@printParam ExitLevel "AtrMultiplicator" /> * sqATR("Current", 0, <@printParam ExitLevel "AtrPeriod" />, 1)
            <#else>
                (openPrice == 0 ? sqGetBid(${symbol}) : openPrice) - <@printParam ExitLevel "AtrMultiplicator" /> * sqATR("Current", 0, <@printParam ExitLevel "AtrPeriod" />, 1)
            </#if>
        </#if>
        <#break>
      <#case "TrailingStop">
        <#if getBlockParamValue(ExitLevel, "Type") == "1">
            <#if isLongOrder>
                openPrice + sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            <#else>
                openPrice - sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            </#if>
        <#else>
			<#if isLongOrder>
                openPrice + <@printParam ExitLevel "AtrMultiplicator" /> * sqATR("Current", 0, <@printParam ExitLevel "AtrPeriod" />, 1)
            <#else>
                openPrice - <@printParam ExitLevel "AtrMultiplicator" /> * sqATR("Current", 0, <@printParam ExitLevel "AtrPeriod" />, 1)
            </#if>
        </#if>
        <#break>
    
      <#default>
        !!! Unknown exit level type ${ExitLevel.@key} !!!
    </#switch>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printHedgingATMOrders block orderFillType>
  <#local orderType><@printOrderType block orderFillType "#Direction#" /></#local>
  <#local orderSymbol>"<@printParam block "#Symbol#" />"</#local>
  <#local orderSize><@printSizeMethod block orderFillType "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" "#Symbol#" /></#local>
  <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
  <#list ATM.Exits.* as Exit>
    <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />
      //order with ${exitLevelType} exit

      if(sizeRemaining > 0){
          <#if (Exit?index > 0)>if(openingAllowed){</#if>
          orderSize = Math.min(fixLotSize(${orderSymbol}, <@printATMOrderSize Exit orderSize />), sizeRemaining);

          order = sqOpenOrder(
              <@printOrderType block orderFillType "#Direction#" />, 
              ${orderSymbol}, 
              orderSize, 
              openPrice, 
              <@printMagicNumber block />, 
              <@printComment block />, 
              0, 
              <#if orderFillType == "MARKET">false<#else><@printParam block "#ReplaceExisting#" /></#if>, 
              <@printParam block "#AllowDuplicateTrades#" />, 
              null, 
              <#if (Exit?index = 0)>false<#else>true</#if>
          );
      
		if(order != null) {
			ticket = order.getLabel();
			
              <#if Exit?index = 0>openingAllowed = true;</#if>
              // set or initialize all order exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
              <#if hasSetParam(block, "#BarsValid#") >
              sqSetOrderExpiration(ticket, <@printParam block "#BarsValid#" />);
              </#if>
              sqSetSLandPT(order, sl, <@printATMProfitTarget Exit orderType, orderSymbol />);

      <#switch exitLevelType>
          <#case "BarsBased">
              //BarsBased exit
              sqSetGlobalVariable(ticket, "ExitAfterBars", <@printParam Exit.ExitLevels.ExitLevel[0] "ExitAfterBars" />);
              <#break>
          <#case "None">
              <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "LimitExitAfterBars") == "true">
              sqSetGlobalVariable(ticket, "ExitAfterBars", <@printParam Exit.ExitLevels.ExitLevel[0] "MaxBars" />);
              </#if>
              <#break>
          <#default>
            <#if exitLevelType == "TrailingStop">
              <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
              sqSetGlobalVariable(ticket, "TrailingStop", sqStringHash("<@fix_string><@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" /></@fix_string>"));
              sqSetGlobalVariable(ticket, "TrailingStopType", SLPTTYPE_RANGE);
              <#else>
              sqSetGlobalVariable(ticket, "TrailingStop", sqStringHash("<@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * sqATR(OrderSymbol(),0,<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" />, 1)"));
              sqSetGlobalVariable(ticket, "TrailingStopType", SLPTTYPE_RANGE);
              </#if>
            </#if>
      </#switch>

          }

          sizeRemaining -= orderSize;
          <#if (Exit?index > 0)>}</#if>
      }
  </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printATMProfitTarget Exit orderType orderSymbol><@compress_single_line>
  <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />
  <#switch exitLevelType>
    <#case "BarsBased">
    <#case "None">
    <#case "TrailingStop">
      0
      <#break>
    <#default>
      <@printATMOrderPrice Exit orderType orderSymbol /> 
  </#switch>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMExitValues>
  <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
  <#list ATM.Exits.* as Exit>
      <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />
      <#if exitLevelType == "TrailingStop">
          <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
              <#local hash><@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" /></#local> 
              <#if hash != "0">
    if(idHash == (string) sqStringHash("${hash}")) {
      return sqConvertToRealPips(instrument, ${hash});
    }
              </#if>
          <#else>
              <#local hash><@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * sqATR(null,0,<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" />, 1)</#local> 
    if(idHash == sqStringHash("${hash}")) {
      return ${hash};
    }
          </#if>
      </#if>
  </#list>
</#macro>

<#-------------------------------------------------------------->

<#function getBlockParamValue block paramName>
<#list block.* as param>
	<#if param.@key == paramName><#return getParameterValue(param) ></#if>
</#list>
<#return "" >
</#function>

<#-------------------------------------------------------------->

<#function getParameterValue param>
<#if param.@variable[0]?? && param.@variable == "true">
   <#return getVariableValue(param) >
<#elseif param.Formula[0]?? >
   NOT IMPLEMENTED!
<#else>
   <#return param >
</#if>
</#function>

<#-------------------------------------------------------------->

<#function getVariableValue variableName>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName><#return var.value ></#if>
	</#list>
   <#return null >
</#function>

<#-------------------------------------------------------------->

<#function getOrderType block type>
<#local orderType><@printOrderType block type "#Direction#" /></#local>
<#return orderType>
</#function>