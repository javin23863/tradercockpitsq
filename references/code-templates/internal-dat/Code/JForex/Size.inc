
<#-------------------------------------------------------------->

<#macro printMoneyManagementVariables>
   <#if isOnlineVersion() == "true" >
      <#include "/JForex/MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_variables.tpl">
   <#else>
   	  <#include "MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_variables.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printExitMethodInit exitMethodKey block orderType directionParamName priceParamName symbolParamName>
   <#if isOnlineVersion() == "true" >
   	<#include "/JForex/ExitMethods/"+exitMethodKey+"_init.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_init.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printExitMethodManage exitMethodKey>
   <#if isOnlineVersion() == "true" >
   	<#include "/JForex/ExitMethods/"+exitMethodKey+"_manage.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_manage.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printExitMethodReset exitMethodKey>
   <#if isOnlineVersion() == "true" >
   	<#include "/JForex/ExitMethods/"+exitMethodKey+"_reset.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_reset.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printExitMethodInclude exitMethodKey>
   <#if isOnlineVersion() == "true" >
   	<#include "/JForex/ExitMethods/"+exitMethodKey+"_include.tpl">
   <#else>
   	<#include "ExitMethods/"+exitMethodKey+"_include.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printMoneyManagementClass>
   <#if isOnlineVersion() == "true" >
      <#include "/JForex/MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_class.tpl">
   <#else>
   	  <#include "MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_class.tpl">
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

<#macro printMMVariableBoolValue className keyName><@compress_single_line>
   <#local found = false />
   <#list doc.StrategyFile.MoneyManagement.Method.Params.* as param>
      <#if param.@key == keyName >
         ${param}
         <#local found = true />
      </#if>
	</#list>
   <#if !found>false</#if>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printCloseSizeMethod block paramName><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.CloseSize.FullPosition">
            -1
            <#break>

         <#case "SQ.Formulas.Size.UseGlobalMM">
            -1
            <#break>
            
         <#case "SQ.Formulas.CloseSize.Percentage">
            (${param.Formula[0].Param}/100.0) * order.getAmount() * 10
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFromVariable">
            <@printVariableName param.Formula[0].Param />
            <#break>

         <#case "SQ.Formulas.CloseSize.UseFormula">
            <@printBlock param.Formula[0]?children[0] />
            <#break>

         <#case "SQ.Formulas.Size.DefineOwnSize">
            <@printParameterValue param.Formula[0].Param />
            <#break>

        <#default>
            This will be processed if it is neither
      </#switch>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printSizeMethod block orderType sizeParamName directionParamName priceParamName slParamName symbolParamName>
<#list block.* as param>
	<#if param.@key == sizeParamName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.Size.DefineOwnSize">
<@printParameterValue param.Formula[0].Param />
            <#break>

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
   	<#include "/JForex/moneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_method.tpl">
   <#else>
   	<#include "MoneyManagement/"+doc.StrategyFile.Strategy.MoneyManagement.@type+"_method.tpl">
   </#if>
</#macro>

<#-------------------------------------------------------------->

<#macro printGlobalSLPTMethod block orderType slptType slptParamName directionParamName priceParamName>
   <#local tagToSearch="global" />
   <#if doc.StrategyFile.Strategy.GlobalSLPT.useSameSLPTforBothDirections=="true">

   <#else>

   </#if>
   <#assign globalSLPT=doc.StrategyFile.Strategy.GlobalSLPT />
   <#assign slptType=slptType />
	<#include "slpt/"+doc.StrategyFile.Strategy.GlobalSLPT.@type+".tpl">
</#macro>

<#-------------------------------------------------------------->

<#macro printPrice block orderType priceParamName isExitMethod="false"><@compress_single_line>
<#if orderType == "MARKET">0
<#elseif isExitMethod == "true">order.getOpenPrice()
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

<#function isComputedMMMethod block paramName>
<#list block.* as param>
	<#if param.@key == paramName>
      <#switch param.Formula[0].@key>
         <#case "SQ.Formulas.Size.DefineOwnSize">
            <#return false />
         <#case "SQ.Formulas.Size.UseFromVariable">
            <#return false />
         <#case "SQ.Formulas.Size.UseFormula">
            <#return false />
         <#case "SQ.Formulas.Size.UseGlobalMM">
            <#if doc.StrategyFile.Strategy.MoneyManagement.@type == "MM_NONE" >
               <#return true />
            </#if>
            <#break>

        <#default>
            <#return false />
      </#switch>
   </#if>
</#list>

<#return true />
</#function>

<#-------------------------------------------------------------->

<#macro printRangeLevelMethod block paramName printType><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.Formula[0].@key?contains(".None")>
            0

      <#elseif param.Formula[0].@key?contains(".FixedValue")>
            <#if printType =="key">
               <@printParameterValue param.Formula[0].Param />
            <#else>
               sqConvertToRealPips(instrument, <@printParameterValue param.Formula[0].Param />)
            </#if>

      <#elseif param.Formula[0].@key?contains(".PctValue")>
            0
      <#elseif param.Formula[0].@key?contains(".ATRBasedValue")>
           <@printParameterValue param.Formula[0].Param[0] /> * sqATR("Current",0,<@printParameterValue param.Formula[0].Param[1] />,1)

      <#elseif param.Formula[0].@key?contains(".PriceRange")>
           sqConvertToRealPips(instrument, <@printBlock param.Formula[0]?children[0] />)

      <#elseif param.Formula[0].@key?contains(".PriceLevel")>
           <@printBlock param.Formula[0]?children[0] />

      <#elseif param.Formula[0].@key?contains(".ValueFromFormula")>
           <#if printType =="key">
               <@fix_string><@printBlock param.Formula[0]?children[0] /></@fix_string>
            <#else>
               <@printBlock param.Formula[0]?children[0] />
            </#if>
             
           
      <#else>
         !!Incorrect SL or PT Set ${param.Formula[0].@key}!!
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>

<#-------------------------------------------------------------->

<#macro printRangeLevelMethodType block paramName printType><@compress_single_line>
<#list block.* as param>
	<#if param.@key == paramName>
      <#if param.Formula[0].@key?contains(".None")>
            SLPTTYPE_LEVEL

      <#elseif param.Formula[0].@key?contains(".FixedValue")>
            SLPTTYPE_RANGE

      <#elseif param.Formula[0].@key?contains(".PctValue")>
            SLPTTYPE_LEVEL

      <#elseif param.Formula[0].@key?contains(".ATRBasedValue")>
            SLPTTYPE_RANGE

      <#elseif param.Formula[0].@key?contains(".PriceRange")>
            SLPTTYPE_RANGE

      <#elseif param.Formula[0].@key?contains(".PriceLevel")>
            SLPTTYPE_LEVEL
            
      <#elseif param.Formula[0].@key?contains(".ValueFromFormula")>
            SLPTTYPE_RANGE
           
      <#else>
         !!Incorrect SL or PT Set ${param.Formula[0].@key}!!
      </#if>
   </#if>
</#list>
</@compress_single_line></#macro>
