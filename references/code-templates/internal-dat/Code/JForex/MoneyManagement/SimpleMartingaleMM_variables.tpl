
	// Money Management - Simple Martingale MM
	@Configurable(value = "--------- Money Management ---------", readOnly = true)
	public String mmType = "Simple Martingale MM";
	@Configurable("Use Money Management")
	public boolean UseMoneyManagement = true;
	@Configurable(value = "Starting lots", stepSize = 0.1)
    public double mmLotsStart = <@printMMVariableNumber "#LotsStart#" />;
	@Configurable(value = "Lots multiplier")
    public double mmLotsMultiplier = <@printMMVariableNumber "#LotsMultiplier#" />;
	@Configurable(value = "Maximum Lots (reset)", stepSize = 0.1)
    public double mmLotsReset = <@printMMVariableNumber "#LotsReset#" />;
	@Configurable(value = "Size Decimals", stepSize = 1)
	public int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
	@Configurable(value = "Separate MM by direction?")
    public boolean mmSeparateByDirection = <@printMMVariableNumber "#SeparateByDirection#" />;
	@Configurable(value = "Size if no MM", stepSize = 0.1)
	public double mmLotsIfNoMM = 0.1;