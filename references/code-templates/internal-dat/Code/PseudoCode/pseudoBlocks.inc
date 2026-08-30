<#-------------------------------------------------------------->

<#macro printRulesForEvent eventName ruleName>
	<#if doc.StrategyFile.Strategy.@pickerEditor[0]?? && doc.StrategyFile.Strategy.@pickerEditor[0] == "true">
		<#local entryExitLong="">
		<#local entryExitShort="">
		<#local positionScoreLong="">
		<#local positionScoreShort="">

		<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
			<#if event.@key == eventName>
				<#list event.* as rule>
					<#if rule.@type == "StockpickerEntryExit">
						<#if rule.@name?contains("Long")>
							<#local entryExitLong=rule>
						<#else>
							<#local entryExitShort=rule>
						</#if>
					<#elseif rule.@type == "StockpickerPS">
						<#if rule.@name?contains("Long")>
							<#local positionScoreLong=rule>
						<#elseif rule.@name?contains("Short")>
							<#local positionScoreShort=rule>
						<#else>
							<#local positionScoreLong=rule>
							<#local positionScoreShort=rule>
						</#if>
					</#if>
				</#list>
			</#if>
		</#list>

		<@printRule entryExitLong ruleName />
		<@printRule positionScoreLong ruleName true />
		<@printGlobalMMMethod />


		<@printRule entryExitShort ruleName />
		<@printRule positionScoreShort ruleName false />
		<@printGlobalMMMethod />
	<#else>
		<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
			<#if event.@key == eventName>
				<#list event.* as rule>
					<@printRule rule ruleName />
				</#list>
			</#if>
		</#list>
	</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printRule rule ruleName isLong=true>
<@replaceBr>
<#if rule.@type != "StockpickerPS">
//--------------------------------------------------------------------
// ${ruleName} ${rule.@name} (<#if rule.@type[0]?? && rule.@type != "StockpickerEntryExit" && rule.@everyTick[0]?? && rule.@everyTick == "false">On Bar Open<#else>On Every Tick</#if>)
//--------------------------------------------------------------------           
</#if>        
<#if rule.@type == "IfThen" && isNonemptyRule(rule) == true >
if <#list rule.If.* as block><@printBlock block /></#list>
{
	<#local count=1>
	<#list rule.Then.* as block>
    // Action #${count}
	<@printBlock block />
	<#local count=count+1>
	</#list>
}
<#elseif rule.@type == "IfThenElse" && isNonemptyRule(rule) == true>
if <#list rule.If.* as block><@printBlock block /></#list>
{
	<#local count=1>
	<#list rule.Then.* as block>
    // Action #${count}
	<@printBlock block />
	<#local count=count+1>
	</#list>
} else {
	<#local count=1>
	<#list rule.Else.* as block>
    // Action #${count}
	<@printBlock block />
	<#local count=count+1>
	</#list>

}
<#elseif rule.@type == "ActionOnly" && isNonemptyRule(rule) == true>
	<#local count=1>
	<#list rule.Then.* as block>
    // Action #${count}
<@printBlock block />
	<#local count=count+1>

	</#list>
<#elseif rule.@type == "ForEachTrade" && isNonemptyRule(rule) == true>
if <#list rule.If.* as block><@printBlock block /></#list>
{
	<#local count=1>
	<#list rule.Then.* as block>
    // Action #${count}
	<@printBlock block />
	<#local count=count+1>
	</#list>
}
<#elseif rule.@type == "Signal">
   <#if isNonemptyRule(rule) == false >
      <#list rule.signals.* as signal>
<@printVariableName signal.@variable /> = false;
	    </#list>
   <#else>
      <#list rule.signals.* as signal>
<@printVariableName signal.@variable /> = <#if signal?children[0]??><@printBlock signal?children[0] /><#else>false</#if>;

      </#list>
   </#if>
<#elseif rule.@type == "SignalFuzzy"> 
   <#if isNonemptyRule(rule) == false >
      <#list rule.signals.* as signal>
<@printVariableName signal.@variable /> = false2;
	    </#list>
   <#else>
      <#list rule.signals.* as signal>
<@printVariableName signal.@variable /> = FUZZY Logic. Variable is true if min ${signal.@minTrue?number} % conditions (<@printFuzzySignalNumbers signal />) are correct:
        <#list signal.* as item>
        - <@printBlock item />
        </#list>
        
      </#list>
   </#if>
