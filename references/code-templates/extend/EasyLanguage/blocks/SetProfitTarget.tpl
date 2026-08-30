<#assign direction>
    <@printParam block "#Direction#" />
</#assign>

<#assign profitTarget>
    <@printRangeLevelMethod block "#ProfitTarget#" />
</#assign>

<#--Direction: ${direction} --> 
<#--Profit Target: ${profitTarget}-->

<#if profitTarget?contains(" * tickSize") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set PT Fixed Value Long ----------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice + ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?contains(" * tickSize") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set PT Fixed Value Short ---------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice - ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?contains(" * SQ_ATR") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set PT ATR Based Long ------------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice + ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?contains(" * SQ_ATR") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set PT ATR Based Short -----------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice - ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?contains("SQ_ConvertToRealPips") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set PT Range Based Long ----------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice + ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?contains("SQ_ConvertToRealPips") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set PT Range Based Short ---------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice - ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?matches("^-?\\d+(\\.\\d+)?$") && (direction?number == 1)>
//-------------------------- Set PT Price Level Long -----------------------------------------
<#assign stopPrice = profitTarget?number>
if(MarketPosition > 0 && ${stopPrice} > Close) then begin
    Sell Next Bar at ${profitTarget} Limit;
end;
</#if>
<#if profitTarget?matches("^-?\\d+(\\.\\d+)?$") && (direction?number == -1)>
//-------------------------- Set PT Price Level Short ----------------------------------------
<#assign stopPrice = profitTarget?number>
if(MarketPosition < 0 && ${stopPrice} < Close) then begin
	Buy to Cover Next Bar at ${profitTarget} Limit;
end;
</#if>      