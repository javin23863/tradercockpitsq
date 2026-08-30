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
    MaxList(Round(
    <#switch PositionSize.@key>
        <#case "SizePercents">
            ${originalSize} / 100.0 * <@printParam PositionSize "Percents" />
            <#break>
        <#case "SizeFixedLots">
            <@printParam PositionSize "Lots" />
            <#break>
        <#default>
            RemainingSize
    </#switch>
    , <@printATMDecimals />), <@printATMMinSize />)
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMOrderPrice Exit isLongOrder><@compress_single_line>
    <#local ExitLevel = Exit.ExitLevels.ExitLevel[0] />

    <#switch ExitLevel.@key>
      <#case "MultipleOfOriginalSL">
        <#if isLongOrder>
            EntryPrice + (<@printParam ExitLevel "Multiplicator" /> * (EntryPrice - IntLongSL))
        <#else>
            EntryPrice - (<@printParam ExitLevel "Multiplicator" /> * (IntShortSL - EntryPrice))
        </#if> 
        <#break>

      <#case "MultipleOfOriginalPT">
        <#if isLongOrder>
            EntryPrice + (<@printParam ExitLevel "Multiplicator" /> * (IntPT - EntryPrice))
        <#else>
            EntryPrice - (<@printParam ExitLevel "Multiplicator" /> * (EntryPrice - IntPT))
        </#if>
        <#break>
      
      <#case "FixedProfit">
        <#if getBlockParamValue(ExitLevel, "Type") == "1">
            <#if isLongOrder>
                EntryPrice + SQ_ConvertToRealPips(<@printParam ExitLevel "FixedPips" />) 
            <#else>
                EntryPrice - SQ_ConvertToRealPips(<@printParam ExitLevel "FixedPips" />) 
            </#if>
        <#else>
            <#if isLongOrder>
                EntryPrice + <@printParam ExitLevel "AtrMultiplicator" /> * SQ_ATR_R(<@printParam ExitLevel "AtrPeriod" />, 1)[1]
            <#else>
                EntryPrice - <@printParam ExitLevel "AtrMultiplicator" /> * SQ_ATR_R(<@printParam ExitLevel "AtrPeriod" />, 1)[1]
            </#if>
        </#if>
        <#break>
      <#case "TrailingStop">
        <#if getBlockParamValue(ExitLevel, "Type") == "1">
            <#if isLongOrder>
                CurrentBid - SQ_ConvertToRealPips(<@printParam ExitLevel "FixedPips" />) 
            <#else>
                CurrentAsk + SQ_ConvertToRealPips(<@printParam ExitLevel "FixedPips" />) 
            </#if>
        <#else>
            <#if isLongOrder>
                CurrentBid - <@printParam ExitLevel "AtrMultiplicator" /> * SQ_ATR_R(<@printParam ExitLevel "AtrPeriod" />, 1)[1]
            <#else>
                CurrentAsk + <@printParam ExitLevel "AtrMultiplicator" /> * SQ_ATR_R(<@printParam ExitLevel "AtrPeriod" />, 1)[1]
            </#if>
        </#if>
        <#break>
      <#case "None">
        0
        <#break>
      <#default>
        !!! Unknown exit level type ${ExitLevel.@key} !!!
    </#switch>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMExits block>
  <#local isLongOrder><@printOrderDirection block "#Direction#" "true" "false" /></#local><#local isLongOrder = isLongOrder == "true"/>
  <#local orderSymbol>"<@printParam block "#Symbol#" />"</#local>
  <#local orderSize>NumberOfShares</#local>
  <#local decimals = doc.StrategyFile.ATMs.ATM[0].@sizeDecimals />
  <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
  <#list ATM.Exits.* as Exit>
    <#local exitLevelType = Exit.ExitLevels.ExitLevel[0].@key />

        //order with ${exitLevelType} exit

        If RemainingSize > 0 Then begin
            OrderSize = MinList(<@printATMOrderSize Exit orderSize />, RemainingSize);
            ATMExitPrice = <@printATMOrderPrice Exit isLongOrder />;

    <#switch exitLevelType>
        <#case "None">
            <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "LimitExitAfterBars") == "true">
            if (BarsSinceEntry(0) >= <@printParam Exit.ExitLevels.ExitLevel[0] "MaxBars" />) then
		            <@printOrderDirection block "#Direction#" "Sell" "BuyToCover" />("<@printOrderDirection block "#Direction#" "Long" "Short" />Exit${Exit?index + 1}-EAB") OrderSize shares next bar at market;
            </#if>
            <#break>
        <#default>
          <#if exitLevelType == "TrailingStop">
            IntPriceLevel = ATMExitPrice;
        <#if getOrderDirection(block, "#Direction#") = 1>
            If IntPriceLevel > 0 and IntPriceLevel - EntryPrice >= 0 and (IntLongTS = 0 or Round2Fraction(IntPriceLevel) > IntLongTS) and Round2Fraction(IntPriceLevel) < CurrentBid then begin
                IntLongTS = Round2Fraction(IntPriceLevel); 
            end;
            If IntLongTS > 0 and IntLongTS > IntLongSL then begin
                Sell("LongExit${Exit?index + 1}-TS") OrderSize shares next bar at IntLongTS stop;
                LongSLPlaced = true;
            end;		
        <#else>
            If IntPriceLevel > 0 and EntryPrice - IntPriceLevel >= 0 and (IntShortTS = 0 or Round2Fraction(IntPriceLevel) < IntShortTS) and Round2Fraction(IntPriceLevel) > CurrentAsk then begin
                IntShortTS = Round2Fraction(IntPriceLevel);
            end;   
            If IntShortTS > 0 and (IntShortTS < IntShortSL or IntShortSL = 0)  then begin
                BuyToCover("ShortExit${Exit?index + 1}-TS") OrderSize shares next bar at IntShortTS stop;
                ShortSLPlaced = true;
            end;		
        </#if>     
          <#else>
            <#local exitOrderName = "FP" />
            <#if exitLevelType == "MultipleOfOriginalSL"><#local exitOrderName = "MOSL"/>
            <#elseif exitLevelType == "MultipleOfOriginalPT"><#local exitOrderName = "MOPT"/>
            </#if>
            <@printOrderDirection block "#Direction#" "Sell" "BuyToCover" />("<@printOrderDirection block "#Direction#" "Long" "Short" />Exit${Exit?index + 1}-${exitOrderName}") OrderSize shares next bar at ATMExitPrice limit;
          </#if>
    </#switch>

            RemainingSize -= OrderSize;
        end;
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