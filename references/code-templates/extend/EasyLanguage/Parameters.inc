
<#-------------------------------------------------------------->

<#macro elPrintInputs>
inputs:
<#if hasStrategyVariables() >
	// Strategy variables
	<@printStrategyVariables true />
</#if>
<#if hasTradingOptionsVariables() >

	// Trading Options
<@printTradingOptionsVariables />
</#if>
<#if hasMoneyManagementVariables() >

<@printMoneyManagementVariables />
</#if>

	InitialCapital(<@printInitialCapital />);
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
	<@elPrintVariable var printExternal />
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printInitialCapital><@compress_single_line>
  <#if moneyManagement?? && moneyManagement.InitialCapital[0]?? >
      ${moneyManagement.InitialCapital}
  <#else>
      10000
  </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro elPrintVars>
vars:
	// Internal variables
	<@printStrategyVariables false />
	NumberOfSharesLong(0),NumberOfSharesShort(0),
	tickSize(MinMove/PriceScale),
	OpenOrdersAllowed(true),
	IntPriceLevel(0),IntPriceLevelLong(0),IntPriceLevelShort(0),IntLongSL(0),IntShortSL(0),IntPT(0),
	IntLongTS(0),IntShortTS(0),
	IntLongBE(0),IntShortBE(0),
	InitDone(false),
	LongSLPlaced(false),ShortSLPlaced(false),
   OrderSize(0),
   RemainingSize(0),
   ATMExitPrice(0);

	Array: bool cond[100](false);
   
</#macro>

<#-------------------------------------------------------------->

<#macro elPrintVariable var printInputs>
<#if (printInputs && var.makeExternal == "true") || (!printInputs && var.makeExternal == "false")>
	${var.name}(<#if var.type=="string">"</#if>${var.value}<#if var.type=="string">"</#if>),
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printInput block quoteSymbol=false ><@compress_single_line>
<#list block.* as param>
	<#if param.@controlType == "dataVar">
      <@printDataVar param quoteSymbol />
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printDataVar id quoteSymbol>
   <#if id != "0">
__NBSP1__of Data${id?number+1}
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printParam block paramName defaultValue="NULL"><@compress_single_line>
<#local found=false />
<#list block.* as param>
	<#if param.@key == paramName>
      <@printParameterValue param paramName/>
      <#local found=true />
   </#if>
</#list>
<#if found==false && defaultValue != "NULL">${defaultValue}</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printAllowDuplicateTradesCondition block paramName defaultValue="NULL"><#if getParam(block, paramName) != "true">if(MarketPosition = 0) then </#if></#macro>

<#-------------------------------------------------------------->

<#function getParam block paramName defaultValue="NULL">
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.@variable[0]?? && param.@variable == "true">
      <#elseif param.Formula[0]?? >
      <#else>
        <#return param>
      </#if>
   </#if>
</#list>

<#return defaultValue>
</#function>

<#-------------------------------------------------------------->

<#macro printParameterValue param paramName="NULL">
<#if param.@variable[0]?? && param.@variable == "true">
<@printVariableName param />
<#elseif param.Formula[0]?? >
<@printFormulaKey param.Formula[0] />
<#else>
<#if param?ends_with(".0")>${param?replace(".0", "")}<#else>${param}</#if>
</#if>
</#macro>

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
<#local found = false />
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#local found = true />
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
<#if found == false>${shift}</#if>
</@compress_single_line></#macro>


<#-------------------------------------------------------------->

<#macro printShift block shift><@compress_single_line>[
<@printCIShift block shift />
]</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printShiftAsParam block shift><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#if param.@variable[0]?? && param.@variable == "true">
         <@printVariableName param />
         <#if shift != "0">+${shift}</#if>

      <#elseif param.Formula[0]?? >
         <@printFormulaKey param.Formula[0] />
         <#if shift != "0">+${shift}</#if>

      <#else>
         <#if param == "0">
            ${shift}
         <#else>
            ${param}
         <#if shift != "0">+${shift}</#if>
      </#if>
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>


<#-------------------------------------------------------------->

<#macro printShiftForDaily block shift><@compress_single_line>
<#list block.* as param>
	<#if param.@key == "#Shift#">
      <#if param.@variable[0]?? && param.@variable == "true">
         (<@printVariableName param />)
