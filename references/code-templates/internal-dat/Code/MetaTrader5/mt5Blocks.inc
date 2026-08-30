<#global indicators=[]>
<#global variables=[]>
<#global expressions=[]>
<#global indyIndex=0> 
<#global isBrazilianVersion=doc.StrategyFile.@isBrazilianVersion?contains("true")>
       
<#-------------------------------------------------------------->

<#macro printIndicatorConstants>
    <#list doc.StrategyFile.Strategy.Rules.Events.* as event>
        <#list event.* as rule>
            <#if isNonemptyRule(rule) == true >
              <@findIndicatorsUsedInFunctions rule />
    	     	</#if>
    		</#list>
    </#list>
    <#list doc.StrategyFile.Strategy.Datas.* as data>
       <#if data.symbol == "NULL" || data.symbol == "Current" || data.symbol == "Same as main chart">
          <#local symbol = "NULL" />
       <#else>
          <#local symbol = data.symbol />
       </#if>
       <#local indyId><@compress_single_line><@printDataVar data.id false "_" true /></@compress_single_line></#local>
       <#local indyId = "HEIKENASHI_" + indyId />
       <#local hasHeikenAshi = false />
        <#if doc.StrategyFile.@hasHeikenAshi?contains("true")>
            <#local hasHeikenAshi=true />
        </#if>
       <#local indicatorHandle = 255>
       <#if !indyIsDefined(indyId)>
          <#if hasHeikenAshi>
            <#local indicatorHandle>iCustom(<@compress_single_line><@printDataVar data.id false ", " /></@compress_single_line>, "SqHeikenAshi")</#local>
          </#if>
         <#global indicators = indicators + [{ "id" : indyId, "shortName" : createIndyShortName(indyId), "handle" : indicatorHandle}]>
       </#if>
    </#list>
    <#if doc.StrategyFile.Strategy.GlobalSLPT?? && doc.StrategyFile.Strategy.GlobalSLPT.values?? >   
       <#list doc.StrategyFile.Strategy.GlobalSLPT.values.* as globalValue>
           <#if globalValue?children[0]?? && globalValue?children[0].@type[0]?? && globalValue?children[0].@type[0] == "atr">
              <#local atrPeriod = globalValue?children[0]?children[1] />
              <#local indyId = "ATR_NULL_0_" + atrPeriod />
              <#if !indyIsDefined(indyId)>
                <#local indicatorHandle>iCustom(NULL, 0, "SqATR", ${atrPeriod})</#local>
                <#global indicators = indicators + [{ "id" : indyId, "shortName" : createIndyShortName(indyId), "handle" : indicatorHandle}]>
              </#if>
           </#if> 
       </#list>
    </#if>
    <#if doc.StrategyFile.ATMs[0]?? && doc.StrategyFile.ATMs.ATM[0]??>
        <#list doc.StrategyFile.ATMs.ATM[0].Exits.* as atmExit>
          <#if atmExit.ExitLevels[0]?? && atmExit.ExitLevels.ExitLevel[0]??>
            <#list atmExit.ExitLevels.ExitLevel[0].* as atmExitParam>
              <#if atmExitParam.@key[0]?? && atmExitParam.@key[0] == "AtrPeriod">
                <#local atrPeriod = atmExitParam />
                <#local indyId = "ATR_NULL_0_" + atrPeriod />
                <#if !indyIsDefined(indyId)>
                  <#local indicatorHandle>iCustom(NULL, 0, "SqATR", ${atrPeriod})</#local>
                  <#global indicators = indicators + [{ "id" : indyId, "shortName" : createIndyShortName(indyId), "handle" : indicatorHandle}]>
                </#if>
                <#break>
              </#if>
            </#list>
          </#if>
        </#list>
    </#if>
   <#local index = 0 />
   <#list indicators as indicator>
#define ${indicator.shortName} ${index}     //${indicator.handle}
   <#local index = index + 1 />
   </#list> 
</#macro>        

<#-------------------------------------------------------------->
     
