extern string smm = "----------- Money Management - Fixed Amount -----------";
extern bool UseMoneyManagement = true;
extern double mmRiskedMoney = <@printMMVariableNumber "#RiskedMoney#" />;
extern int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
extern double mmMaxLots = <@printMMVariableNumber "#MaxLots#" />;
extern double mmLotsIfNoMM = <@printMMVariableNumber "#LotsIfNoMM#" />;
extern double mmMultiplier = ${orderSizeMultiplier};
extern double mmStep = ${orderSizeStep};
