
<#macro compress_single_line><#local captured><#nested></#local>${captured?replace("^\\s+|\\s+$|\\n|\\r", "", "rm") }</#macro>

<#macro fix_string><#local captured><#nested></#local>${captured?replace("\"", "", "rm")?replace("\'", "\"", "rm") }</#macro>

<#macro escape_string><#local captured><#nested></#local>${captured?replace("\"", "\\\"")}</#macro>

<#macro Descriptions>//${.node}</#macro>

<#-------------------------------------------------------------->

<#-- function will return number of non-empty rules in this event -->

<#function getRulesCountForEvent eventName default="">
	<#local count = 0>   GET FOR EVENT ${eventName}
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
  		<#if event.@key == eventName>
            <#list event.* as rule>
				<#if isNonemptyRule(rule) == true >
                	<#local count = count + 1 >
				</#if>
			</#list>
  		</#if>
	</#list>

    <#return count>
</#function>

<#-------------------------------------------------------------->

<#function isOnlineVersion>
   <#list doc.StrategyFile.options.* as opt>
      <#if opt?node_name == "OnlineVersion">
         <#return opt>
      </#if>
   </#list>

   <#return "false">
</#function>


<#-------------------------------------------------------------->

<#function isNonemptyRule rule>
	<#local count = 0>

	<#if rule.@type == "IfThen">
        <#list rule.If.* as ifs>
           	<#local count = count + 1 >
		</#list>
        <#list rule.Then.* as thens>
           	<#local count = count + 1 >
		</#list>

	<#elseif rule.@type == "IfThenElse">
        <#list rule.If.* as ifs>
           	<#local count = count + 1 >
		</#list>
        <#list rule.Then.* as thens>
           	<#local count = count + 1 >
		</#list>
        <#list rule.Else.* as thens>
           	<#local count = count + 1 >
		</#list>

	<#elseif rule.@type == "ActionOnly">
        <#list rule.Then.* as thens>
           	<#local count = count + 1 >
		</#list>

	<#elseif rule.@type == "ForEachTrade">
        <#list rule.If.* as ifs>
           	<#local count = count + 1 >
		</#list>
        <#list rule.Then.* as thens>
           	<#local count = count + 1 >
		</#list>

	<#elseif rule.@type == "Signal">
    <#list rule.signals.* as signal>
      <#list signal.* as condition>
           	<#local count = count + 1 >
      </#list>
		</#list>

	<#elseif rule.@type == "SignalFuzzy">
    <#list rule.signals.* as signal>
      <#list signal.* as condition>
           	<#local count = count + 1 >
      </#list>
		</#list>

	<#elseif rule.@type == "StockpickerEntryExit">
    <#if rule.Entry[0]?? || rule.Exit[0]??>
      <#local count = count + 1 >
    </#if>

	<#elseif rule.@type == "StockpickerPS">
    <#if rule.Value[0]?? || rule.Value[0].Item[0]??>
      <#local count = count + 1 >
    </#if>
	</#if>

	<#if count gt 0>
		<#return true>
	<#else>
		<#return false>
	</#if>
</#function>


<#function getSetting settingName default="">
<#list doc.StrategyFile.TestResults.Results.SettingsFile.Settings.* as c>
  <#if c?node_name == settingName>
    <#return c>
  </#if>
</#list>
<#if default !="">
   <#return default>
<#else>
   <#return "N/A">
</#if>
</#function>

<#-------------------------------------------------------------->

<#function getSetting settingName default="">
<#list doc.StrategyFile.TestResults.Results.SettingsFile.Settings.* as c>
  <#if c?node_name == settingName>
    <#return c>
  </#if>
</#list>
<#if default !="">
   <#return default>
<#else>
   <#return "N/A">
</#if>
</#function>

<#-------------------------------------------------------------->

<#function getOptions optionName default="">

<#-- first go through options -->
<#list doc.StrategyFile.options.* as opt>
  <#if opt?node_name == optionName>
    <#return opt>
  </#if>
</#list>

