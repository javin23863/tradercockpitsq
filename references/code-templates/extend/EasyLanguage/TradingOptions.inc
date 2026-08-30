<@tradingOptionsLoad />

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

<#function hasTradingOptionsVariables>
   <#list tradingOptions as option>
      <#return true >
	</#list>
  <#return false >
</#function>

<#-------------------------------------------------------------->

<#macro printTradingOptionsVariables>
   <#list tradingOptions as option>
      <#if isOnlineVersion() == "true" >
        <#attempt>
            <#include "/EasyLanguage/TradingOptions/"+option+"_variables.tpl">
        <#recover>

    // Template for a trading option  /TradingOptions/${option}_variables.tpl missing, ignoring!
        </#attempt>      
      <#else>
        <#attempt>
            <#include "TradingOptions/"+option+"_variables.tpl">
        <#recover>

    // Template for a trading option /TradingOptions/${option}_variables.tpl missing, ignoring!
        </#attempt>      
      </#if>
	</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionsCodes>
   <#list tradingOptions as option>
      <#if isOnlineVersion() == "true" >
        <#attempt>
   	  <#include "/EasyLanguage/TradingOptions/"+option+"_code.tpl">
        <#recover>

// Template for a trading option /TradingOptions/${option}_code.tpl missing, ignoring!
        </#attempt>      
      <#else>
        <#attempt>
   	  <#include "TradingOptions/"+option+"_code.tpl">
        <#recover>

// Template for a trading option /TradingOptions/${option}_code.tpl missing, ignoring!
        </#attempt>      
      </#if>
	</#list>
    
    
</#macro>

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
      0800
   <#else>
      <@convertSecondsToHHMM value />
   </#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro convertSecondsToHHMM value><@compress_single_line>
   <#if value="" || value="NaN">
     Undefined
   <#else>
     <#local hh = value?number / 3600 />
     <#local hh = hh?floor />

     <#local mm = (value?number - (hh * 3600)) / 60 />
	 <#local mm = mm?floor />
     <#if hh < 10>0</#if>${hh}<#if mm < 10>0</#if>${mm}
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

