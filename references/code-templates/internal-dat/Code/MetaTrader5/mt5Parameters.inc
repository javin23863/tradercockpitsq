<@moneyManagementLoad />

<#-------------------------------------------------------------->

<#macro moneyManagementLoad>
  <#if doc.StrategyFile.Strategy.MoneyManagement[0]??> 
		<#assign moneyManagement = doc.StrategyFile.Strategy.MoneyManagement />
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro mt5PrintSubcharts>
   <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
input string Subchart${index}Symbol = "${data.symbol}";
input int Subchart${index}Timeframe = ${data.timeFrame};
input double Subchart${index}TickSizeSQ = <#if data.ticksize[0]??>${data.ticksize}<#else>-1</#if>; //Subchart${index}PipSizeSQ
      <#else>
      
input string slts = "----------- Use Tick size from SQ (for CFDs) -----------";
// For exotic pairs (usually non forex CFDs) the default method of computing 
// tick size in EA might not work correctly.
// By turning this on and specifying MainChartTickSizeSQ value you can let EA 
// use the correct tick size          
input bool UseSQTickSize = false; // UseSQPipSize                                                               
input double MainChartTickSizeSQ = <#if data.ticksize[0]??>${data.ticksize}<#else>-1</#if>; //MainChartPipSizeSQ
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>
                       
<#-------------------------------------------------------------->

<#macro mt5PrintSubchartConditions>
  <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
    else if (symbol == "Subchart${index}Symbol") return correctSymbol(Subchart${index}Symbol);
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro mt5PrintTFHelp tf>
<@compress_single_line>
<#switch tf>
  <#case "1">// M1<#break>
  <#case "5">// M5<#break>
  <#case "10">// M10<#break>
  <#case "15">// M15<#break>
  <#case "30">// M30<#break>
  <#case "60">// H1<#break>
  <#case "120">// H2<#break>
  <#case "240">// H4<#break>
  <#case "1440">// D1<#break>
</#switch>
</@compress_single_line>
</#macro>

<#-------------------------------------------------------------->

<#macro mt5PrintVariables>
  <#local MagicNumberDefined=false />
	<#list doc.StrategyFile.Strategy.Variables.* as var>
  <#if var.name == "MagicNumber">
      <#local MagicNumberDefined=true />
  </#if>
<@mt5PrintVariable var />

	</#list>
  <#if MagicNumberDefined == false>
int MagicNumber = 12345;      //default MagicNumber - will be used in orders that do not have MagicNumber specified
  </#if>

//+------------------------------------------------------------------+
// Money Management variables
//+------------------------------------------------------------------+
<@printMoneyManagementVariables />
  
  
input double InitialCapital = <@printInitialCapital />; //InitialCapital (0 means whole account balance). Used in some MM methods.
  
//+------------------------------------------------------------------+
// Trading Options variables
//+------------------------------------------------------------------+
<@printTradingOptionsVariables />

</#macro>
          
<#-------------------------------------------------------------->

<#macro mt5PrintVariable var><@compress_single_line>
<#local timeValue="" />
<#local isTime=var.type == "int" && (var.name?contains("TimeTo") || var.name?contains("TimeFrom")) />
<#if isTime>
   <#local minuteValue = (var.value?number % 100) />
   <#if minuteValue < 10>
      <#local minuteValue = "0" + minuteValue?string(",##0;; roundingMode=down") />
   <#else>
      <#local minuteValue = minuteValue?string(",##0;; roundingMode=down") />
   </#if>
   <#local hourValue = (var.value?number / 100)?string(",##0;; roundingMode=down") />
   <#local timeValue = hourValue + ":" + minuteValue />
</#if>
<#if var.makeExternal == "true">input</#if> <#if var.type=="boolean">bool<#elseif isTime>string<#else>${var.type}</#if> ${var.name} = <#if var.type=="string">"${var.value}"<#elseif isTime>"${timeValue}"<#else>${var.value}</#if>;        //${var.name}
</@compress_single_line>
</#macro>

<#-------------------------------------------------------------->

<#macro printParam block paramName defaultValue="NULL" printType=false><@compress_single_line>
<#local found=false />
<#list block.* as param>
	<#if param.@key == paramName>
      <@printParameterValue param printType/>
      <#local found=true />
   </#if>
