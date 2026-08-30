<#-------------------------------------------------------------->

<#macro printExitMethodInit exitMethodKey block orderType directionParamName priceParamName symbolParamName>
   <#if isOnlineVersion() == "true" >
   	<#include "/MetaTrader4/ExitMethods/"+exitMethodKey+"_init.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_init.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printOrderType block orderType directionParamName ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == directionParamName>
      <@printOrderTypeValue param orderType />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printOrderTypeValue param orderType>
<#if param.@variable[0]?? && param.@variable == "true">
sqGetOrderType(<@printVariableName param />, "${orderType}")
<#else>
   <#if param?number gt 0>
      <#switch orderType>
         <#case "MARKET">OP_BUY<#break>
         <#case "STOP">OP_BUYSTOP<#break>
         <#case "LIMIT">OP_BUYLIMIT<#break>
      </#switch>
   <#else>
      <#switch orderType>
         <#case "MARKET">OP_SELL<#break>
         <#case "STOP">OP_SELLSTOP<#break>
         <#case "LIMIT">OP_SELLLIMIT<#break>
      </#switch>
   </#if>
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printPrice block orderType priceParamName><@compress_single_line>
<#if orderType == "MARKET">0
<#else>
   <#list block.* as param>
	  <#if param.@key == priceParamName>
         <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.Price.UseFormula">
            <#if param.Formula[0]?children[0]?? >
               <@printBlock param.Formula[0]?children[0] />
            <#else>
               #Unknown#
            </#if>
            <#break>

        <#default>
            !!Incorrect Price Set!!
      </#switch>
   </#if>
</#list>
</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printVariables>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
    <#if var.name != "LongEntrySignal" && var.name != "ShortEntrySignal" && var.name != "LongExitSignal" && var.name != "ShortExitSignal">
<@printVariable var />
    </#if>
	</#list>
</#macro>


<#-------------------------------------------------------------->

<#macro printSubcharts>
   <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
Subchart${index} = ${data.symbol} / <@printSubchartTF data.timeFrame />;
      <#else>
Main chart = <#if data.symbol == "NULL">Current Symbol<#else>${data.symbol}</#if> / <@printSubchartTF data.timeFrame />;
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printVariable var>
${var.type} ${var.name} = <@compress_single_line>${var.value}</@compress_single_line>;
</#macro>

<#-------------------------------------------------------------->

<#macro printParam block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName><@printParameterValue param /></#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printDOWParam block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName><@printDOWParameterValue param /></#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printMonthParam block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName><@printMonthParameterValue param /></#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function getParam block paramName>
<#list block.* as param>
	<#if param.@key == paramName><#return getParameterValue(param) ></#if>
</#list>
<#return null >
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

<#function getBlockParamValue block paramName>
<#list block.* as param>
	<#if param.@key == paramName><#return getParameterValue(param) ></#if>
</#list>
<#return "" >
</#function>

<#-------------------------------------------------------------->

<#function getVariableValue variableName>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName><#return var.value ></#if>
	</#list>
   <#return null >
</#function>

<#-------------------------------------------------------------->

<#-- macro to get SL or PT value from parameters -->
<#macro printSLPTMethod block orderType slptType slptParamName directionParamName priceParamName symbolParamName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == slptParamName>    
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.SLPT.None">
            0
            <#break>

         <#case "SQ.Formulas.SLPT.UseGlobalSLPT">
            = Global ${slptType}
            <#break>

         <#case "SQ.Formulas.SLPT.FixedValue">
            = <@printParameterValue param.Formula[0].Param /> pips
            <#break>

         <#case "SQ.Formulas.SLPT.PctValue">
            = <@printParameterValue param.Formula[0].Param /> %
            <#break>

         <#case "SQ.Formulas.SLPT.ATRBasedValue">
            = <@printParameterValue param.Formula[0].Param[0] /> * ATR(<@printParameterValue param.Formula[0].Param[1] />)
            <#break>

         <#case "SQ.Formulas.SLPT.ValueFromFormula">
            at <@printBlock param.Formula[0]?children[0] />
            <#break>

         <#case "SQ.Formulas.SLPT.PriceLevel">
            at level <@printBlock param.Formula[0]?children[0] />
            <#break>

        <#default>
            !!Incorrect SL or PT Set!!
      </#switch>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printParameterValue param>
