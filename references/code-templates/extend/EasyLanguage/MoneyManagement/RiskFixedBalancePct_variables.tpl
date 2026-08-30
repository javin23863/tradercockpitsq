	// Money Management - Risk Fixed % Of Balance
    bool UseMoneyManagement(true),
    double mmRiskPercent(<@printMMVariableNumber "#Risk#" />),
	int mmDecimals(<@printMMVariableNumber "#Decimals#" />),
	double mmLotsIfNoMM(<@printMMVariableNumber "#LotsIfNoMM#" />),
	double mmMaxLots(<@printMMVariableNumber "#MaxLots#" />),
    double mmMultiplier(${orderSizeMultiplier}),
