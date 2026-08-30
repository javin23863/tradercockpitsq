
// Exit on Friday
if ExitOnFriday then begin
	if DayOfWeek(Date) = 5 then begin
		if FridayExitTime <> 0000 and Time >= FridayExitTime then begin
			if (MarketPosition > 0) then Sell("ExitFridayL") This Bar On Close;
			if (MarketPosition < 0) then BuyToCover("ExitFridayS") This Bar On Close;
			OpenOrdersAllowed = False;
		end;
	end
	else if DayOfWeek(Date) = 6 then begin
		OpenOrdersAllowed = False;
	end; 
end;