<#macro findIndicatorsUsedInFunctions block><@compress_single_line>
    <#list block?children as c>
      <#if c?node_type == 'element' && c.@key[0]?? && c.@key?starts_with("CBlock_")>
        <@findIndicatorsUsedInFunctions c />
      <#elseif c?node_type == 'element' && (c?node_name == "Block" || c?node_name == "Item") && c.@indicatorName[0]??>
        <#if c.@mt5ExtendedTemplate[0]??>
           <#-- Print this block into local variable just to register indicator IDs and handles -->
           <#local temp><@printBlock c "0" true false /></#local>
        <#else>
          <#local indicatorId = getIndicatorId(c)>
          <#if indicatorId?starts_with("TREND")>
            <#local indicatorId = indicatorId?replace("TREND", "TRSMA")>
            <#if !indyIsDefined(indicatorId)>
              <#local indicatorHandle><@fix_string><@compress_single_line>iMA(<@printInput c />, 200, 0, MODE_SMA, PRICE_CLOSE)</@compress_single_line></@fix_string></#local>
              <#global indicators = indicators + [{ "id" : indicatorId, "shortName" : createIndyShortName(indicatorId), "handle" : indicatorHandle}]>
            </#if>
          <#elseif !indyIsDefined(indicatorId)>
              <#local indicatorHandle><@fix_string><@compress_single_line><@printBlock c "0" false true /></@compress_single_line></@fix_string></#local>
              <#global indicators = indicators + [{ "id" : indicatorId, "shortName" : createIndyShortName(indicatorId), "handle" : indicatorHandle}]>
          </#if>                      
        </#if>          
      <#elseif c?node_type == 'element' && (c?node_name == "Block" || c?node_name == "Item") && c.@cdataIndyName[0]?? >
          <#local indicatorValue><@printParam c "#Value#" /></#local>
          <#local indicatorId = "CI_"+c.@cdataIndyName[0]+"_"+indicatorValue>
          <#local indicatorHandle = c.@mt5 >

          <#if !indyIsDefined(indicatorId)>
            <#global indicators = indicators + [{ "id" : indicatorId, "shortName" : indicatorId, "handle" : indicatorHandle}]>
          </#if>

      <#elseif c?node_name == "Formula" && c.@key[0]?ends_with("ATRBasedValue")>
          <#local atrPeriod><@printATRPeriodFromFormula c /></#local>
          <#local indicatorId = "ATR_NULL_0_" + atrPeriod>
          <#local indicatorHandle>iCustom(NULL, 0, "SqATR", ${atrPeriod})</#local>
          
          <#if !indyIsDefined(indicatorId)>
            <#global indicators = indicators + [{ "id" : indicatorId, "shortName" : createIndyShortName(indicatorId), "handle" : indicatorHandle}]>
          </#if>
      <#else>
          <@findIndicatorsUsedInFunctions c />
      </#if>
   </#list> 
</@compress_single_line></#macro>        
            
<#-------------------------------------------------------------->
                                    
<#macro printIndicatorsInitialization>
   <#list indicators as indicator>
   ArrayResize(indicatorHandles, ArraySize(indicatorHandles) + 1, 10);
   indicatorHandles[${indicator.shortName}] = ${indicator.handle};
   
   </#list>
</#macro>

<#-------------------------------------------------------------->
  
<#function indyIsDefined indyId>
   <#list indicators as indicator>
      <#if indicator.id == indyId>
        <#return true>
      </#if>
   </#list>
   <#return false>
</#function>          

<#-------------------------------------------------------------->
  
<#function createIndyShortName fullName>
  <#local indyBaseName = fullName?split("_")[0]> 
  
  <#local index = 1>
  <#list indicators as indicator>
    <#if indicator.id?starts_with(indyBaseName) >
       <#local index = index + 1>
    </#if>
  </#list>
  <#return indyBaseName + "_" + index>
</#function>

<#-------------------------------------------------------------->
  
<#function getIndyShortName indyId>
  <#local all = "">
  <#list indicators as indicator>
    <#local all = all + indicator.id + ", ">
    <#if indicator.id == indyId>
       <#return indicator.shortName>
    </#if>
  </#list>
  <#return "!!! " + indyId + " not found in [" + all + "]">
</#function> 

<#-------------------------------------------------------------->

