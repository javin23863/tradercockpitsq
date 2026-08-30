
	@Configurable(value = "Dont Trade On Weekends")
	public boolean DontTradeOnWeekends = <@printTradingOptionBoolValue "DontTradeOnWeekends" "DontTradeOnWeekends"/>;
	@Configurable(value = "Friday Close Time")
	public String FridayCloseTime = "<@printTradingOptionTimeValue "DontTradeOnWeekends" "FridayCloseTime"/>";
	@Configurable(value = "Sunday Open Time")
	public String SundayOpenTime = "<@printTradingOptionTimeValue "DontTradeOnWeekends" "SundayOpenTime"/>";