<#-- first go through attributes -->
<#list doc.StrategyFile.Strategy.@@ as attr>
  <#if attr?node_name == optionName>
    <#return attr>
  </#if>
</#list>

<#-- now check sub elements -->
<#list doc.StrategyFile.Strategy.* as c>
  <#if c?node_name == optionName>
    <#return c>
  </#if>
</#list>

<#-- nothing found -->
<#if default !="">
   <#return default>
<#else>
   <#return "N/A">
</#if>
</#function>

<#-------------------------------------------------------------->

<#function hasVariables ignoreMagicNumber=false>
   <#local hasParams=false />
   
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.name?contains("MagicNumber")>
         <#if ignoreMagicNumber == false>
            <#local hasParams=true>
         </#if>
      <#else>
         <#return true>
      </#if>   
	</#list>

    <#return hasParams>
</#function>

<#-------------------------------------------------------------->

<#function containsMultiCharts >
  <#-- check multiple datas -->
  <#local datasCount=0 />
  <#list doc.StrategyFile.Strategy.Datas.* as var>
    <#local datasCount=datasCount+1>
		<#if (datasCount > 1) >
      <#return true>
    </#if>   
	</#list>
  
  <#-- check OpenD, HighD, LowD, CloseD -->
  <#return findDailyBlocksRecursive(doc.StrategyFile.Strategy.Rules) >
</#function>

<#-------------------------------------------------------------->

<#function findDailyBlocksRecursive node>

	<#list node.* as subNode>
		<#if subNode?node_name == "Item">
      <#if subNode.@key == "OpenD" || subNode.@key == "HighD" || subNode.@key == "LowD" || subNode.@key == "CloseD">
        <#return true>
      </#if>
    </#if>

    <#if findDailyBlocksRecursive(subNode) >
      <#return true>
    </#if>            
	</#list>

  <#return false>
</#function>

<#-------------------------------------------------------------->

<#function isNumber text>
    <#return text?matches("^[0-9]+$")>
</#function>

<#-------------------------------------------------------------->
     
<#function isBooleanBlock block>
    <#return block?children[0]?? && block?children[0]?children[0]?? && block?children[0]?children[0].@key[0]?? && (block?children[0]?children[0].@key[0] == "BooleanVariable" || block?children[0]?children[0].@key[0] == "Boolean")>
</#function>

<#-------------------------------------------------------------->

<#function childWithKeyExists block key>
  <#list block.* as param>
  	<#if param.@key == key><#return true></#if>
  </#list>
  <#return false>
</#function>

<#-------------------------------------------------------------->

<#macro printCDIndy block sourcecode shift><@compress_single_line>
  <#local cdataId = block.@key[0]?substring(10) />
  <#if cdataId != "">
    <#local found = 0 />
    <#list block.@@ as attr>
      <#if attr?node_name == sourcecode && attr != "" >
        <#local shiftValue><@printCIShift block shift /></#local>
	    <#local code =  attr>
		
		<#list block.* as param>
			<#if param.@variable[0]?? && param.@variable[0] == "true">
				<#local code =  code?replace(param.@key, getVariableValue(param))>
			<#else>
				<#if param.@type=="string">
					<#local code =  code?replace(param.@key, "\""+param+"\"")>
				<#else>
					<#local code =  code?replace(param.@key, param)>
				</#if>   
		  </#if>   
		</#list>
	   
	   ${code}
		
        <#local found = 1 />
      </#if>
    </#list>
    <#if found == 0>
    ExternIndy:<@printCDIndyName block />.<@printCDIndyLine block />[<@printShift block shift />]
    </#if>
  <#else>
    [UNKNOWN EXTERNAL INDICATOR]
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCDIndyName block><@compress_single_line>
  <#local cdataId = block.@key[0]?substring(10) />
  ${cdataId}
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCDIndyLine block><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Value#">
        <@printOptionReversed param param.@values />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printOptionReversed param options>
	<#list options?split(",") as option>
		<#local kv = option?split("=") />
		<#if kv[1] == param >
			${kv[0]}
			<#return />
		</#if>
	</#list>
	ERROR: OPTION ${param} NOT FOUND!!
