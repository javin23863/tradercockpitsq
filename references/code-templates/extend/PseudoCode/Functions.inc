<#-------------------------------------------------------------->

<#macro replaceBr>
<#local captured><#nested></#local>
${ captured?replace("__BR__", "\n   ") }
</#macro>

<#-------------------------------------------------------------->

<@tradingOptionsLoad />

<#-------------------------------------------------------------->

<#macro tradingOptionsLoad>
	<#assign tradingOptions = [] />

	<#list doc.StrategyFile.AvailableTradingOptions.* as option>
		<#assign tradingOptions = tradingOptions + option />
	</#list>  
</#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptions>
   <#local stockPickerOptions = ["StockpickerOptions"] />
   <#list tradingOptions as option>
      <#if (getOptions("BacktestEngine") != "Stockpicker" && stockPickerOptions?seq_contains(option) == false) || (getOptions("BacktestEngine") == "Stockpicker" && stockPickerOptions?seq_contains(option))>
         <#if isOnlineVersion() == "true" >
            <#attempt>
                  <#include "/PseudoCode/TradingOptions/"+option+"_variables.tpl">
            <#recover>

// Template for a trading option ${option} missing, ignoring!
            </#attempt>      
         <#else>
            <#attempt>
                  <#include "TradingOptions/"+option+"_variables.tpl">
            <#recover>

// Template for a trading option ${option} missing, ignoring!
         </#attempt>      
         </#if>
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
         <#attempt>
            ${option}
         <#recover>
            ${option.@value}
         </#attempt>
         <#local found = true />
      </#if>
	</#list>
   <#if !found>0</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printTradingOptionIntOption className keyName paramOptions defaultValue="0"><@compress_single_line>
   <#local found = false />
   <#list doc.StrategyFile.TradingOptions.Params.* as option>
      <#if option.@className == className && option.@key == keyName >
<@printOption option paramOptions defaultValue/>
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