<#--
puvodne tam bylo variable -1, pak nesedela strategie s a bez variable. Podminka byla HighD crosses above neco
<#if shift != "0">+${shift}-1</#if>
--->
         <#if shift != "0">[${shift}]</#if>

      <#elseif param.Formula[0]?? >
         (<@printFormulaKey param.Formula[0] />)
         <#if shift != "0">[${shift}-1]</#if>

      <#else>
         (${param})
         <#if shift != "0">[${shift}]</#if>
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printVariableName variableName><@compress_single_line>
	<#list doc.StrategyFile.Strategy.Variables.* as var>
		<#if var.id == variableName>${var.name}</#if>
	</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printComputedFromParam block><@compress_single_line>
   <@printParamOptions block "#ComputedFrom#" "0=Close,1=Open,2=High,3=Low,4=MedianPrice,5=TypicalPrice,6=WeightedClose" "Close" />
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printMAType block><@compress_single_line>
   <@printParamOptions block "#Type#" "0=SQ_SMA,1=SQ_EMA,2=SQ_SMMA,3=SQ_LWMA" "SQ_SMA"/>
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

<#macro printOrderDirection block directionParamName buyLabel sellLabel><@compress_single_line>
<#list block.* as param>
	<#if param.@key == directionParamName>
        <#if param?number gt 0>
            ${buyLabel}
        <#else>
            ${sellLabel}
        </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function getOrderDirection block directionParamName>
<#list block.* as param>
	<#if param.@key == directionParamName>
        <#if param?number gt 0>
            <#return 1 />
        <#else>
            <#return -1 />
        </#if>
   </#if>
</#list>
<#return 1 />
</#function>

<#-------------------------------------------------------------->

<#macro printExitMethodInit exitMethodKey block orderType directionParamName priceParamName symbolParamName>
   <#if isOnlineVersion() == "true" >
   	<#include "/MetaTrader4/ExitMethods/"+exitMethodKey+"_init.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_init.tpl">
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

<#-- macro to get SL or PT value from parameters -->
<#macro printSLPTMethod block orderType slptType slptParamName directionParamName priceParamName symbolParamName isExitMethod="false"><@compress_single_line>

<#local price><@printRealPrice block orderType priceParamName isExitMethod /></#local>

