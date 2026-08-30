
extern string smm = "----------- Money Management - Crypto Size by price -----------";
extern bool UseMoneyManagement = true;
extern bool mmUseAccountBalance = <@printMMVariableNumber "#UseAccountBalance#" />;
extern double mmMaxSize = <@printMMVariableNumber "#MaxSize#" />;
extern int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
extern double mmLotsIfNoMM = 1;
extern double mmMultiplier = ${orderSizeMultiplier};
extern double mmStep = ${orderSizeStep};
