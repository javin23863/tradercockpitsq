    Open/Reverse <@printParamOptions block "#Direction#" "1=Long,-1=Short" /> order at Market;
		Size = <@printCloseSizeMethod block />;
		Comment = "<@printParam block "#Comment#" />";
		Duplicate trades = <@printParamOptions block "#AllowDuplicateTrades#" "true=allowed,false=disabled" />; 
        <#if usesATM() >
          <@printATM block />

        <#else>
          <#list ExitMethodsList as exitMethod>
            <@printExitMethodInit exitMethod block "MARKET" "#Direction#" "#Price#" "#Symbol#" />
          </#list>
        </#if>

