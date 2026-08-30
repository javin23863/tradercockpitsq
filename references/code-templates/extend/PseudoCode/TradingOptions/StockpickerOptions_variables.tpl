Entry type: <@printTriggerType doc.StrategyFile.Strategy.@entryTriggeredAt />
Exit type: <@printTriggerType doc.StrategyFile.Strategy.@exitTriggeredAt />
Broker: <@printTradingOptionIntValue "BrokerOption" "PickerBroker" />

<#macro printTriggerType type><@compress_single_line>
<#if type == "OnBarOpen">
  On Bar Open
<#else>
  <#if type == "OnBarClose">
    On Bar Close
  <#else>
    <#if type == "BeforeBarOpen" || type == "AfterBarClose">
      Before Bar Open
    <#else>
      ${type}
    </#if>
  </#if>
</#if>
</@compress_single_line></#macro>