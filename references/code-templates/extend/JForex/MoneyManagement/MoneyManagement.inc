<@moneyManagementLoad />

<#-------------------------------------------------------------->

<#macro moneyManagementLoad>
  <#if doc.StrategyFile.Strategy.MoneyManagement??> 
		<#assign moneyManagement = doc.StrategyFile.Strategy.MoneyManagement />
  </#if>
</#macro>

<#-------------------------------------------------------------->

<#function hasMoneyManagementVariables>
   <#return doc.StrategyFile.Strategy.MoneyManagement?? > 
</#function>

<#-------------------------------------------------------------->

<#macro printMoneyManagementVariables>
   <#if isOnlineVersion() == "true" >
      <#include "/JForex/MoneyManagement/"+moneyManagement.@type+"_variables.tpl">
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
   <#list doc.StrategyFile.Strategy.MoneyManagement.Method.Params.* as param>
      <#if param.@key == paramKey >
         <#if param?contains(".")>1<#else>${param}</#if>
      </#if>
	</#list>
</@compress_single_line></#macro>
