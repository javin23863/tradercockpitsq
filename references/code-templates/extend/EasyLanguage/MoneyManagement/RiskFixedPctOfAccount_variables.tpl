	// Money Management - Risk Fixed % Of Account
    bool UseMoneyManagement(true),
	double mmRiskPercent(<@printMMVariableNumber "#Risk#" />),
	int mmDecimals(<@printMMVariableNumber "#Decimals#" />),
	int mmStopLossPips(<@printMMVariableNumber "#StopLossInPips#" />),
	double mmLotsIfNoMM(<@printMMVariableNumber "#LotsIfNoMM#" />),
	double mmMaxLots(<@printMMVariableNumber "#MaxLots#" />),
    double mmMultiplier(${orderSizeMultiplier}),