<#if param.@variable[0]?? && param.@variable == "true">
<@printVariableName param />
<#elseif param.Formula[0]?? >
<@printFormulaValue param.Formula[0] />
<#else>
<#if param?ends_with(".0")>${param?replace(".0", "")}<#else>${param}</#if>
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printDOWParameterValue param>
<#if param.@variable[0]?? && param.@variable == "true">
<@printVariableName param />
<#elseif param.Formula[0]?? >
<@printFormulaValue param.Formula[0] />
<#else>
<#if param?ends_with(".0")><#local dow = param?replace(".0", "")/><#else><#local dow = param/></#if>
	<#switch dow>
  	<#case "0">Sunday<#break>
  	<#case "1">Monday<#break>
  	<#case "2">Tuesday<#break>
  	<#case "3">Wednesday<#break>
  	<#case "4">Thursday<#break>
  	<#case "5">Friday<#break>
  	<#case "6">Saturday<#break>
  	<#default>Unknown = ${dow}
  </#switch>
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printMonthParameterValue param>
<#if param.@variable[0]?? && param.@variable == "true">
<@printVariableName param />
<#elseif param.Formula[0]?? >
<@printFormulaValue param.Formula[0] />
<#else>
<#if param?ends_with(".0")><#local month = param?replace(".0", "")/><#else><#local month = param/></#if>
	<#switch month>
  	<#case "1">January<#break>
  	<#case "2">February<#break>
  	<#case "3">March<#break>
  	<#case "4">April<#break>
  	<#case "5">May<#break>
  	<#case "6">Jun<#break>
  	<#case "7">July<#break>
  	<#case "8">August<#break>
  	<#case "9">September<#break>
  	<#case "10">October<#break>
  	<#case "11">November<#break>
  	<#case "12">December<#break>
  	<#default>Unknown = ${month}
  </#switch>
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printVariableName variableName><@compress_single_line>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>${var.name}</#if>
	</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFormulaValue Formula>
${Formula.@key}
</#macro>

<#-------------------------------------------------------------->

<#macro printInput block addComma quoteSymbol=false ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType == "dataVar">
      <@printDataVar param addComma quoteSymbol />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printDataVar2 id addComma quoteSymbol>
	<#list doc.StrategyFile.Strategy.Datas.* as data>
		<#if data.id == id>
         <#if data.symbol != "NULL" && data.symbol != "Current" && data.timeFrame != "0" && data.timeFrame != "Current">
           ${data.symbol}, ${data.timeFrame}<#if addComma=true>,</#if>
         <#else>
            <#if data.symbol != "NULL" && data.symbol != "Current">
               ${data.symbol}<#if addComma=true>,</#if>
            </#if>
            <#if data.timeFrame != "0" && data.timeFrame != "Current">
               ${data.timeFrame}<#if addComma=true>,</#if>
            </#if>
         </#if>
      </#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printDataVar id addComma quoteSymbol>
   <#if id == "0">
      Main chart
      <#if doc.StrategyFile.Strategy.Datas.data[0].timeFrame != "0">
      ,
      ${doc.StrategyFile.Strategy.Datas.data[0].timeFrame}
      </#if>
   <#else>
      Subchart${id}, <@printSubchartTF doc.StrategyFile.Strategy.Datas.data[id?number].timeFrame />
   </#if>
   <#if addComma=true>, </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro mt4PrintSymbol symbol>
<@compress_single_line>
<#if symbol == "Same as main chart">
${doc.StrategyFile.Strategy.Datas.data[0].symbol}  // Same as main chart
<#else>
${symbol}
</#if>
</@compress_single_line>
</#macro>

<#-------------------------------------------------------------->

<#macro printSubchartTF tf><@compress_single_line>
<#switch tf>
  <#case "0">Current TF<#break>
  <#case "1">M1<#break>
  <#case "5">M5<#break>
  <#case "10">M10<#break>
  <#case "15">M15<#break>
  <#case "30">M30<#break>
  <#case "60">H1<#break>
  <#case "120">H2<#break>
  <#case "240">H4<#break>
  <#case "1440">D1<#break>
  <#case "10080">Weekly<#break> 
  <#case "43200">Monthly<#break>     
  <#case "40320">Monthly<#break>     
  
  <#case "2">M2<#break>
  <#case "3">M3<#break>
  <#case "4">M4<#break>
  <#case "6">M6<#break>
  <#case "10">M10<#break>
  <#case "12">M12<#break>
  <#case "16385">H1<#break>
  <#case "16386">H2<#break>
  <#case "16387">H3<#break>
  <#case "16388">H4<#break>
  <#case "16390">H6<#break> 
  <#case "16392">H8<#break>
  <#case "16396">H12<#break>
  <#case "16408">D1<#break>
  <#case "32769">W1<#break>   
  <#case "49153">MN1<#break>
  
  <#default>${tf}