</#macro>


<#-------------------------------------------------------------->

<#macro printCBlock block sourcecode shift><@compress_single_line>
  <#local cblockId = block.@key[0]?substring(7) />
  <#if sourcecode == "pseudo">
    CustomBlock:<@printCBlockName block />(<@printCBlockParams block />){<@printBlock block.Contents?children[0] />}
  <#else>
    <#if sourcecode == "el">{CustomBlock:<@printCBlockName block />}<#else>/*CustomBlock:<@printCBlockName block />*/</#if>
    <@printBlock block.Contents?children[0] />
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCBlockName block><@compress_single_line>
  <#local cblockId = block.@name[0] />
  ${cblockId}
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCBlockParams block><@compress_single_line>
<#local paramsAsStr = "" />
<#list block.* as param>
	<#if param?node_name == "Param" && param.@type != "data">
        <#if  paramsAsStr != "">       
            <#local paramsAsStr = paramsAsStr + ", " />
        </#if>
        <#local paramsAsStr = paramsAsStr + param />
   </#if>
</#list>
<#if paramsAsStr != "">
${paramsAsStr}
</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCBlockShift block shift><@compress_single_line>
<#list block.* as param>
	<#if param?node_name == "Param" && param.@type?starts_with("#Shift")>
        ${param}
         <#if shift != "0">+${shift}</#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printTimeFromHHMM block paramKey addQuotes=true addMT5Quotes=false><@compress_single_line>
<#list block.* as param>
	<#if param?node_name == "Param" && param.@key == paramKey>
      <#if param.@variable[0]?? && param.@variable[0] == "true">
         HHMMToString(${getVariableValue(param)})
      <#else>
        <#local intValue>${param}</#local> 
        <#if addQuotes>"</#if>
        <#if addMT5Quotes>'</#if>
        
        <#if intValue?contains(":")>
          <#list intValue?split(":") as x>
            ${x?number?string["00"]}
            <#if x?index == 0>:</#if>
          </#list>
        <#else>
          ${(intValue?number / 100)?int}:${(intValue?number % 100)?string["00"]}
        </#if>

        <#if addMT5Quotes>'</#if>
        <#if addQuotes>"</#if>
      </#if>
  </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printBacktestInfo><@compress_single_line>
<#if doc.StrategyFile.options[0]?? && doc.StrategyFile.options.Backtest[0]??> 
<#local bt = doc.StrategyFile.options.Backtest />
Backtested on <#if bt.Symbol[0]??>${bt.Symbol}<#else>??</#if> / <#if bt.Timeframe[0]??>${bt.Timeframe}<#else>??</#if>, <#if bt.DateFrom[0]??>${bt.DateFrom}<#else>??</#if> - <#if bt.DateTo[0]??>${bt.DateTo}<#else>??</#if>
<#else>
{ Backtest info not available }
</#if>
</@compress_single_line></#macro>   

<#-------------------------------------------------------------->

<#macro printComment block><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Comment#">
      <#if param.@variable[0]?? && param.@variable == "true">
        <@printVariableName param />
      <#else>
        "${param}"
      </#if>
      <#break>
   </#if>
</#list>
</@compress_single_line></#macro>   

<#-------------------------------------------------------------->

<#macro printStrategyName><@compress_single_line>
  <#if doc.StrategyFile.options[0]??>
    <#if doc.StrategyFile.options.StrategyClassName[0]??>
      ${doc.StrategyFile.options.StrategyClassName}
    <#else>
      ${doc.StrategyFile.options.StrategyName}
    </#if> 
  </#if>
</@compress_single_line></#macro>   

<#-------------------------------------------------------------->

<#macro printCustomComment><@compress_single_line>
  <#local comment><@printStrategyName /></#local>
  <#local comment = comment?replace("Optimized", "Opt.") />
  <#local comment = comment?replace("Optimization", "Opt.") />
  <#if comment?length <= 30 >
    ${comment}
  <#else>
    ${comment?substring(0,30)}
  </#if>
</@compress_single_line></#macro>   