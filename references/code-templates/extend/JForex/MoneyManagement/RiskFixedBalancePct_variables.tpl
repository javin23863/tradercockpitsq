	
	// Money Management - Risk Fixed % Of Balance
	@Configurable(value = "--------- Money Management ---------", readOnly = true)
	public String mmType = "Risk Fixed % Of Balance";
	@Configurable("Use Money Management")
	public boolean UseMoneyManagement = true;
	@Configurable(value = "Risk in %", stepSize = 0.1)
	public double mmRiskPercent = <@printMMVariableNumber "#Risk#" />;
	@Configurable(value = "Size Decimals", stepSize = 1)
	public int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
	@Configurable(value = "Maximum lots", stepSize = 0.1)
	public double mmMaxLots = <@printMMVariableNumber "#MaxLots#" />;
	@Configurable(value = "Size if no MM", stepSize = 0.1)
	public double mmLotsIfNoMM = <@printMMVariableNumber "#LotsIfNoMM#" />;