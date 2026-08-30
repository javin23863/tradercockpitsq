
<#-------------------------------------------------------------->

<#macro printRulesForEvent eventName>
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
  		<#if event.@key == eventName>
            <#list event.* as rule>
<@printRule rule />
                
			</#list>
  		</#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printRule rule>
<@replaceBr>
<#if rule.@type == "IfThen" && isNonemptyRule(rule) == true>
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
<@conditionsFixer>if <#list rule.If.* as block><@printBlock block /></#list></@conditionsFixer>
then begin
	<#local count=1>
	<#list rule.Then.* as block>
	// Action #${count}
<@printBlock block />
   <#local count=count+1>

	</#list>
end;

<@printTradeExits rule.@name> 
   <#local count=1>
	<#list rule.Then.* as block>
      <#if block.@returnType[0]?? && block.@returnType[0] == "order">
	      <@printTradeExit block />
	   </#if>
	   <#local count=count+1>
	</#list></@printTradeExits>
<#elseif rule.@type == "IfThenElse" && isNonemptyRule(rule) == true>
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
<@conditionsFixer>if <#list rule.If.* as block><@printBlock block /></#list></@conditionsFixer>
then begin
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	   <@printBlock block />
	   <#local count=count+1>


	</#list>
   end else begin
	<#local count=1>
	<#list rule.Else.* as block>
   // Action #${count}
	 <@printBlock block />
	 <#local count=count+1>


	</#list>

end;
<#elseif rule.@type == "ActionOnly" && isNonemptyRule(rule) == true>
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
      <@printBlock block />
	  <#local count=count+1>


	</#list>
<#elseif rule.@type == "ForEachTrade" && isNonemptyRule(rule) == true>
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
   if <#list rule.If.* as block><@printBlock block /></#list>
   then begin
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	  <@printBlock block />
	  <#local count=count+1>


	</#list>
   end;
<#elseif rule.@type == "Signal">                            
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
	<#list rule.signals.* as signal>
<@conditionsFixer><@printVariableName signal.@variable /> = <#if signal?children[0]?? ><@printBlock signal?children[0] /><#else>false</#if>;</@conditionsFixer>

	</#list>

<#elseif rule.@type == "SignalFuzzy">
//------------------------------------------------------------------
// Rule: ${rule.@name}
//------------------------------------------------------------------
  <#list rule.signals.* as signal>
<@printFuzzyVariable signal "" />
	</#list>
</#if>
</@replaceBr>
</#macro>

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
         <#if block.@customIndicator[0]??>
            <@printCustomIndicator block shift />
         <#else>
            <#if isOnlineVersion() == "true" >
               <#include "/EasyLanguage/blocks/"+blockKey+".tpl">
            <#else>
               <#if blockKey??>
                  <#include "blocks/"+blockKey+".tpl">
               </#if>
            </#if>
         </#if>
	   <#else>
		   !!! Template file /blocks/${blockKey}.tpl missing for block '${blockKey} ' !!!
	   </#if>
   </#if>
</#if>
</#macro>


<#-------------------------------------------------------------->

<#macro printTradeExit block shift="0">
    <#if isOnlineVersion() == "true" >
        <#include "/EasyLanguage/blocks/TradeExit.tpl">
    <#else>
        <#include "blocks/TradeExit.tpl">
    </#if>
</#macro>

<#-------------------------------------------------------------->

<#function blockFileExists block>
	<#return true>
</#function>

<#-------------------------------------------------------------->

<#macro printBlockChild block childName shift="0"><@compress_single_line>
   <#list block?children as c>
      <#if c?node_name == "Block" && c.@key == childName>
	     <@printBlock c shift/>
   	</#if>
   </#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFuzzyVariable signal space>
${space}// FUZZY Logic. Variable is true if min ${signal.@minTrue?number} % conditions (<@printFuzzySignalNumbers signal />) are correct
<#local index = 0 /> 
<@conditionsFixer>
<#list signal.* as item>
${space}cond[${index}] = <@printBlock item />;
    <#local index = index+1 />
  </#list>

${space}<@printVariableName signal.@variable /> = SQ_EvaluateFuzzySignal(cond, <@printFuzzySignalMethodNumbers signal />);</@conditionsFixer>
</#macro>

<#-------------------------------------------------------------->

<#macro printFuzzySignalNumbers signal><@compress_single_line>
<#local minCount = (signal?children?size * (0.01 * signal.@minTrue?number))?round />
<#if (minCount > 0)>${minCount}<#else>1</#if> out of ${signal?children?size}
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFuzzySignalMethodNumbers signal><@compress_single_line>
${signal?children?size}, ${(signal?children?size * (0.01 * signal.@minTrue?number))?round}
</@compress_single_line></#macro>








