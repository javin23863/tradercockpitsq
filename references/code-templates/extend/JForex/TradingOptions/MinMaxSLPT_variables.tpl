
	@Configurable(value = "Minimum SL", stepSize = 1)
	public int MinimumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumSL"/>;  // Minimum SL in ticks/pips, 0 means unlimited
	@Configurable(value = "Maximum SL", stepSize = 1)
	public int MaximumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumSL"/>;  // Maximum SL in ticks/pips, 0 means unlimited
	@Configurable(value = "Minimum PT", stepSize = 1)
	public int MinimumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumPT"/>;  // Minimum PT in ticks/pips, 0 means unlimited
	@Configurable(value = "Maximum PT", stepSize = 1)
	public int MaximumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumPT"/>;  // Maximum PT in ticks/pips, 0 means unlimited	