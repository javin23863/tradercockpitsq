
	// Money Management - Fixed size
	@Configurable(value = "--------- Money Management ---------", readOnly = true)
	public String mmType = "Fixed size";
	@Configurable(value = "Order size", stepSize = 0.1)
	public double mmLots = <@printMMVariableNumber "#Size#" />;