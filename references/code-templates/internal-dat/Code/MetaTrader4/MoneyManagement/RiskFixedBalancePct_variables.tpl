extern string smm = "----------- Money Management - Risk Fixed % Of Balance -----------";
extern bool UseMoneyManagement = true;
extern double mmRiskPercent = <@printMMVariableNumber "#Risk#" />;
extern int mmDecimals = <@printMMVariableNumber "#Decimals#" />;
extern double mmLotsIfNoMM = <@printMMVariableNumber "#LotsIfNoMM#" />;
extern double mmMaxLots = <@printMMVariableNumber "#MaxLots#" />;
extern double mmMultiplier = ${orderSizeMultiplier};
extern double mmStep = ${orderSizeStep};