</#switch>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printShift block shift><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <@printVariableName param />
         <#if shift != "0">+${shift}</#if>

      <#elseif param.Formula[0]?? >
         <@printCompositeParameterValue param.Formula[0] />
         <#if shift != "0">+${shift}</#if>

      <#else>
         <#if param == "0">
            <#if shift != "0">
               ${shift}
            <#else>
               0
            </#if>
         <#else>
            ${param}
         <#if shift != "0">+${shift}</#if>
      </#if>
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function getShift block shift="0">
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <#return "var" />
      <#elseif param.Formula[0]?? >
         <#return "formula" />
      <#else>
         <#if param == "0">
            <#if shift != "0">
               <#return shift />
            <#else>
               <#return "0" />
            </#if>
         <#else>
            <#return param />
         </#if>
      </#if>
   </#if>
</#list>

<#return "unknown" />
</#function>

<#-------------------------------------------------------------->

<#macro printComputedFromParam block key="#ComputedFrom#"><@compress_single_line>
   <@printParamOptions block key "0=PRICE_CLOSE,1=PRICE_OPEN,2=PRICE_HIGH,3=PRICE_LOW,4=PRICE_MEDIAN,5=PRICE_TYPICAL,6=PRICE_WEIGHTED,7=VOLUME" "PRICE_CLOSE"/>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printParamOptions block paramName paramOptions defaultValue="NULL"><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
		<#if param.@variable[0]?? && param.@variable == "true">
			<@printVariableName param />
		<#elseif param.Formula[0]?? >
			ERROR: NOT SUPPORTED !!
		<#else>
			<@printOption param paramOptions defaultValue/>
		</#if>
	</#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printOption param options defaultValue="NULL">
	<#list options?split(",") as option>
		<#local kv = option?split("=") />
		<#if kv[0] == param >
			${kv[1]}
			<#return />
		</#if>
	</#list>
	<#if defaultValue == "NULL">
	   ERROR: OPTION ${param} NOT FOUND!!
   <#else>
      ${defaultValue}
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#function hasSetParam block paramName>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.@variable[0]?? && param.@variable == "true">
         <#return true />
      <#elseif param.Formula[0]?? >
         <#if param.Formula.@key?contains("None") >
            <#return false />
         <#else>
            <#return true />
         </#if>
      <#else>
         <#if param != "0"><#return true><#else><#return false></#if>
      </#if>
   </#if>
</#list>
<#return false>
</#function>

<#-------------------------------------------------------------->

<#macro printMagicNumber block ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#MagicNumber#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <#if param == "">
            0
         <#else>
            <#list doc.StrategyFile.Strategy.Variables.* as var>
		          <#if var.id == param>${var.name}</#if>
	         </#list>
         </#if>
      <#else>
      0
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printRangeLevelMethod block paramName printType><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.Formula[0].@key?contains(".None")>
            0

      <#elseif param.Formula[0].@key?contains(".FixedValue")>
            <@printParameterValue param.Formula[0].Param /> pips

      <#elseif param.Formula[0].@key?contains(".ATRBasedValue")>
           <@printParameterValue param.Formula[0].Param[0] /> * ATR(<@printParameterValue param.Formula[0].Param[1] />)

      <#elseif param.Formula[0].@key?contains(".PriceRange")>
           <@printBlock param.Formula[0]?children[0] />

      <#elseif param.Formula[0].@key?contains(".PriceLevel")>
           <@printBlock param.Formula[0]?children[0] />

      <#elseif param.Formula[0].@key?contains(".ValueFromFormula")>
           <@printBlock param.Formula[0]?children[0] />
           
      <#else>
         !!Incorrect SL or PT Set ${param.Formula[0].@key}!!
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printPriceMethod block><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Price#">
      <@printBlock param.Formula[0]?children[0] />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCloseSizeMethod block ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Size#">
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.CloseSize.FullPosition">
            Full position
            <#break>

         <#case "SQ.Formulas.Size.UseGlobalMM">
            according to Global MM
            <#break>
            
         <#case "SQ.Formulas.CloseSize.Percentage">
            <@printParameterValue param.Formula[0].Param /> % of position
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFromVariable">
            size = <@printVariableName param.Formula[0].Param />
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFormula">
            size = <@printBlock param.Formula[0]?children[0] />
            <#break>

         <#case "SQ.Formulas.Size.DefineOwnSize">
            <@printParameterValue param.Formula[0].Param /><#break>

        <#default>
            This will be processed if it is neither
      </#switch>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFormulaParam block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <@printBlock param.Formula[0]?children[0] />
   </#if>
</#list>
</@compress_single_line></#macro>
    
<#-------------------------------------------------------------->

<#function hasTrailingActivation block>
   <#if hasSetParam(block, "#TrailingStop.TrailingActivation#")>
      <#local value><@printRangeLevelMethod block "#TrailingStop.TrailingActivation#" "key" /></#local>
      <#if value == "-5000000pips">
        <#return false />
      <#else> 
        <#return true />
      </#if>
   <#else>
      <#return false />
   </#if>
</#function>  