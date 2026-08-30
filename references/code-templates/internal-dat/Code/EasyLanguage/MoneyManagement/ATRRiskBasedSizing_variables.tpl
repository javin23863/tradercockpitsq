// Money Management - ATR Risk-Based Sizing
    bool UseMoneyManagement(true),
    double mmRiskPercent(<@printMMVariableNumber "#Risk#" />),
	int mmATRPeriod(<@printMMVariableNumber "#ATRPeriod#" />),
	double mmATRMultiplier(<@printMMVariableNumber "#ATRMult#" />),
	int mmDecimals(<@printMMVariableNumber "#Decimals#" />),
	double mmLotsIfNoMM(<@printMMVariableNumber "#LotsIfNoMM#" />),
	double mmMaxLots(<@printMMVariableNumber "#MaxLots#" />),
    double mmMultiplier(${orderSizeMultiplier}),