  // Money Management - Simple Martingale MM
    bool UseMoneyManagement(true),
    double mmLotsStart(<@printMMVariableNumber "#LotsStart#" />),
    double mmLotsMultiplier(<@printMMVariableNumber "#LotsMultiplier#" />),
    double mmLotsReset(<@printMMVariableNumber "#LotsReset#" />),
	  int mmDecimals(<@printMMVariableNumber "#Decimals#" />),
    bool mmSeparateByDirection(<@printMMVariableNumber "#SeparateByDirection#" />),
    double mmLotsIfNoMM(1),
    double mmMultiplier(${orderSizeMultiplier}),