<#function getIndicatorId block params=[] iFunctionName="" iCustomIndicatorName="">
  <#local name = block.@indicatorName[0]?upper_case + "_" + printIndyInput(block) >

  <#if (params?size > 0)>
    <#local indicatorHandle><@fix_string><@compress_single_line>${iFunctionName}(<@printInput block />, '<#if iCustomIndicatorName != "">${iCustomIndicatorName}<#else>${block.@indicatorName[0]}</#if>'</@compress_single_line></@fix_string></#local>
    
    <#list params as param>
      <#if param?starts_with("#")>
        <#if param?contains("ComputedFrom")>
          <#local paramValue><@printComputedFromParam block /></#local> 
        <#else>
          <#local paramValue><@printParam block param /></#local> 
        </#if>
      <#else>
        <#local paramValue=param />
      </#if>
      
      <#local name = name + "_" + paramValue>
      <#local indicatorHandle = indicatorHandle + ", " + paramValue>
    </#list>

    <#local indicatorHandle = indicatorHandle + ")">

    <#if !indyIsDefined(name)>
      <#global indicators = indicators + [{ "id" : name, "shortName" : createIndyShortName(name), "handle" : indicatorHandle}]>
    </#if>
  <#else>
    <#list block.* as param>
      <#if param.@key[0]?? && param.@key[0] != "#Level#" && param.@key[0] != "#Line#" && param.@key[0] != "#Shift#" && (!param.@controlType?? || param.@controlType != "dataVar")>  
        <#local paramValue><@printParameterValue param /></#local> 
        <#local name = name + "_" + paramValue>
      </#if>                       
    </#list> 
  </#if>
  
  <#return name>
</#function> 

<#-------------------------------------------------------------->

<#macro printHeikenAshiCases>
  <#local index = 0 />
  <#list doc.StrategyFile.Strategy.Datas.* as data>
    <#if (index > 0) >
      if(symbol == correctSymbol(Subchart${data.id}Symbol) && timeframe == TFMigrate(Subchart${data.id}Timeframe)){
         return ${getIndyShortName("HEIKENASHI_Subchart" + data.id)};
      }
    </#if>
    <#local index = index + 1 />
  </#list>      
</#macro>     
                 
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
           
<#-------------------------------------------------------------->

<#macro printFiboCases>
    <#list indicators as indicator>
         <#if indicator.id?starts_with("FIBO") >
    if(indyIndex == ${indicator.shortName}) return true;
	     	</#if>
		</#list>       
</#macro>
                 
<#-------------------------------------------------------------->

<#macro printATRIndicatorName block><@compress_single_line>
<#local period><@printATRPeriodFromFormula block /></#local>
<#local indyId = "ATR_NULL_0_" + period>
${getIndyShortName(indyId)}
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printATRPeriodFromFormula block><@compress_single_line>
  <#list block?children as param>
    <#if param.@key[0]?? && param.@key[0] == "#AtrPeriod#"><@printParameterValue param /></#if>
  </#list> 
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printValuesUsedInExitMethods functionName printType>
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
      <#list event.* as rule>
         <#if isNonemptyRule(rule) == true >
<@findValuesUsedInExitMethods rule functionName printType/>
	     	</#if>
		</#list>
	</#list>
  <#if usesATM()>
<@printATMExitValues />
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro findValuesUsedInExitMethods block functionName printType>
   <#list block?children as c>
      <#if c?node_type == 'element' && c?node_name == "Param">
         <#if !c.@key?contains(".None") && c.Formula[0]?? && c.@exitMethod[0]??>
            <#local hash><@compress_single_line><@escape_string><@printRangeLevelMethod block c.@key "key" /></@escape_string></@compress_single_line></#local> 
            <#if hash != "0">
   if(idHash == sqStringHash("${hash}")) {
      return (<@compress_single_line><@printRangeLevelMethod block c.@key printType /></@compress_single_line>);
   }
            </#if>
         </#if>
      <#else>
         <@findValuesUsedInExitMethods c functionName printType />
      </#if>
   </#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printRulesForEvent eventName>
	<#list doc.StrategyFile.Strategy.Rules.Events.* as event>
  		<#if event.@key == eventName>
            <#list event.* as rule>
				<#if isNonemptyRule(rule) == true >
