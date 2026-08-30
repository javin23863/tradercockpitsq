
	@Configurable(value = "Limit max distance")
	public boolean LimitMaxDistanceFromMarketPrice = <@printTradingOptionBoolValue "MaxDistanceFromMarket" "MaxDistanceFromMarket"/>;
	@Configurable(value = "Max distance %", stepSize = 1)
	public double MaxDistanceFromMarketPct = <@printTradingOptionIntValue "MaxDistanceFromMarket" "MaxDistancePct"/>;