</#list>
<#if found==false && defaultValue != "NULL">${defaultValue}</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printParameterValue param printType=false><@compress_single_line>
<#if param.@variable[0]?? && param.@variable[0] == "true">
   <#if printType == true>
      <@printVariableType param />
   <#else>
      <@printVariableName param />
   </#if>
<#elseif param.Formula[0]?? >
  <@printFormulaKey param.Formula[0] />
<#else>
  ${param}
</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFormulaParam block paramName isRule=false><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <@printBlock param.Formula[0]?children[0] "0" isRule/>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCIShift block shift forcePrint=false><@compress_single_line>
<#local found=false>
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#local found=true>
      <#if param.@variable[0]?? && param.@variable == "true">
         <@printVariableName param />
         <#if shift != "0">+${shift}</#if>

      <#elseif param.Formula[0]?? >
         <@printFormulaKey param.Formula[0] />
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
<#if found == false && forcePrint == true>0</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printShift block shift forcePrint=false><@printCIShift block shift forcePrint /></#macro>

<#-------------------------------------------------------------->

<#macro printInput block quoteSymbol=false ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType[0]?? && param.@controlType == "dataVar">
      <@printDataVar param quoteSymbol "," false param.@chartTF[0] />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printInputSymbol block ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType[0]?? && param.@controlType == "dataVar">
   	<#list doc.StrategyFile.Strategy.Datas.* as data>
         <#if data.id == param>
            <#if data.symbol == "NULL" || data.symbol == "Current">
               "NULL"
            <#else>"${data.symbol}"</#if>
         </#if>
      </#list>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printDataVar id quoteSymbol separator="," simple=false chartTF="">
   <#if id == "0">
      <#if doc.StrategyFile.Strategy.Datas.data[0].symbol == "NULL" || doc.StrategyFile.Strategy.Datas.data[0].symbol == "Current">
         <#if quoteSymbol == true>"NULL"<#else>NULL</#if>
      <#else>
         "${doc.StrategyFile.Strategy.Datas.data[0].symbol}"
      </#if>
      ${separator}
      <#if chartTF == "">${doc.StrategyFile.Strategy.Datas.data[0].timeFrame}<#else>PERIOD_${chartTF}</#if>
   <#else>
      <#if simple == true>
        Subchart${id}
      <#else>
        correctSymbol(Subchart${id}Symbol)
        ${separator}
        TFMigrate(Subchart${id}Timeframe)
      </#if>
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#function printIndyInput block >
<#local text="" />
<#list block.* as param>
	 <#if param.@controlType[0]?? && param.@controlType == "dataVar">
      <#assign paramValue><@compress_single_line><@printDataVar param false "_" true param.@chartTF[0] /></@compress_single_line></#assign>
      <#local text += paramValue />
   </#if>
</#list>
<#return text>
</#function>

<#-------------------------------------------------------------->

<#macro printVariableName variableName><@compress_single_line>
    <#local found = false />
    
    <#attempt>
    	<#list variables as var>
    		<#if var.name == variableName>
          ${var.shortName}
          <#local found = true />
          <#break>
        </#if>
    	</#list>
    <#recover>
    </#attempt>
    
    <#if found == false>  
      <#attempt>
        <#list doc.StrategyFile.Strategy.Variables.* as var>
    		  <#if var.id == variableName>
            ${var.name}
            <#break>
          </#if>
        </#list>
      <#recover>
        ${variableName}
      </#attempt>
    </#if>
</@compress_single_line></#macro>

<#macro printVariableType variableName><@compress_single_line>
    <#local found = false />
    
    <#attempt>
    	<#list variables as var>
    		<#if var.name == variableName>
            <#if var.type == "boolean">
               bool
            <#else>
               ${var.type}
            </#if>
          <#local found = true />
          <#break>
        </#if>
    	</#list>
    <#recover>
    </#attempt>
    
    <#if found == false>  
      <#attempt>
        <#list doc.StrategyFile.Strategy.Variables.* as var>
    		  <#if var.id == variableName>
               <#if var.type == "boolean">
                  bool
               <#else>
                  ${var.type}
               </#if>
            <#break>
          </#if>
        </#list>
      <#recover>
        ${variableName}
      </#attempt>
    </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFormulaKey Formula>
${Formula.@key}
</#macro>

<#-------------------------------------------------------------->

