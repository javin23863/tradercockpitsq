
	@Configurable(value = "Limit Time Range")
	public boolean LimitSignalsTimeRange = <@printTradingOptionBoolValue "LimitTimeRange" "LimitTimeRange"/>;
	@Configurable(value = "Time Range From")
	public String SignalTimeRangeFrom = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeFrom"/>";
	@Configurable(value = "Time Range To")
	public String  SignalTimeRangeTo = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeTo"/>";
	@Configurable(value = "Exit At End Of Range")
	public boolean ExitAtEndOfRange = <@printTradingOptionBoolValue "LimitTimeRange" "ExitAtEndOfRange"/>;
