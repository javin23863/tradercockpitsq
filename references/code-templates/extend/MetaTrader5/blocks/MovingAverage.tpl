<#assign MAMethodInt>
    <@printParam block "#MAMethod#" />
</#assign>
<#assign TypeInt>
    <@printParam block "#Type#" />
</#assign>

<#if MAMethodInt?? && MAMethodInt?has_content>
    <#assign MAMethod>
        <@printMAMethod><@printParam block "#MAMethod#" /></@printMAMethod>
    </#assign>
</#if>

<#if TypeInt?? && TypeInt?has_content>
    <#assign MAMethod>
        <@printMAMethod><@printParam block "#Type#" /></@printMAMethod>
    </#assign>
</#if>

iMA(<@printInput block />, <@printParam block "#Period#" />, 0, ${MAMethod}, <@printComputedFromParam block />)