<#list block.* as param>
	<#if param.@key == slptParamName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.SLPT.None">
            0
            <#break>

         <#case "SQ.Formulas.SLPT.UseGlobalSLPT">
            sqGetGlobal${slptType}("<@printParam block symbolParamName />", ${price})
            <#break>

         <#case "SQ.Formulas.SLPT.FixedValue">
            <#if isExitMethod == "true">EntryPrice<#else><#if orderType == "MARKET">Close<#else>IntPriceLevel</#if></#if> <#if slptType=="SL"><@printOrderDirection block "#Direction#" "-" "+" /><#else><@printOrderDirection block "#Direction#" "+" "-" /></#if> <@printParameterValue param.Formula[0].Param /> * tickSize
            <#break>

         <#case "SQ.Formulas.SLPT.PctValue">
            <#if isExitMethod == "true">EntryPrice<#else><#if orderType == "MARKET">Close<#else>IntPriceLevel</#if></#if> <#if slptType=="SL"><@printOrderDirection block "#Direction#" "-" "+" /><#else><@printOrderDirection block "#Direction#" "+" "-" /></#if> <@printParameterValue param.Formula[0].Param />/100 * <#if orderType == "MARKET">Close<#else>IntPriceLevel</#if>
            <#break>

         <#case "SQ.Formulas.SLPT.ATRBasedValue">
         		Round2Fraction(<#if isExitMethod == "true">EntryPrice<#else><#if orderType == "MARKET">Close<#else>IntPriceLevel</#if></#if> <#if slptType=="SL"><@printOrderDirection block "#Direction#" "-" "+" /><#else><@printOrderDirection block "#Direction#" "+" "-" /></#if> (<@printParameterValue param.Formula[0].Param[0] /> * SQ_ATR_R(<@printParameterValue param.Formula[0].Param[1] />, 1)))
            <#break>

         <#case "SQ.Formulas.SLPT.ValueFromFormula">
            Round2Fraction(<#if isExitMethod == "true">EntryPrice<#else><#if orderType == "MARKET">Close<#else>IntPriceLevel</#if></#if> <#if slptType=="SL"><@printOrderDirection block "#Direction#" "-" "+" /><#else><@printOrderDirection block "#Direction#" "+" "-" /></#if> SQ_ConvertToRealPips(<@printBlock param.Formula[0]?children[0] />))
            <#break>
         
         <#case "SQ.Formulas.SLPT.PriceLevel">
            Round2Fraction(<@printBlock param.Formula[0]?children[0] />)
            <#break>

        <#default>
            !!Incorrect SL or PT Set!!
      </#switch>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printPrice block orderType priceParamName isExitMethod="false"><@compress_single_line>
<#if orderType == "MARKET">0
<#elseif isExitMethod == "true">EntryPrice
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

<#macro printRealPrice block orderType priceParamName isExitMethod="false"><@compress_single_line>
    <#if orderType == "MARKET">
        Close
    <#else> 
        <@printPrice block orderType priceParamName isExitMethod />
    </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printRangeLevelMethod block paramName ><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.Formula[0].@key?contains(".None")>
            0

      <#elseif param.Formula[0].@key?contains(".FixedValue")>
            <@printParameterValue param.Formula[0].Param /> * tickSize

      <#elseif param.Formula[0].@key?contains(".ATRBasedValue")>
           <@printParameterValue param.Formula[0].Param[0] /> * SQ_ATR_R(<@printParameterValue param.Formula[0].Param[1] /> , 1)

      <#elseif param.Formula[0].@key?contains(".PriceRange")>
           SQ_ConvertToRealPips( <@printBlock param.Formula[0]?children[0] />)

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

<#function isPriceLevelFormula block paramName>
   <#list block.* as param>
	   <#if param.@key == paramName>
         <#return param.Formula[0].@key?contains(".PriceLevel")>
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

<#macro printFormulaKey Formula>
${Formula.@key}
</#macro>

<#-------------------------------------------------------------->

<#macro printFromList block paramName label1="unknown" label2="unknown" label3="unknown" label4="unknown"><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
        <#if param?number == 0>${label1}</#if>
        <#if param?number == 1>${label2}</#if>
        <#if param?number == 2>${label3}</#if>
        <#if param?number == 3>${label4}</#if>
  </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printSubcharts>
   <#local index = 0 />
	<#list doc.StrategyFile.Strategy.Datas.* as data>
      <#if (index > 0) >
//   Subchart${index} = ${data.symbol} / <@printSubchartTF data.timeFrame />;
      <#else>
//   Generated for Main chart = <#if data.symbol == "NULL">Current Symbol<#else>${data.symbol}</#if> / <@printSubchartTF data.timeFrame />;
      </#if>
      <#local index = index + 1 />
	</#list>
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
  <#case "10080">W1<#break>   
  <#case "43200">MN1<#break>     
  <#case "40320">MN1<#break>     
  
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

<#macro printInitialSL block orderType>
<#local sl><@printSLPTMethod block orderType "SL" "#StopLoss.StopLoss#" "#Direction#" "#Price#" "#Symbol#" "true" /></#local>
<#local slVar><@printOrderDirection block "#Direction#" "IntLongSL" "IntShortSL" /></#local>
                  ${slVar} = SQ_ConvertToPips(Absvalue(${sl?replace("* EntryPrice", "* TmpPrice")?replace("EntryPrice", "")?replace("* TmpPrice", "* EntryPrice")}));
                  If MinimumSL > 0 and ${slVar} < MinimumSL Then ${slVar} = MinimumSL;
                  If MaximumSL > 0 and ${slVar} > MaximumSL Then ${slVar} = MaximumSL;

                  If ${slVar} > 0 Then begin
                     SetStopContract;
                     SetStopLoss((MinMove / PriceScale) * BigPointValue * ${slVar});
                  end;
</#macro>

<#-------------------------------------------------------------->

<#macro printInitialPT block orderType>
<#local pt><@printSLPTMethod block orderType "PT" "#ProfitTarget.ProfitTarget#" "#Direction#" "#Price#" "#Symbol#" "true" /></#local>
<#local ptVar>IntPT</#local>
                  ${ptVar} = SQ_ConvertToPips(Absvalue(${pt?replace("* EntryPrice", "* TmpPrice")?replace("EntryPrice", "")?replace("* TmpPrice", "* EntryPrice")}));
                  If MinimumPT > 0 and ${ptVar} < MinimumPT Then ${ptVar} = MinimumPT;
                  If MaximumPT > 0 and ${ptVar} > MaximumPT Then ${ptVar} = MaximumPT;

                  If ${ptVar} > 0 Then begin
                     SetStopContract;
                     SetProfitTarget((MinMove / PriceScale) * BigPointValue * ${ptVar});
                  end;
</#macro>