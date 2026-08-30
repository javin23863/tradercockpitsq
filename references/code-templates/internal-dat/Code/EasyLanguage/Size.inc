
<#-------------------------------------------------------------->

<#macro printSizeMethod block orderType sizeParamName directionParamName priceParamName slParamName symbolParamName>
<#list block.* as param>
	<#if param.@key == sizeParamName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.Size.DefineOwnSize">
<@printParameterValue param.Formula[0].Param /><#break>

         <#case "SQ.Formulas.Size.UseGlobalMM">
            <@printGlobalMMMethod block orderType directionParamName priceParamName slParamName symbolParamName />
            <#break>
         <#case "SQ.Formulas.Size.UseFromVariable">
<@compress_single_line><@printVariableName param.Formula[0].Param /></@compress_single_line><#break>

         <#case "SQ.Formulas.Size.UseFormula">
<@compress_single_line><@printBlock param.Formula[0].Param?children[0] /></@compress_single_line><#break>

        <#default>
            !!Incorrect Size Set!!
      </#switch>
   </#if>
</#list>
</#macro>

<#-------------------------------------------------------------->

<#macro printGlobalMMMethod block orderType directionParamName priceParamName slParamName symbolParamName>
   <#assign mm=doc.StrategyFile.Strategy.MoneyManagement />
   <#if isOnlineVersion() == "true" >
   	<#include "/EasyLanguage/MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_method.tpl">
   <#else>
   	<#include "MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_method.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printCloseSizeMethod block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.CloseSize.FullPosition">
            OrderLots()
            <#break>

         <#case "SQ.Formulas.Size.UseGlobalMM">
            OrderLots()
            <#break>
            
         <#case "SQ.Formulas.CloseSize.Percentage">
            (${param.Formula[0].Param}/100.0) * OrderLots()
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFromVariable">
            <@printVariableName param.Formula[0].Param />
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFormula">
            <@printBlock param.Formula[0]?children[0] />
            <#break>

         <#case "SQ.Formulas.Size.DefineOwnSize">
<@printParameterValue param.Formula[0].Param /><#break>

        <#default>
            This will be processed if it is neither
      </#switch>
   </#if>
</#list>
</@compress_single_line></#macro>