<#elseif rule.@type == "StockpickerEntryExit"> 
    <#assign hasEntryCond = false />
	<#list rule.* as block>
		<#if block?node_name == "Entry" && block.Item[0]??>
Entry Condition: <@printBlock block.Item />
            <#assign hasEntryCond = true />
		</#if>
		<#if block?node_name == "Exit">
			<#if block.Item[0]??>
Exit Condition: <@printBlock block.Item />
			<#else>
				<#if block.@atEndOfDay[0]?? && block.@atEndOfDay == "true">
Exit: At end of day (${block.@atEndOfDay})
				</#if>
			</#if>
		</#if>
		<#if block?node_name == "Order">
			<#if block.Item[0]??>
                <#if hasEntryCond>
Order: <@printBlock block.Item />
                <#else>
none
                </#if>
			</#if>
		</#if>
	</#list>
<#elseif rule.@type == "StockpickerPS" && (!doc.StrategyFile.Strategy.@singleAsset?has_content || doc.StrategyFile.Strategy.@singleAsset?contains("false"))> 
	<#list rule.Value.* as item>
		<#if item.Param[0].@key == "#PositionScore#">
          <#if item.Param[1].Formula.Block?children[0]??>
		  <#local positionScoreText><@printBlock item /></#local>
Position Score = ${positionScoreText?split("=")[1]}
          <#else>
Position Score = none
          </#if>
		<#else>
		<@compress_single_line>
Max positions: <#if isLong><@printTradingOptionIntValue "StockpickerOptions" "PickerMaxOpenPositionsLong" /><#else><@printTradingOptionIntValue "StockpickerOptions" "PickerMaxOpenPositionsShort"/></#if>
		</@compress_single_line>
		</#if>
	</#list>
</#if></@replaceBr>
</#macro>

<#-------------------------------------------------------------->

<#macro printFuzzySignalNumbers signal><@compress_single_line>
<#local minCount = (signal?children?size * (0.01 * signal.@minTrue?number))?round />
<#if (minCount > 0)>${minCount}<#else>1</#if> out of ${signal?children?size}
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printBlock block shift="0">
<#if block?node_name == "Block">
	<@printBlock block?children[0] shift/>
<#else>
   <#if block.@key=="#Value#" || block.@key=="#Left#" || block.@key=="#Right#">
   !! Undefined Value !!
   <#else>
       <#local blockKey = block.@key />
       <#if blockKey?starts_with("CDataIndy") >
         <#local blockKey = "CDataIndy" />
       <#elseif blockKey?starts_with("CBlock") >
         <#local blockKey = "CBlock" />
       </#if>
	   <#if blockFileExists(blockKey) >
         <#if isOnlineVersion() == "true" >
	       <#include "/PseudoCode/blocks/"+blockKey+".tpl">
         <#else>
   		   <#include "blocks/"+blockKey+".tpl">
         </#if>
	   <#else>
		   !!! Template file /blocks/${blockKey}.tpl missing for block '${blockKey} ' !!!
	   </#if>
   </#if>
</#if>
</#macro>


<#-------------------------------------------------------------->

<#function blockFileExists block>
	<#return true>
</#function>

<#-------------------------------------------------------------->

<#macro printBlock2 block>${block?node_name}</#macro>

<#-------------------------------------------------------------->

<#macro printBlockChild block childName shift="0"><@compress_single_line>
   <#local found = false />
   <#list block?children as c>
      <#if c?node_name == "Block" && c.@key == childName>
	     <@printBlock c shift/>
        <#local found = true />
   	</#if>
   </#list>
   <#if found == false>#Unknown#</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printGlobalMMMethod>
   <#assign mm=doc.StrategyFile.Strategy.MoneyManagement >
   <#if isOnlineVersion() == "true" >
   	<#include "/PseudoCode/MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+".tpl">
   <#else>
   	<#include "MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+".tpl">
   </#if>

Fractional shares: <@compress_single_line>
	<#if getParam(mm.Method.Params[0], "#AllowFractionalShares#")?? && getParam(mm.Method.Params[0], "#AllowFractionalShares#") == "true">
		Decimals: <@printParam mm.Method.Params "#FractionalDecimalNumbers#" />, Step: <@printParam mm.Method.Params "#FractionalStep#" />
	<#else>
	 	Not used
	</#if>
</@compress_single_line>
</#macro>