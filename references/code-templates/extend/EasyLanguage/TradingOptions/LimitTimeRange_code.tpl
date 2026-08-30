

// Limit time range
if(OpenOrdersAllowed = True and LimitSignalsTimeRange = True and ((SignalTimeRangeFrom < SignalTimeRangeTo and (Time < SignalTimeRangeFrom or Time >= SignalTimeRangeTo)) or 
									   (SignalTimeRangeFrom >= SignalTimeRangeTo and (Time < SignalTimeRangeFrom and Time >= SignalTimeRangeTo)))
) then OpenOrdersAllowed = False;


// Exit At End Of Range
if LimitSignalsTimeRange = True and ExitAtEndOfRange = true then begin
	if SignalTimeRangeFrom < SignalTimeRangeTo and Time >= SignalTimeRangeTo then begin
		if (MarketPosition > 0) then Sell("ExitEndOfRangeL1") This Bar On Close;
		if (MarketPosition < 0) then BuyToCover("ExitEndOfRangeS1") This Bar On Close;
	end;
	if SignalTimeRangeFrom >= SignalTimeRangeTo and Time >= SignalTimeRangeTo and Time < SignalTimeRangeFrom then begin
		if (MarketPosition > 0) then Sell("ExitEndOfRangeL2") This Bar On Close;
		if (MarketPosition < 0) then BuyToCover("ExitEndOfRangeS2") This Bar On Close;
	end;
end;
