
	// Money Management - Crypto Size by price
	@Configurable(value = "--------- Money Management ---------", readOnly = true)
	public String mmType = "Crypto Size by price";
	@Configurable("Use Money Management")
	public boolean UseMoneyManagement = true;
	@Configurable("Use account balance")
	public boolean mmUseAccountBalance = <@printMMVariableNumber "#UseAccountBalance#" />;
	@Configurable(value = "Maximum size", stepSize = 0.1)
	public double mmMaxSize = <@printMMVariableNumber "#MaxSize#" />;
	@Configurable(value = "Size if no MM", stepSize = 0.1)
	public double mmLotsIfNoMM = 1;
    @Configurable(value = "Size Decimals", stepSize = 1)
	public int mmDecimals = <@printMMVariableNumber "#Decimals#" />;