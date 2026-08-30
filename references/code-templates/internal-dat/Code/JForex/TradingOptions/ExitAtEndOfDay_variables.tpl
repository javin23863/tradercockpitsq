	
	@Configurable(value = "Exit At End Of Day")
	public boolean ExitAtEndOfDay = <@printTradingOptionBoolValue "ExitAtEndOfDay" "ExitAtEndOfDay"/>;
	@Configurable(value = "End Of Day Exit Time")
	public String DayExitTime = "<@printTradingOptionTimeValue "ExitAtEndOfDay" "EODExitTime"/>";
	