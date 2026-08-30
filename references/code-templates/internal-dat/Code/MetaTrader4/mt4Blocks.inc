

<#macro printIndicatorsUsedInFunctions>
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
      <#list event.* as rule>
         <#if isNonemptyRule(rule) == true >
<@findIndicatorsUsedInFunctions rule />
	     	</#if>
		</#list>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro findIndicatorsUsedInFunctions block>
   <#list block?children as c>
      <#if c?node_type == 'element' && (c?node_name == "Block" || c?node_name == "Item") && c.@key[0]?? && c.@key?starts_with("#Indicator") >
   if(idHash == "<@fix_string><@compress_single_line><@printBlock c /></@compress_single_line></@fix_string>") {
      return (<@compress_single_line><@printBlock c "shift" /></@compress_single_line>);
   }
      <#else>
         <@findIndicatorsUsedInFunctions c />
      </#if>
   </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printValuesUsedInExitMethods >
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
      <#list event.* as rule>
         <#if isNonemptyRule(rule) == true >
<@findValuesUsedInExitMethods rule />
	     	</#if>
		</#list>
	</#list>
  <#if usesATM()>
<@printATMExitValues />
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro findValuesUsedInExitMethods block >
   <#list block?children as c>
      <#if c?node_type == 'element' && c?node_name == "Param">
         <#if !c.@key?contains(".None") && c.Formula[0]?? && c.@exitMethod[0]??>

   if(idHash == (string) sqStringHash("<@compress_single_line><@fix_string><@printRangeLevelMethod block c.@key "key" /></@fix_string></@compress_single_line>")) {
      return (<@compress_single_line><@printRangeLevelMethod block c.@key "price" /></@compress_single_line>);
   }
         </#if>
      <#else>
         <@findValuesUsedInExitMethods c />
      </#if>
   </#list>
</#macro>


<#-------------------------------------------------------------->

<#macro printRulesForEvent eventName>
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
  		<#if event.@key == eventName>
            <#list event.* as rule>
<@printRule rule eventName/>
                
			</#list>
  		</#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->


<#macro printRule rule eventName="">
<@replaceBr>
<#local notBarOpen=(rule.@everyTick[0]?? == true && rule.@everyTick == "true") || eventName == "OnInit">
   //------------------------
   // Rule: ${rule.@name}
   //------------------------
<#if rule.@type == "IfThen" && isNonemptyRule(rule) == true>
   if (<#if notBarOpen><#else>sqIsBarOpen()
      && </#if><#list rule.If.* as block><@printBlock block /></#list>)
   {
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	   <@printBlock block />
	   <#local count=count+1>


	</#list>
  }
<#elseif rule.@type == "IfThenElse" && isNonemptyRule(rule) == true>
  <#if notBarOpen><#else>if(sqIsBarOpen()){</#if>
   if(<#list rule.If.* as block><@printBlock block /></#list>)
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
  <#if notBarOpen><#else>}</#if>
<#elseif rule.@type == "ActionOnly" && isNonemptyRule(rule) == true>
  <#if notBarOpen><#else>if(sqIsBarOpen()){</#if>
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
      <@printBlock block />
	  <#local count=count+1>


	</#list>
  <#if notBarOpen><#else>}</#if>
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
   <#if notBarOpen>
    // init signals on every tick   
	<#list rule.signals.* as signal>
   <@printVariableName signal.@variable /> = <#if signal?children[0]??><@printBlock signal?children[0] /><#else>false</#if>;

	</#list>
   <#else>if (sqIsBarOpen()) {
     // init signals only on bar open 
	<#list rule.signals.* as signal>
     <@printVariableName signal.@variable /> = <#if signal?children[0]??><@printBlock signal?children[0] /><#else>false</#if>;

	</#list>
   } 
   </#if>

<#elseif rule.@type == "SignalFuzzy">
   <#if notBarOpen>     
     // init signals on every tick   
	<#list rule.signals.* as signal>
   <@printFuzzyVariable signal "   " />

	</#list>
   <#else> if (sqIsBarOpen()) {
     // init signals only on bar open 
	<#list rule.signals.* as signal>
     <@printFuzzyVariable signal "     " />

	</#list>
   } 
   </#if>

