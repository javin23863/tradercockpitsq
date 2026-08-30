<#assign direction>
    <@printParam block "#Direction#" />
</#assign>
<#assign stopLoss>
    <@printRangeLevelMethod block "#StopLoss#" />
</#assign>
<#--Direction: ${direction} --> 
<#--Stop Loss: ${stopLoss}  --> 

<#if stopLoss?contains(" * tickSize") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set SL Fixed Value Long ----------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice - ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?contains(" * tickSize") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set SL Fixed Value Short ---------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice + ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?contains(" * SQ_ATR") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set SL ATR Based Long ------------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice - ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?contains(" * SQ_ATR") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set SL ATR Based Short -----------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice + ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?contains("SQ_ConvertToRealPips") && (direction?number == 1 || direction?number == 0)>
//--------------------------- Set SL Range Based Long ----------------------------------------
if(MarketPosition > 0) then begin
   Sell Next Bar at EntryPrice - ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?contains("SQ_ConvertToRealPips") && (direction?number == -1 || direction?number == 0)>
//--------------------------- Set SL Range Based Short ---------------------------------------
if(MarketPosition < 0) then begin
    Buy to Cover Next Bar at EntryPrice + ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?matches("^-?\\d+(\\.\\d+)?$") && (direction?number == 1)>
//-------------------------- Set SL Price Level Long -----------------------------------------
<#assign stopPrice = stopLoss?number>
if(MarketPosition > 0 && ${stopPrice} < Close) then begin
    Sell Next Bar at ${stopLoss} Stop;
end;
</#if>
<#if stopLoss?matches("^-?\\d+(\\.\\d+)?$") && (direction?number == -1)>
//-------------------------- Set SL Price Level Short ----------------------------------------
<#assign stopPrice = stopLoss?number>
if(MarketPosition < 0 && ${stopPrice} > Close) then begin
	Buy to Cover Next Bar at ${stopLoss} Stop;
end;
</#if>      