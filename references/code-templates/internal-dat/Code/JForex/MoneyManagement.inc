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
