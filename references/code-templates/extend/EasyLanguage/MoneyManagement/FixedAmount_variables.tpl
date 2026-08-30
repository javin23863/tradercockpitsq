	// Money Management - Fixed Amount
    bool UseMoneyManagement(true),
	double mmRiskedMoney(<@printMMVariableNumber "#RiskedMoney#" />),
	int mmDecimals(<@printMMVariableNumber "#Decimals#" />),
	double mmLotsIfNoMM(<@printMMVariableNumber "#LotsIfNoMM#" />),
	double mmMaxLots(<@printMMVariableNumber "#MaxLots#" />),
    double mmMultiplier(${orderSizeMultiplier}),
