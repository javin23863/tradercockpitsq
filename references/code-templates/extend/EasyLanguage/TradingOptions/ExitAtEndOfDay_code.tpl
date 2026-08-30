

// Exit on close (end of day)
if ExitAtEndOfDay then begin
	if DayExitTime <> 0000 then begin 
		if LimitSignalsTimeRange = False then begin 
		 	if Time >= DayExitTime then begin 
				if (MarketPosition > 0) then Sell("ExitEndOfDayL1") This Bar On Close;
				if (MarketPosition < 0) then BuyToCover("ExitEndOfDayS1") This Bar On Close;
				OpenOrdersAllowed = False;
			end;
		end;
		if LimitSignalsTimeRange = True then begin
			if Time >= DayExitTime and Time < SignalTimeRangeFrom then begin
				if (MarketPosition > 0) then Sell("ExitEndOfDayL2") This Bar On Close;
				if (MarketPosition < 0) then BuyToCover("ExitEndOfDayS2") This Bar On Close; 
				OpenOrdersAllowed = False;
			end;
			if DayExitTime >= SignalTimeRangeFrom and  SignalTimeRangeFrom < SignalTimeRangeTo then begin
				if Time >= DayExitTime and Time >= SignalTimeRangeFrom then begin
					if (MarketPosition > 0) then Sell("ExitEndOfDayL3") This Bar On Close;
					if (MarketPosition < 0) then BuyToCover("ExitEndOfDayS3") This Bar On Close;
					OpenOrdersAllowed = False;
				end;
			end;
		end;
	end
	else SetExitOnClose;
end;

