

// Max trades per day
if(OpenOrdersAllowed = True and MaxTradesPerDay <> 0 and EntriesToday(Date) >= MaxTradesPerDay) then
	OpenOrdersAllowed = False;