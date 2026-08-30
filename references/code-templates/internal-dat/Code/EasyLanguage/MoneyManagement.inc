<@moneyManagementLoad />

<#-------------------------------------------------------------->

<#macro moneyManagementLoad>
  <#if doc.StrategyFile.Strategy.MoneyManagement[0]??> 
		<#assign moneyManagement = doc.StrategyFile.Strategy.MoneyManagement />
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#function hasMoneyManagementVariables>
   <#return doc.StrategyFile.Strategy.MoneyManagement[0]?? > 
</#function>

<#-------------------------------------------------------------->

<#macro printMoneyManagementVariables>
   <#if isOnlineVersion() == "true" >
      <#include "/MetaTrader4/MoneyManagement/"+moneyManagement.@type+"_variables.tpl">
   <#else>
      <#include "MoneyManagement/"+moneyManagement.@type+"_variables.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printMMVariableNumber paramKey><@compress_single_line>
   <#list doc.StrategyFile.Strategy.MoneyManagement.Method.Params.* as param>
      <#if param.@key == paramKey >
         ${param}
      </#if>
	</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printMMSizeNumber paramKey><@compress_single_line>
   <#local found = false />
   <#list doc.StrategyFile.Strategy.MoneyManagement.Method.Params.* as param>
      <#if param.@key == paramKey >
         <#if param?contains("0.")>1<#else>${param}</#if>
         <#local found = true />
      </#if>
	</#list>
   <#if !found>1</#if>
</@compress_single_line></#macro>