</#if>
</@replaceBr>
</#macro>

<#-------------------------------------------------------------->

<#macro printFuzzyVariable signal space>
${space}// FUZZY Logic. Variable is true if min ${signal.@minTrue?number} % conditions (<@printFuzzySignalNumbers signal />) are correct
  <#local index = 0 /> 
  <#list signal.* as item>
${space}cond[${index}] = <@printBlock item />;
    <#local index = index+1 />
  </#list>

${space}<@printVariableName signal.@variable /> = sqEvaluateFuzzySignal(<@printFuzzySignalMethodNumbers signal />);
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

<#-------------------------------------------------------------->

<#macro printBlockChild block childName shift="0"><@compress_single_line>
   <#list block?children as c>
      <#if c?node_name == "Block" && c.@key == childName>
	     <@printBlock c shift/>
   	</#if>
   </#list>
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
         <#if block.@customIndicator[0]??>
            <@printCustomIndicator block shift />
         <#else>
            <#if isOnlineVersion() == "true" >
               <#include "/MetaTrader4/blocks/"+blockKey+".tpl">
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

<#macro printCustomIndicator block shift="0"><@compress_single_line>
//double iCustom( string symbol, int timeframe, string name, ..., int mode, int shift)

iCustom(<@printInputCI block />,
"${block.@key}",


<@printShift block shift />)
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printInputCI block quoteSymbol=false ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "data" && param.@type == "data">
      <@printDataVar param quoteSymbol />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function blockFileExists block>
BK: {block?name}
	<#return true>
</#function>

<#-------------------------------------------------------------->

<#macro printBlock2 block>${block?node_name}</#macro>


<#-------------------------------------------------------------->

<#macro printManageOrdersForMagicNumbers >
	<#list MagicNumbersList as magicNumber>
   sqManageOrders(${magicNumber});
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro getAllMagicNumbers><@compress_single_line>
  <#global MagicNumbersList = [] />
  
  <#recurse doc.StrategyFile.Strategy.Rules.Events>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro @element><#recurse></#macro>

<#-------------------------------------------------------------->

<#macro Item><@compress_single_line>
  <#if (.node.@returnType[0]!"") == "order">
    <#recurse>
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro Param><@compress_single_line>
  <#if (.node.@key[0]!"") == "#MagicNumber#">
    <#assign MN = getMagicNumberParam(.node) />
    <@addMagicNumberToList MN />
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro addMagicNumberToList MN><@compress_single_line>
  <#if MagicNumbersList?seq_contains(MN) == false>
      <#global MagicNumbersList = MagicNumbersList + [MN] />  
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function getMagicNumberParam param>
  <#if param.@variable[0]?? && param.@variable == "true">
    <#if param == "">
      <#return "0" >
    <#else>
      <#list doc.StrategyFile.Strategy.Variables.* as var>
		    <#if var.id == param>
          <#return var.name>
        </#if>
	    </#list>
    </#if>
  <#else>
    <#if param == "" || isNumber(param) == false>
        <#return 0 >
    <#else>
        <#return param >
    </#if>
  </#if>
  <#return param >
</#function>           
                 
<#-------------------------------------------------------------->

<#macro printMarketDigitsCases>
  <#local index = 0 />
  <#list doc.StrategyFile.Strategy.Datas.* as data>
    <#if (index == 0) >
      if(symbol == Symbol()){
         return MainChartTickSizeSQ;
      }
    <#else>  
      if(symbol == correctSymbol(Subchart${data.id}Symbol)){
         return Subchart${data.id}TickSizeSQ;
      }
    </#if>
    
    <#local index = index + 1 />
  </#list>      
</#macro>  