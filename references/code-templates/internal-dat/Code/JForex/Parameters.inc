<#-------------------------------------------------------------->

<#macro jfPrintSubcharts>
  <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
private String Subchart${index}Symbol = "${data.symbol}";
private int Subchart${index}Timeframe = ${data.timeFrame}; <@jfPrintTFHelp data.timeFrame />
private double Subchart${index}TickSizeSQ = <#if data.ticksize[0]??>${data.ticksize}<#else>-1</#if>;
      <#else>
      
private String slts = "----------- Use Tick size from SQ (for CFDs) -----------";
// For exotic pairs (usually non forex CFDs) the default method of computing 
// tick size in EA might not work correctly.
// By turning this on and specifying MainChartTickSizeSQ value you can let EA 
// use the correct tick size          
private boolean UseSQTickSize = false;                                                                             
private double MainChartTickSizeSQ = <#if data.ticksize[0]??>${data.ticksize}<#else>-1</#if>;
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>
                  
<#-------------------------------------------------------------->

<#macro jfPrintSubchartConditions>
  <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
    else if (symbol == "Subchart${index}Symbol") return Subchart${index}Symbol;
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro correctSubchartSymbols>
  <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
   Subchart${index}Symbol = correctSymbol(Subchart${index}Symbol);        
      </#if>
      <#local index = index + 1 />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro jfPrintTFHelp tf>
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

<#macro jfPrintVariable var>
<@compress_single_line>
<#if var.makeExternal == "true">extern </#if><#if var.type == "boolean">bool<#else>${var.type}</#if> ${var.name} = <#if var.type=="string">"</#if>${var.value}<#if var.type=="string">"</#if>;
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
  <#if param.@variable[0]?? && param.@variable == "true">   
   <#if printType == true>
    <@printVariableType param />
   <#else>
    <@printVariableName param />
   </#if>
  <#elseif param.Formula[0]?? >
    <@printFormulaKey param.Formula[0] />
  <#else>
    <#if param?ends_with(".0")>${param?replace(".0", "")}<#else>${param}</#if>
  </#if>
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

<#macro printCIShift block shift><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <@printVariableName param /><#if shift != "0">+${shift}</#if>

      <#elseif param.Formula[0]?? >
         <@printFormulaKey param.Formula[0] /><#if shift != "0">+${shift}</#if>

      <#else>
         <#if param == "0">
            <#if shift != "0">
               ${shift}
            <#else>
               0
            </#if>
         <#else>
            ${param}<#if shift != "0">+${shift}</#if>
         </#if>
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printShift block shift><@printCIShift block shift /></#macro>

<#-------------------------------------------------------------->

<#macro printInput block quoteSymbol=false ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType == "dataVar">
      <@printDataVar param quoteSymbol param.@chartTF[0] />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printInputSymbol block ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType == "dataVar">
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

<#macro printDataVar id quoteSymbol chartTF="">
   <#if id == "0">
      <#if doc.StrategyFile.Strategy.Datas.data[0].symbol == "NULL" || doc.StrategyFile.Strategy.Datas.data[0].symbol == "Current">
         <#if quoteSymbol == true>"Current"<#else>"Current"</#if>
      <#else>
         "${doc.StrategyFile.Strategy.Datas.data[0].symbol}"
      </#if>
      , <#if chartTF == "">${doc.StrategyFile.Strategy.Datas.data[0].timeFrame}<#else>PERIOD_${chartTF}</#if>
   <#else>
      Subchart${id}Symbol, Subchart${id}Timeframe
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printVariableName variableName><@compress_single_line>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>${var.name}<#if var.name=="Period">Param</#if></#if>
	</#list>
</@compress_single_line></#macro>

<#macro printVariableType variableName><@compress_single_line>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>${var.type}</#if>
	</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printFormulaKey Formula>
${Formula.@key}
</#macro>

<#-------------------------------------------------------------->

<#macro printComputedFromParam block><@compress_single_line>
   <@printParamOptions block "#ComputedFrom#" "0=CLOSE,1=OPEN,2=HIGH,3=LOW,4=MEDIAN_PRICE,5=TYPICAL_PRICE,6=WEIGHTED_CLOSE,7=VOLUME" />
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printParamOptions block paramName paramOptions><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
		<#if param.@variable[0]?? && param.@variable == "true">
			<@printVariableName param />
		<#elseif param.Formula[0]?? >
			ERROR: NOT SUPPORTED !!
		<#else>
			<@printOption param paramOptions />
		</#if>
	</#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printOption param options>
	<#list options?split(",") as option>
		<#local kv = option?split("=") />
		<#if kv[0] == param >
			${kv[1]}
			<#return />
		</#if>
	</#list>
	ERROR: OPTION ${param} NOT FOUND!!
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
		          <#if var.id == param>${var.name}</#if>
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