<@printRule rule eventName/>

    			</#if>
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
<#if rule.@type == "IfThen">
   if (<#if notBarOpen><#else>_sqIsBarOpen == true
      && </#if><#list rule.If.* as block><@printBlock block "0" true /></#list>)
   {
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	  <@printBlock block "0" true />
	  <#local count=count+1>


	</#list>
  }
<#elseif rule.@type == "IfThenElse">
  <#if notBarOpen><#else>if(_sqIsBarOpen == true){</#if>

   if(<#list rule.If.* as block><@printBlock block "0" true /></#list>)
   {
      
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	  <@printBlock block "0" true />
	  <#local count=count+1>

	</#list>
   } else {
 	<#local count=1>
	<#list rule.Else.* as block>
      // Action #${count}
	  <@printBlock block "0" true />
	  <#local count=count+1>


	</#list>
   }
  <#if notBarOpen><#else>}</#if>
<#elseif rule.@type == "ActionOnly">
  <#if notBarOpen><#else>if(_sqIsBarOpen == true){</#if>
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
      <@printBlock block "0" true />
	  <#local count=count+1>


	</#list>
  <#if notBarOpen><#else>}</#if>
<#elseif rule.@type == "ForEachTrade">
   if <#list rule.If.* as block><@printBlock block /></#list>
   {
	<#local count=1>
	<#list rule.Then.* as block>
      // Action #${count}
	  <@printBlock block "0" true />
	  <#local count=count+1>


	</#list>
   }
<#elseif rule.@type == "Signal">
   <#if notBarOpen>
    // init signals on every tick   
	<#list rule.signals.* as signal>
   <@printVariableName signal.@variable /> = <#if signal?children[0]??><@printBlock signal?children[0] /><#else>false</#if>;

	</#list>
   <#else>if (_sqIsBarOpen == true) {
     // init signals only on bar open 
	<#list rule.signals.* as signal>
     <@printVariableName signal.@variable /> = <@compress_single_line><#if signal?children[0]??><@printBlock signal?children[0] /><#else>false</#if></@compress_single_line>;

	</#list>
   }
   </#if>
<#elseif rule.@type == "SignalFuzzy">
   <#if notBarOpen>
    // init signals on every tick   
	<#list rule.signals.* as signal>
   <@printFuzzyVariable signal "   " />

	</#list>
   <#else>if (_sqIsBarOpen == true) {
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
${space}cond[${index}] = <@compress_single_line><@printBlock item /></@compress_single_line>;
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

<#macro printBlockChild block childName shift="0" isRule=false>
  <@compress_single_line>
     <#list block?children as c>
        <#if c?node_name == "Block" && c.@key == childName>
  	      <@printBlock c shift isRule />   
     	  </#if>
     </#list>
  </@compress_single_line>
</#macro>

<#-------------------------------------------------------------->

<#macro printBlock block shift="0" isRule=true isIndyInit=false doEscape=false>
<#if block?node_name == "Block">
  <@printBlock block?children[0] shift isRule isIndyInit doEscape />
<#else>
   <#if block.@key=="#Value#" || block.@key=="#Left#" || block.@key=="#Right#">
   !! Undefined Value !!
   <#else>
      <#if isIndyInit == true >
        <#local key = block.@indicatorName[0]>
      <#else>
         <#local key = block.@key[0]>
         <#if key?starts_with("CDataIndy") >
           <#local key = "CDataIndy" />
         <#elseif key?starts_with("CBlock") >
           <#local key = "CBlock" />
         </#if>      
      </#if>
      <#if blockFileExists(key) >
         <@escape doEscape>
             <#if isRule == true && isSimpleIndicator(block) == true >
          sqGetIndicatorValue(${getIndyShortName(getIndicatorId(block))}, <@printIndicatorBufferIndex block />, <@printParam block "#Shift#" "0" />) 
             <#else>
                 <#if block.@customIndicator[0]??>
                    <@printCustomIndicator block shift />
                 <#else>
                    <#if isOnlineVersion() == "true" >
                       <#include "/MetaTrader5/blocks/"+key+".tpl">
                    <#else>
                       <#if block.@key??>
                          <#include "blocks/"+key+".tpl">
                       </#if>
                    </#if>
                 </#if>
              </#if>
         </@escape>
     <#else>
       !!! Template file /blocks/${key}.tpl missing for block '${block.@key} ' !!!
     </#if>
   </#if>
</#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printCustomIndicator block shift="0">
<@compress_single_line>
//double iCustom( string symbol, int timeframe, string name, ..., int mode, int shift)

iCustom(<@printInputCI block />, "${block.@key}", <@printShift block shift />)
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

<#function isSimpleIndicator block>
  <#return block.@indicatorName[0]?? && block.@key[0] == block.@indicatorName[0] >
</#function>

<#-------------------------------------------------------------->

<#macro printIndicatorExpression block childKey="0"><@compress_single_line>    
  <#local ret=false />
  <#list block.* as child>
    <#if child.@key[0]?? && !ret && (childKey == "0" || child.@key[0] == childKey)> 
       <#local ret=true />
       <#if child?children[0].@indicatorName[0]?? > 
          <#assign expression>sqGetIndicatorValue(${getIndyShortName(getIndicatorId(child?children[0]))}, <@printIndicatorBufferIndex child?children[0] />, <@printShift child?children[0] "0" true />)</#assign>
       <#else>
          <#assign expression><@printBlock child?children[0] "0" true false /></#assign>
       </#if>
       <#if !expressions?seq_contains(expression)>
          <#global expressions = expressions + [expression]>
       </#if>
       "<@compress_single_line><@printBlock child?children[0] "0" true false true /></@compress_single_line>"
    </#if>
  </#list> 
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printNestedIndicatorName block childKey="0">
  <#compress>
  <#local ret=false />
  
  <#list block.* as param>
	<#if param.@key[0]?? && !ret && (childKey == "0" || param.@key[0] == childKey)>
    <#local ret=true />
      <#if param?children[0]?? && param?children[0].@indicatorName[0]?? >
      ${getIndyShortName(getIndicatorId(param?children[0]))}
      <#else>
        <#assign expression><@printBlock param?children[0] "0" true false /></#assign>
        <#if !expressions?seq_contains(expression)>
            <#global expressions = expressions + [expression]>
        </#if>
        "<@compress_single_line><@printBlock param?children[0] "0" true false true /></@compress_single_line>"
      </#if>
  </#if>
  </#list> 
  </#compress>
</#macro>
  
<#-------------------------------------------------------------->

<#macro printNestedIndicatorShift block childKey="0">
  <#compress>
  <#local ret=false />
  <#list block.* as param>
	<#if param.@key[0]?? && !ret && (childKey == "0" || param.@key[0] == childKey) && param?children[0]?? && param?children[0].@indicatorName[0]??>
    <#local ret=true />
    <@printShift param?children[0] "0" true />
  </#if>
  </#list> 
  <#if ret == false>
    0
  </#if>
  </#compress>
</#macro>
      
<#-------------------------------------------------------------->

<#macro printNestedIndicatorBuffer block childKey="0">
  <#compress>
  <#local ret=false />
  <#list block.* as param>
	<#if param.@key[0]?? && !ret && (childKey == "0" || param.@key[0] == childKey) && param?children[0]?? && param?children[0].@indicatorName[0]??>
    <#local ret=true />
    <@printIndicatorBufferIndex param?children[0] />
  </#if>
  </#list> 
  <#if ret == false>
    0
  </#if>
  </#compress>
</#macro>

<#-------------------------------------------------------------->

<#macro printIndicatorBufferIndex block>
  <#compress>
  <@printParam block "#Line#" "0" /><#if block.@indicatorName[0]?? && block.@indicatorName[0] == "BollingerBands"> + 1</#if>
  </#compress>
</#macro>

<#-------------------------------------------------------------->

<#macro printModifiedExpression expression>   
  <@compress_single_line>
  <#local count = 0 /> 
  <#local disableShift = false />
  <#list expression?split(",") as part>
    <#if (part?lower_case?index_of("fibo") >= 0 || part?lower_case?index_of("session") >= 0)><#local disableShift = true /></#if>
    <#if count == 0 >
        return ${part}
    <#else>
        <#if (part?index_of(")") > 0)>
            <#local bareValue = part?replace(" ", "")?split(")")[0] />
            <#if !disableShift && (isNumber(bareValue) || isIntVariable(bareValue) == true) >
                , shift +${part}                    
            <#else> 
                ,${part}  
            </#if>
            <#local disableShift = false />
        <#else>
            ,${part}
        </#if>
    </#if>        
    <#local count++ />
  </#list>
  </@compress_single_line>
</#macro>

<#-------------------------------------------------------------->

<#macro escape enable=true>
  <#local captured><#nested></#local>
  <#if enable == false>${captured}
  <#else>
      <#local index = 0 /> 
      <#list captured?split("\"") as part>
          <#if (part?length > 0)>
              <#if index == 0>${part}
              <#else>\"${part}
              </#if>
              <#local index++ />
          </#if>
      </#list>
  </#if>
</#macro>
     
<#-------------------------------------------------------------->

<#macro printMAMethod>
   <#local captured><#nested></#local>
   <@compress_single_line>
     <#if captured == "0"> MODE_SMA
     <#elseif captured == "1"> MODE_EMA
     <#elseif captured == "2"> MODE_SMMA
     <#elseif captured == "3"> MODE_LWMA
     <#else> MODE_SMA
     </#if>
   </@compress_single_line>
</#macro>
   
<#-------------------------------------------------------------->

<#macro printStochPriceField>
   <#local captured><#nested></#local>
   <@compress_single_line>
     <#if captured == "1"> STO_CLOSECLOSE
     <#else> STO_LOWHIGH
     </#if>
   </@compress_single_line>
</#macro>
   
<#-------------------------------------------------------------->

<#macro printStochLine>
   <#local captured><#nested></#local>
   <@compress_single_line>
     <#if captured == "0"> 1
     <#else> 0
     </#if>
   </@compress_single_line>
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


