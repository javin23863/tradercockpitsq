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

<#macro printATM block>
    <#local ATM = doc.StrategyFile.ATMs.ATM[0]>
        // ATM (Advanced Trade Management) is enabled, 
        // NOTE - original Profit target, Trailing, Exit after bars set in order are ignored!
        // Position size constraints - minimal exit size: ${ATM.@minimalSize}, size decimals:  ${ATM.@sizeDecimals}
    <@printExitMethodInit "StopLoss" block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
    
    <#local index = 1>
    <#list ATM.Exits.Exit as Exit>
        // Exit #${index} - ${Exit.ExitLevels.ExitLevel[0].@key}
        Close <@printATMExitCloseSize Exit /> <@printATMExitCloseType Exit block />

        <#local index = index + 1>
    </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printATMExitCloseSize Exit><@compress_single_line>
<#list Exit.PositionSizes.* as PositionSize>
    <#if PositionSize.@use == "true">
        <#switch PositionSize.@key>
            <#case "SizePercents">
            <@printParam PositionSize "Percents" /> % of position
            <#break>
        
            <#case "SizeFixedLots">
            <@printParam PositionSize "Lots" /> lots of position
            <#break>
            <#default>
            Full remaining position
        </#switch>
    </#if>
</#list>

</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATMExitCloseType Exit block><@compress_single_line>
    <#switch Exit.ExitLevels.ExitLevel[0].@key>
        <#case "MultipleOfOriginalSL">
        at <@printParam Exit.ExitLevels.ExitLevel[0] "Multiplicator" /> * original Stop Loss (<@printSLPTMethod block "MARKET" "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "#Symbol#" />);
        <#break>

        <#case "MultipleOfOriginalPT">
        at <@printParam Exit.ExitLevels.ExitLevel[0] "Multiplicator" /> * original Profit Target (<@printSLPTMethod block "MARKET" "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "#Symbol#" />);
        <#break>
        
        <#case "FixedProfit">
            <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
            at fixed profit <@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" /> pips; 
            <#else>
            at fixed profit <@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * ATR(<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" />);
            </#if>
        <#break>
        
        <#case "TrailingStop">
            <#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "Type") == "1">
            use trailing stop <@printParam Exit.ExitLevels.ExitLevel[0] "FixedPips" /> pips; 
            <#else>
            use trailing stop <@printParam Exit.ExitLevels.ExitLevel[0] "AtrMultiplicator" /> * ATR(<@printParam Exit.ExitLevels.ExitLevel[0] "AtrPeriod" />);
            </#if>
        <#break>
        
        <#case "BarsBased">
        exit after <@printParam Exit.ExitLevels.ExitLevel[0] "ExitAfterBars" /> bars;
        <#break>
        
        <#default>
        at exit rule<#if getBlockParamValue(Exit.ExitLevels.ExitLevel[0], "LimitExitAfterBars") == "true"> with limit - exit after maximum <@printParam Exit.ExitLevels.ExitLevel[0] "MaxBars" /> bars</#if>;
    </#switch>
        
</@compress_single_line></#macro>