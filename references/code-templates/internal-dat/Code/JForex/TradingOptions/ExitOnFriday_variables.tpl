    
	@Configurable(value = "Exit On Friday")
	public boolean ExitOnFriday = <@printTradingOptionBoolValue "ExitOnFriday" "ExitOnFriday"/>;
	@Configurable(value = "Friday Exit Time")
	public String FridayExitTime = "<@printTradingOptionTimeValue "ExitOnFriday" "FridayExitTime"/>";