<#macro printOrderTypeValue param orderType>
<#if param.@variable[0]?? && param.@variable == "true">
sqGetOrderType(<@printVariableName param />, "${orderType}")
<#else>
   <#if param?number gt 0>
      <#switch orderType>
         <#case "MARKET">BUY<#break>
         <#case "STOP">BUYSTOP<#break>
         <#case "LIMIT">BUYLIMIT<#break>
      </#switch>
   <#else>
      <#switch orderType>
         <#case "MARKET">SELL<#break>
         <#case "STOP">SELLSTOP<#break>
         <#case "LIMIT">SELLLIMIT<#break>
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

<#-- macro to get SL or PT value from parameters -->
<#macro printSLPTMethod block orderType slptType slptParamName directionParamName priceParamName symbolParamName isExitMethod="false"><@compress_single_line>

<#list block.* as param>
	<#if param.@key == slptParamName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.SLPT.None">
            0
            <#break>

         <#case "SQ.Formulas.SLPT.UseGlobalSLPT">
            sqGetGlobal${slptType}("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />)
            <#break>

         <#case "SQ.Formulas.SLPT.FixedValue">
            sqGet${slptType}Level("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />, 1, <@printParameterValue param.Formula[0].Param />)
            <#break>

         <#case "SQ.Formulas.SLPT.PctValue">
            sqGet${slptType}PercentLevel("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />, 1, <@printParameterValue param.Formula[0].Param />)
            <#break>

         <#case "SQ.Formulas.SLPT.ATRBasedValue">
            sqGet${slptType}Level("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />, 2, <@printParameterValue param.Formula[0].Param[0] /> * sqATR("Current",0,<@printParameterValue param.Formula[0].Param[1] />,1))
            <#break>

         <#case "SQ.Formulas.SLPT.ValueFromFormula">
            sqGet${slptType}Level("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />, 1, <@printBlock param.Formula[0]?children[0] />)
            <#break>

         <#case "SQ.Formulas.SLPT.PriceLevel">
            sqGet${slptType}Level("<@printParam block symbolParamName />", <@printOrderType block orderType directionParamName />, <@printPrice block orderType priceParamName isExitMethod />, 3, <@printBlock param.Formula[0]?children[0] />)
            <#break>

        <#default>
            !!Incorrect SL or PT Set!!
      </#switch> 
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printPTSLValue paramName SLorPT>
   <#list doc.StrategyFile.Strategy.GlobalSLPT.values.* as slptValue>
   	<#if slptValue?node_name == paramName>
         <#switch slptValue.values.@type>
            <#case "fixed">
   return(sqGet${SLorPT}Level(symbol, orderType, price, 1, <@printParameterValue slptValue.values.value />));
            <#break>
            <#case "variable">
   return(sqGet${SLorPT}Level(symbol, orderType, price, 1, <@compress_single_line><@printVariableName slptValue.values.value /></@compress_single_line>));
            <#break>
            <#case "atr">
   return(sqGet${SLorPT}Level(symbol, orderType, price, 2, <@printParameterValue slptValue.values.value[0] /> * sqATR("Current",0,<@printParameterValue slptValue.values.value[1] />, 1)));
            <#break>
         </#switch>
      </#if>
   </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro jfPrintInputs>

<#if hasStrategyVariables() >
	// Variables
	@Configurable(value = "--------- Variables ---------", readOnly = true)
    public String Variables = "";
	<@printStrategyVariables true />
</#if>
<#if hasTradingOptionsVariables() >

	// Trading Options
	@Configurable(value = "--------- Trading Options ---------", readOnly = true)
	public String tradingOptions = "";
<@printTradingOptionsVariables />
</#if>
<#if hasMoneyManagementVariables() >

<@printMoneyManagementVariables />
</#if>

	private double InitialCapital = <@printInitialCapital />;  
</#macro>

<#-------------------------------------------------------------->

<#function hasStrategyVariables>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
    <#if var.makeExternal == "true">
      <#return true >
    </#if>
	</#list>
  <#return false >  
</#function>

<#-------------------------------------------------------------->

<#macro printStrategyVariables printExternal>
  <#assign somePrinted=false />
	<#list doc.StrategyFile.Strategy.Variables.* as var>
	<@jfPrintVariable var printExternal />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printInitialCapital><@compress_single_line>
  <#if moneyManagement?? && moneyManagement.InitialCapital[0]??>
      ${moneyManagement.InitialCapital}
  <#else>
      10000
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro jfPrintVars>
	// Internal variables
	<@printStrategyVariables false />   
</#macro>

<#-------------------------------------------------------------->

<#macro jfPrintVariable var printInputs>
<#if (printInputs && var.makeExternal == "true") || (!printInputs && var.makeExternal == "false")>
	<#if printInputs>
	@Configurable("${var.name}")
	</#if>
	<#if var.type=="string">
	public String ${var.name}<#if var.name=="Period">Param</#if> = "${var.value}";
	<#else>
	public ${var.type} ${var.name}<#if var.name=="Period">Param</#if> = <#if var.type=="string">"</#if>${var.value}<#if var.type=="string">"</#if>;
	</#if>
</#if>
</#macro>