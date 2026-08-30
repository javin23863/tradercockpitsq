
extern string smm = "----------- Money Management - Simple Martingale MM -----------";
extern bool UseMoneyManagement = true;
extern double mmLotsStart = <@printMMVariableNumber "#LotsStart#" />;
extern double mmLotsMultiplier = <@printMMVariableNumber "#LotsMultiplier#" />;
extern double mmLotsReset = <@printMMVariableNumber "#LotsReset#" />;
input int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
extern bool mmSeparateByDirection = <@printMMVariableNumber "#SeparateByDirection#" />;
extern double mmLotsIfNoMM = 0.1;
extern double mmMultiplier = ${orderSizeMultiplier};
extern double mmStep = ${orderSizeStep};