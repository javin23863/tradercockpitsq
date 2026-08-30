
// Dont trade on weekends
if DontTradeOnWeekends then begin
	if DayOfWeek(Date) = 5 then begin
		if FridayCloseTime <> 0000 and Time >= FridayCloseTime then begin
			OpenOrdersAllowed = False;
		end;	
	end
	else if DayOfWeek(Date) = 6 then begin
		OpenOrdersAllowed = False;
	end
	else if DayOfWeek(Date) = 0 and Time < SundayOpenTime then begin
		OpenOrdersAllowed = False;
	end; 
end;