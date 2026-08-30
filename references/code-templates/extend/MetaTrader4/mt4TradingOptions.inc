<@tradingOptionsLoad />

<#-------------------------------------------------------------->

<#macro tradingOptionsLoad2>
   <#assign tradingOptions = [] />

   <#assign options = listFiles("/TradingOptions") />

   <#list options as option>
      <#if array_contains(option.@className, tradingOptions) == false && notKnownBuildOption(option.@className) >
         <#assign tradingOptions = tradingOptions + [option.@className] />
      </#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro tradingOptionsLoad>
	<#assign tradingOptions = [] />

	<#list doc.StrategyFile.AvailableTradingOptions.* as option>
      <#if option != "StockpickerOptions" >
		   <#assign tradingOptions = tradingOptions + option />
      </#if>
	</#list>  
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsVariables>
   <#if tradingOptions?size == 0>
int MinimumSL = 0;
int MaximumSL = 0;    
int MinimumPT = 0;  
int MaximumPT = 0;
bool LimitMaxDistanceFromMarketPrice = false;
double MaxDistanceFromMarketPct = 0;
   <#else> 
     <#list tradingOptions as option>
<#if option != "UseInitialSLPT">
        <#if isOnlineVersion() == "true" >
        <#attempt>
            <#include "/MetaTrader4/TradingOptions/"+option+"_variables.tpl">
        <#recover>

// Template for a trading option  /TradingOptions/${option}_variables.tpl missing, ignoring!
        </#attempt>   
        <#else>
        <#attempt>
            <#include "TradingOptions/"+option+"_variables.tpl">
        <#recover>

// Template for a trading option  /TradingOptions/${option}_variables.tpl missing, ignoring!
        </#attempt>   
        </#if>  
</#if>
  	</#list>
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printEmptyTradingOptionClass option>
//----------------------------------------------------------------
// WARNING! Template for a trading option  /TradingOptions/${option}_class.tpl missing, adding empty class:
//----------------------------------------------------------------
class C${option} : public CTradingOption {
      
   public:
      C${option}() {
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         return true;
      }
};
C${option}* obj${option};
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsClasses>
   <#list tradingOptions as option>
<#if option != "UseInitialSLPT">
      <#if isOnlineVersion() == "true" >
        <#attempt>
            <#include "/MetaTrader4/TradingOptions/"+option+"_class.tpl">
        <#recover>
            <@printEmptyTradingOptionClass option />
        </#attempt>   
      <#else>
        <#attempt>
            <#include "TradingOptions/"+option+"_class.tpl">
        <#recover>
            <@printEmptyTradingOptionClass option />
        </#attempt>   
      </#if>
</#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsInit>
   <#list tradingOptions as option>
<#if option != "UseInitialSLPT">
   obj${option} = new C${option}();
</#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsDeinit>
   <#list tradingOptions as option>
<#if option != "UseInitialSLPT">
   delete obj${option};
</#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsHandlers>
   bool continueWithBarUpdate = true;

   <#list tradingOptions as option>
<#if option != "UseInitialSLPT">
   if(!obj${option}.onBarUpdate()) continueWithBarUpdate = false;
</#if>
	</#list>

   return(continueWithBarUpdate);
</#macro>

<#-------------------------------------------------------------->

<#function tradingOptionExists className keyName>
   <#list doc.StrategyFile.TradingOptions.Params.* as option>
      <#if option.@className == className && option.@key == keyName >
         <#return true />
      </#if>
	</#list>
   <#return false />
</#function>

<#-------------------------------------------------------------->

<#macro printTradingOptionBoolValue className keyName><@compress_single_line>
   <#local found = false />
   <#list doc.StrategyFile.TradingOptions.Params.* as option>
      <#if option.@className == className && option.@key == keyName >
         ${option}
         <#local found = true />
      </#if>
	</#list>
   <#if !found>false</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionIntValue className keyName><@compress_single_line>
   <#local found = false />
   <#list doc.StrategyFile.TradingOptions.Params.* as option>
      <#if option.@className == className && option.@key == keyName >
         ${option}
         <#local found = true />
      </#if>
	</#list>
   <#if !found>0</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionTimeValue className keyName><@compress_single_line>
   <#local found = false />

   <#list doc.StrategyFile.TradingOptions.Params.* as option>
      <#if option.@className == className && option.@key == keyName >
         <#local found = true />
         <#local value = option />
      </#if>
	</#list>

   <#if !found>
      08:00
   <#else>
      <@convertSecondsToHHMM value true />
   </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro convertSecondsToHHMM value printColon=false><@compress_single_line>
   <#if value="" || value="NaN">
     Undefined
   <#else>
     <#local hh = value?number / 3600 />
     <#local hh = hh?floor />

     <#local mm = (value?number - (hh * 3600)) / 60 />
	 <#local mm = mm?floor />
     <#if hh < 10>0</#if>${hh}<#if printColon=true>:</#if><#if mm < 10>0</#if>${mm}
   </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#function array_contains value list>
   <#list list as element>
      <#if element == value >
         <#return true />
      </#if>
   </#list>

   <#return false />
</#function>

