
extern string smm = "----------- Money Management - Stocks Size by price -----------";
extern bool UseMoneyManagement = true;
extern bool mmUseAccountBalance = <@printMMVariableNumber "#UseAccountBalance#" />;
extern double mmMaxSize = <@printMMVariableNumber "#MaxSize#" />;
extern double mmLotsIfNoMM = 1;
extern double mmMultiplier = ${orderSizeMultiplier};
extern double mmStep = ${orderSizeStep};