<#macro printComputedFromParam block><@compress_single_line>
   <@printParamOptions block "#ComputedFrom#" "0=PRICE_CLOSE,1=PRICE_OPEN,2=PRICE_HIGH,3=PRICE_LOW,4=PRICE_MEDIAN,5=PRICE_TYPICAL,6=PRICE_WEIGHTED" "PRICE_CLOSE"/>
</@compress_single_line></#macro>


<#macro printParamOptions block paramName paramOptions defaultValue="NULL"><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
		<#if param.@variable[0]?? && param.@variable == "true">
			<@printVariableName param />
		<#elseif param.Formula[0]?? >
			ERROR: NOT SUPPORTED !!
		<#else>
			<@printOption param paramOptions defaultValue />
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

<#macro printOrderType block orderType directionParamName ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == directionParamName>
      <@printOrderTypeValue param orderType />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printMagicNumber block ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#MagicNumber#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <#if param == "">
            0
         <#else>
            <#list doc.StrategyFile.Strategy.Variables.* as var>
		          <#if var.id == param || var.name == param>${var.name}</#if>
	         </#list>
         </#if>
      <#else>
        <#if param == "" || isNumber(param) == false>
            0
        <#else>
            ${param}
        </#if>
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printSymbol block ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Symbol#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <#if param == "">
            "${param}"
         <#else>
            <#list doc.StrategyFile.Strategy.Variables.* as var>
		          <#if var.id == param>${var.name}</#if>
	         </#list>
         </#if>
      <#else>
        "${param}"
      </#if>
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
         <#case "MARKET">ORDER_TYPE_BUY<#break>
         <#case "STOP">ORDER_TYPE_BUY_STOP<#break>
         <#case "LIMIT">ORDER_TYPE_BUY_LIMIT<#break>
      </#switch>
   <#else>
      <#switch orderType>
         <#case "MARKET">ORDER_TYPE_SELL<#break>
         <#case "STOP">ORDER_TYPE_SELL_STOP<#break>
         <#case "LIMIT">ORDER_TYPE_SELL_LIMIT<#break>
      </#switch>
   </#if>
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


<#macro hasSetParam2 block paramName>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.@variable[0]?? && param.@variable == "true">
         true1
      <#elseif param.Formula[0]?? >
         <#if param.Formula.@key?contains("None") >
            true2
         </#if>
      <#else>
         <#if param != "0">true3<#else>false1</#if>
      </#if>
   </#if>
</#list>
false2
</#macro>


<#-------------------------------------------------------------->


<#function hasZeroValue block paramName>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if getParamValue(param, paramName) == "0">
         <#return true />
      <#else>
         <#return false />
      </#if>
   </#if>
</#list>
<#return false>
</#function>

<#-------------------------------------------------------------->

<#function getParamValue param paramName>
<#if param.@variable[0]?? && param.@variable == "true">
   <#return getVariableValue(param) />
<#elseif param.Formula[0]?? >
   <#return "CantBeUsedHere" />
<#else>
   <#return param />
</#if>
</#function>

<#-------------------------------------------------------------->

<#function getVariableValue variableName>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>
        <#return var.value />
    </#if>
	</#list>
  <#return "Undefined" />
</#function>

<#-------------------------------------------------------------->

<#function isIntVariable variableName>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>
        <#return var.type?lower_case == "int" />
    </#if>
	</#list>
  <#return false />
</#function>

<#-------------------------------------------------------------->

<#macro printOppositeDirection block><@compress_single_line>
<#local value><@printParam block "#Direction#" /></#local>
<#if value == "1">-1<#else>1</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro showSymbolsInMarketWatch>
  <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
   SymbolSelect(correctSymbol(Subchart${index}Symbol), true);        
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro tryPrintCustomComment><@compress_single_line>
<#local found=false />

<#list doc.StrategyFile.Strategy.Variables.* as var>
  <#if var.name == "CustomComment">
      <#local found=true />
  </#if>
</#list>

<#if found == false>
input string CustomComment = "<@printCustomComment />";
</#if>

</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printInitialCapital><@compress_single_line>
  <#if moneyManagement?? && moneyManagement.InitialCapital[0]?? >
      ${moneyManagement.InitialCapital}
  <#else>
      10000
  </#if>
</@compress_single_line></#macro>