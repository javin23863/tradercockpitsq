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

<#macro printNettingATMExits block orderFillType>
  <#local orderType><@printOrderType block orderFillType "#Direction#" /></#local>
  <#local orderSymbol><@printSymbol block /></#local>
  <#local orderSize><@printSizeMethod block orderFillType "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" /></#local>
  <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
  <#list ATM.Exits.* as Exit>
    <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />
            //${exitLevelType} exit

            if(sizeRemaining > 0){
                exitSize = MathMin(fixLotSize(${orderSymbol}, <@printATMOrderSize Exit orderSize />), sizeRemaining);
        <#switch exitLevelType>
            <#case "BarsBased">
                addOrderExit(${orderSymbol}, MagicNumber, _ticket, 0, ATM_EXIT_TYPE_TIME, <@printParam Exit.ExitLevels.ExitLevel[0] "ExitAfterBars" />, exitSize, -1, -1, 0);
                <#break>
            <#case "None">
                <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "LimitExitAfterBars") == "true">
                addOrderExit(${orderSymbol}, MagicNumber, _ticket, 0, ATM_EXIT_TYPE_TIME, <@printParam Exit.ExitLevels.ExitLevel[0] "MaxBars" />, exitSize, -1, -1, 0);
                </#if>
                <#break>
            <#default>
              <#if exitLevelType == "TrailingStop">
                <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
                addOrderExit(${orderSymbol}, MagicNumber, _ticket, 0, ATM_EXIT_TYPE_TRAILING, -1, exitSize, <@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" />, -1, 0);
                <#else>
                <#local atrID>ATR_NULL_0_<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" /></#local>
                addOrderExit(${orderSymbol}, MagicNumber, _ticket, 0, ATM_EXIT_TYPE_TRAILING, -1, exitSize, -1, <@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" />, ${getIndyShortName(atrID)});
                </#if>
              <#else>
                exitTicket = openPosition(
                    <@printATMOrderType orderType />, // Order type
                    ${orderSymbol}, // Symbol
                    exitSize,  //Size
                    sqFixMarketPrice(<@printATMOrderPrice Exit orderType orderSymbol />, ${orderSymbol}), // Price
                    0, // Stop Loss
                    0, // Profit Target
                    correctSlippage(sqMaxEntrySlippage, "Current"), // Max deviation          
                    <@printComment block />, // Comment
                    <@printMagicNumber block />, // MagicNumber
                    ExpirationTime, // Expiration time
                    <#if orderFillType == "MARKET">false<#else><@printParam block "#ReplaceExisting#" /></#if>, // Replace existing order (if it exists)
                    <@printParam block "#AllowDuplicateTrades#" />,  // Allow duplicate trades
                    true
                );
                
                if(exitTicket != 0) {
                    addOrderExit(${orderSymbol}, MagicNumber, _ticket, exitTicket, ATM_EXIT_TYPE_LEVEL, -1, exitSize, -1, -1, 0);
                }
              </#if>
        </#switch>
        
                sizeRemaining -= exitSize;
            }
  </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printATMOrderType originalOrderType><@compress_single_line>
<#if originalOrderType?starts_with("ORDER_TYPE_BUY")>
  ORDER_TYPE_SELL_LIMIT
<#else>
  ORDER_TYPE_BUY_LIMIT
</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMOrderSize Exit originalSize ><@compress_single_line>
    <#local PositionSize = Exit.PositionSizes.PositionSize[0] />
    MathMax(NormalizeDouble(
    <#switch PositionSize.@key>
        <#case "SizePercents">
            NormalizeDouble(${originalSize} / 100.0 * <@printParam PositionSize "Percents" />, 2)
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
    <#local isLongOrder = originalOrderType?starts_with("ORDER_TYPE_BUY") />
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
            <#local atrID>ATR_NULL_0_<@printParam ExitLevel "AtrPeriod" /></#local>
            <#if isLongOrder>
                (openPrice == 0 ? sqGetAsk(${symbol}) : openPrice) + <@printParam ExitLevel "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)
            <#else>
                (openPrice == 0 ? sqGetBid(${symbol}) : openPrice) - <@printParam ExitLevel "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)
            </#if>
        </#if>
        <#break>
      <#case "TrailingStop">
        <#if getBlockParamValue(ExitLevel, "Type") == "1">
            <#if isLongOrder>
                sqGetBid(${symbol}) - sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            <#else>
                sqGetAsk(${symbol}) + sqConvertToRealPips(${symbol}, <@printParam ExitLevel "FixedPips" />) 
            </#if>
        <#else>
            <#local atrID>ATR_NULL_0_<@printParam ExitLevel "AtrPeriod" /></#local>
            <#if isLongOrder>
                sqGetBid(${symbol}) - <@printParam ExitLevel "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)
            <#else>
                sqGetAsk(${symbol}) + <@printParam ExitLevel "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)
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
  <#local orderSymbol><@printSymbol block /></#local>
  <#local orderSize><@printSizeMethod block orderFillType "#Size#" "#Direction#" "#Price#" "#StopLoss.StopLoss#" /></#local>
  <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
  <#list ATM.Exits.* as Exit>
    <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />
        //order with ${exitLevelType} exit

        if(sizeRemaining > 0){
            <#if (Exit?index > 0)>if(openingAllowed){</#if>
            orderSize = MathMin(fixLotSize(${orderSymbol}, <@printATMOrderSize Exit orderSize />), sizeRemaining);

            _ticket = openPosition(
                <@printOrderType block orderFillType "#Direction#" />, // Order type
                ${orderSymbol}, // Symbol
                orderSize,  //Size
                openPrice, // Price
                sl, // Stop Loss
                <@printATMProfitTarget Exit orderType, orderSymbol />, // Profit Target
                correctSlippage(sqMaxEntrySlippage, "Current"), // Max deviation          
                <@printComment block />, // Comment
                <@printMagicNumber block />, // MagicNumber
                ExpirationTime, // Expiration time
                <#if orderFillType == "MARKET">false<#else><@printParam block "#ReplaceExisting#" /></#if>, // Replace existing order (if it exists)
                <@printParam block "#AllowDuplicateTrades#" />,  // Allow duplicate trades
                <#if (Exit?index = 0)>false<#else>true</#if>
            );
            
            if(_ticket != 0) {
                <#if Exit?index = 0>openingAllowed = true;</#if>
                // order was successfuly placed, set or initialize all its exit methods (SL, PT, Trailing Stop, Exit After Bars, etc.)
                <#if hasSetParam(block, "#BarsValid#") >
                sqSetOrderExpiration(_ticket, <@printParam block "#BarsValid#" />);
                </#if>

        <#switch exitLevelType>
            <#case "BarsBased">
                //BarsBased exit
                sqSetExitAfterXBars(_ticket, <@printParam Exit.ExitLevels.ExitLevel[0] "ExitAfterBars" />);
                <#break>
            <#case "None">
                <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "LimitExitAfterBars") == "true">
                sqSetExitAfterXBars(_ticket, <@printParam Exit.ExitLevels.ExitLevel[0] "MaxBars" />);
                </#if>
                <#break>
            <#default>
              <#if exitLevelType == "TrailingStop">
                <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
                <#local expression><@escape_string><@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" /></@escape_string></#local>
                sqSetTrailingStop(_ticket, "${expression}", SLPTTYPE_RANGE);
                <#else>
                <#local atrID>ATR_NULL_0_<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" /></#local>
                <#local expression><@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)</#local>
                sqSetTrailingStop(_ticket, "${expression}", SLPTTYPE_RANGE);
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
      sqFixMarketPrice(<@printATMOrderPrice Exit orderType orderSymbol />, ${orderSymbol})  
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
    if(idHash == sqStringHash("${hash}")) {
      return sqConvertToRealPips(PositionGetString(POSITION_SYMBOL), ${hash});
    }
              </#if>
          <#else>
              <#local atrID>ATR_NULL_0_<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" /></#local>
              <#local hash><@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * sqGetIndicatorValue(${getIndyShortName(atrID)}, 1)</#local> 
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
