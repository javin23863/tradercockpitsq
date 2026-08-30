[#ftl]

[#macro bbPrintNode node index=1][@compress single_line=true]
[#if node?node_name=="@text"]${node}[#return][/#if]
[#if node?node_name=="variable" || node?node_name=="Variable"][@globalPrintVariableName node.@name /][#return][/#if]

	[#if checkMacroExists(node?node_name) == 1]
		[@(.vars[node?node_name]) node?children node index/]
	[#else]
      [#if checkIsCustomIndicator(node?node_name) == 1]
         [@UserCustomIndicator node?children node index/]
      [#else]
         Missing macro for '${node?node_name}'. You have to add this macro to building blocks include
      [/#if]
	[/#if]
[/@compress][/#macro]

[#----------------------------------------------------------------------------]

[#macro pseudoPrintParams children="" printShift=false][@compress single_line=true]
[#if !children?is_node][#return/][/#if]

[#if pseudoHasSomeParams(children)]([/#if]
[#local someParam=false /]
[#list children.* as param]
   [#switch param.@name]
      [#case "_SYMBOL_"]
         [#if param != "NULL"]
            [#if someParam],[/#if]
            [@printParamOrComboValue param /]
            [#local someParam=true /]
         [/#if]
         [#break]
      [#case "_TIMEFRAME_"]
         [#if param != "0"]
            [#if someParam], [/#if]
            [@printParamOrComboValue param /]
            [#local someParam=true /]
         [/#if]
         [#break]
      [#case "_SHIFT_"]
         [#if printShift][@globalPrintNumberOrVariable param /][/#if]
         [#break]
      [#case "_CI_PARAMS_"]
         [#if someParam],[/#if]
         [@pseudoPrintCustIndParams param /]
         [#break]

      [#case "_OUTPUT_VALUE_"]
      [#case "_INDICATOR_"]
         [#break]

      [#default]
         [#if someParam], [/#if]
         [@printParamOrComboValue param /]
         [#local someParam=true /]
   [/#switch]
[/#list]
[#if pseudoHasSomeParams(children)])[/#if]
[/@compress][/#macro]

[#----------------------------------------------------------------------------]

[#function checkParamIsNumber param]
[#if param?matches(".*[^0-9\\.\\-]+?.*")]
   [#return false]
[#else]
   [#return true]
[/#if]
[/#function]

[#----------------------------------------------------------------------------]

[#macro pseudoPrintCustIndParams param][@compress single_line=true]
      [#if param?string=="NULL" || checkParamIsNumber(param)]${param}[#else]
         [#if param.@type == "variable"][@globalPrintNumberOrVariable param /][#else]${param?string}[/#if]
      [/#if]
[/@compress][/#macro]

[#----------------------------------------------------------------------------]

[#function pseudoHasSomeParams children]
[#local someParam=false /]
[#list children.* as param]
   [#switch param.@name]
      [#case "_SYMBOL_"]
         [#if param != "NULL"]
            [#local someParam=true /]
         [/#if]
         [#break]
      [#case "_TIMEFRAME_"]
         [#if param != "0"]
            [#local someParam=true /]
         [/#if]
         [#break]
      [#case "_SHIFT_"]
         [#break]
      [#default]
         [#local someParam=true /]
   [/#switch]
[/#list]

   [#return someParam]
[/#function]


[#----------------------------------------------------------------------------]


[#macro pseudoPrintShift children ][@compress single_line=true]
[#list children.* as param]
[#if param.@name = "_SHIFT_"][@globalPrintNumberOrVariable param /][/#if]
[/#list]
[/@compress][/#macro]


[#----------------------------------------------------------------------------]


[#macro pseudoPrintOutputValue children ][@compress single_line=true]
[#list children.* as param]
[#if param.@name = "_OUTPUT_VALUE_"]${param?number}[/#if]
[/#list]
[/@compress][/#macro]

[#----------------------------------------------------------------------------]


[#macro pseudoPrintOutputValue children ][@compress single_line=true]
[#list children.* as param]
[#if param.@name = "_OUTPUT_VALUE_"][@printParamOrComboValue param /]

[/#if]
[/#list]
[/@compress][/#macro]

[#macro printParamOrComboValue param]
[#if globalAttributeExists(param, "comboValue")]${param.@comboValue}[#else][@globalPrintNumberOrVariable param /][/#if]
[/#macro]


[#----------------------------------------------------------------------------]

[#macro pseudoPrintGlobalPTSLVariables]
[#local ptSLNode = globalGetOptions("GlobalSLPT") /]
[#if ptSLNode.useGlobalSLPT == "true"]
  [#if ptSLNode.useSameSLPTforBothDirections == "true"]
ProfitTarget = ${ptSLNode.values.globalSL.values.value} pips;
StopLoss = ${ptSLNode.values.globalPT.values.value} pips;
  [#else]
LongProfitTarget = ${ptSLNode.values.globalLongPT.values.value} pips;
LongStopLoss = ${ptSLNode.values.globalLongPT.values.value} pips;
ShortProfitTarget = ${ptSLNode.values.globalShortPT.values.value} pips;
ShortStopLoss = ${ptSLNode.values.globalShortSL.values.value} pips;
  [/#if]
[/#if]
[/#macro]

[#----------------------------------------------------------------------------]

[#macro UserCustomIndicator children node index=1]
CustomIndicator[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]


[#----------------------------------------------------------------------------]





[#macro Ask children node index=1]
Ask
[/#macro]

[#macro Bid children node index=1]
Bid
[/#macro]

[#macro Spread children node index=1]
Spread
[/#macro]

[#macro SpreadInPips children node index=1]
SpreadInPips
[/#macro]

[#macro Open children node index=1]
Open[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro High children node index=1]
High[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Low children node index=1]
Low[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Close children node index=1]
Close[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro OpenD children node index=1]
OpenD[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HighD children node index=1]
HighD[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro LowD children node index=1]
LowD[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro CloseD children node index=1]
CloseD[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HAOpen children node index=1]
HeikenAshiOpen[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HAHigh children node index=1]
HeikenAshiHigh[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HALow children node index=1]
HeikenAshiLow[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HAClose children node index=1]
HeikenAshiClose[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro AO children node index=1]
AccelleratorOscilator[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro AD children node index=1]
Accumulation[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro AL children node index=1]
Alligator[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro ADX children node index=1]
ADX_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro ATR children node index=1]
ATR[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro AWO children node index=1]
AwesomeOscilator[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro BP children node index=1]
BearsPower[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro BollingerBand children node index=1]
BollingerBand_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro BU children node index=1]
BullsPower[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro CCI children node index=1]
CCI[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro DE children node index=1]
Demarker[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro EN children node index=1]
Envelopes[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro FI children node index=1]
ForceIndex[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro FC children node index=1]
Fractals[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro GO children node index=1]
GatorOscillators_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro ICH children node index=1]
Ichimoku_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro MACD children node index=1]
MACD_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro MFI children node index=1]
MarketFacilitationIndex[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro MO children node index=1]
Momentum[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro MOFI children node index=1]
MoneyFlowIndex[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro MA children node index=1]
MovingAverage[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro SMA children node index=1]
SMA[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro EMA children node index=1]
EMA[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro TEMA children node index=1]
TEMA[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro OsMA children node index=1]
OsMA[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro OBV children node index=1]
OnBalanceVolume[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro ParabolicSAR children node index=1]
ParabolicSAR[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro RSI children node index=1]
RSI[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro RVI children node index=1]
RelativeVigorIndex_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro SD children node index=1]
StandardDeviation[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Stoch children node index=1]
Stochastic[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Volume children node index=1]
Volume[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro WMI children node index=1]
Williams%R[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro PriceDifference children node index=1]
PriceDifference([@bbPrintNode children[0]?children[0].values.value?children[0] /] - [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro CI children node index=1]
CustomIndicator([@pseudoPrintParams children[0] /], [@pseudoPrintOutputValue children[0] /])[[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HA children node index=1]
HeikenAshi_[@pseudoPrintOutputValue children[0] /][@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Lowest children node index=1]
Lowest[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Highest children node index=1]
Highest[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HighestInRange children node index=1]
HighestInRange[@pseudoPrintParams children[0] /]
[/#macro]

[#macro LowestInRange children node index=1]
LowestInRange[@pseudoPrintParams children[0] /]
[/#macro]

[#macro BarRange children node index=1]
BarRange[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro HighestRange children node index=1]
BiggestRange[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro LowestRange children node index=1]
SmallestRange[@pseudoPrintParams children[0] /][[@pseudoPrintShift children[0] /]]
[/#macro]

[#macro Hour children node index=1]
Hour[[@pseudoPrintParams children[0] true /]]
[/#macro]

[#macro Minute children node index=1]
Minute[[@pseudoPrintParams children[0] true /]]
[/#macro]

[#macro DayOfWeek children node index=1]
DayOfWeek[[@pseudoPrintParams children[0] true /]]
[/#macro]

[#macro IntConstant children node index=1]
_CONSTANT_
[/#macro]

[#macro Time children node index=1]
Time[[@pseudoPrintParams children[0] true /]]
[/#macro]

[#macro TimeCurrent children node index=1]
TimeCurrent()
[/#macro]

[#macro Date children node index=1]
Date[[@pseudoPrintParams children[0] true /]]
[/#macro]

[#macro DateCurrent children node index=1]
DateCurrent()
[/#macro]

[#macro CurrentPosition children node index=1]
Market Position([@pseudoPrintParams children[0] /])
[/#macro]

[#macro CurrentOrderPosition children node index=1]
Order Position([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrdersAveragePrice children node index=1]
Orders Average Price([@pseudoPrintParams children[0] /])
[/#macro]

[#macro Flat children node index=1]
Flat
[/#macro]

[#macro Long children node index=1]
Long
[/#macro]

[#macro Short children node index=1]
Short
[/#macro]

[#macro LastOrder children node index=1]
Last Order([@pseudoPrintParams children[0] /])
[/#macro]

[#macro LastOrderToday children node index=1]
Last Order Today([@pseudoPrintParams children[0] /])
[/#macro]

[#macro BarsSinceEntry children node index=1]
BarsSinceOrderOpen([@pseudoPrintParams children[0] /])
[/#macro]

[#macro BarsSinceExit children node index=1]
BarsSinceOrderClosed([@pseudoPrintParams children[0] /])
[/#macro]

[#macro PendingOrderExists children node index=1]
PendingOrderExists([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderOpenedThisBar children node index=1]
OrderOpenedThisBar([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderOpenedThisMinute children node index=1]
OrderOpenedThisMinute([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderClosedThisBar children node index=1]
OrderClosedThisBar([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderClosedThisMinute children node index=1]
OrderClosedThisMinute([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrdersOpenedToday children node index=1]
Orders Opened Today([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OpenPL children node index=1]
Open P/L ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro ClosedPL children node index=1]
Closed P/L ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro TotalClosedPL children node index=1]
Sum of Closed P/L ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OpenPLInMoney children node index=1]
Open P/L In Money([@pseudoPrintParams children[0] /])
[/#macro]

[#macro ClosedPLInMoney children node index=1]
Closed P/L In Money ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro TotalClosedPLInMoney children node index=1]
Sum of Closed P/L In Money ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro TotalProfits children node index=1]
Total Profits ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro TotalLosses children node index=1]
Total Losses ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro CurrentPositionSize children node index=1]
CurrentPositionSize ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro LastPositionSize children node index=1]
LastPositionSize ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro AccountBalance children node index=1]
Account Balance
[/#macro]

[#macro AccountEquity children node index=1]
Account Equity
[/#macro]

[#macro OrderOpeningPrice children node index=1]
OpenPrice([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderSL children node index=1]
Order SL ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderPT children node index=1]
Order PT ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro Symbol children node index=1]
Symbol ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro OrderSymbol children node index=1]
Order Symbol ([@pseudoPrintParams children[0] /])
[/#macro]

[#macro Addition children node index=1]
([@bbPrintNode children[0]?children[0].values.value?children[0] /] + [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro Subtraction children node index=1]
([@bbPrintNode children[0]?children[0].values.value?children[0] /] - [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro Multiplication children node index=1]
([@bbPrintNode children[0]?children[0].values.value?children[0] /] * [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro Division children node index=1]
([@bbPrintNode children[0]?children[0].values.value?children[0] /] / [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro CustomFunction children node index=1]
[@globalPrintCustomFunction children[0] /]
[/#macro]

[#macro IndicatorAngle children node index=1]
IndicatorAngle([@bbPrintNode children[0]?children[0].values.value?children[0] /])
[/#macro]

[#macro IndicatorAvg children node index=1]
IndicatorAverage[@pseudoPrintParams children[0] /]([@bbPrintNode children[0]?children[0].values.value?children[0] /])
[/#macro]

[#macro Minimum children node index=1]
Minimum([@bbPrintNode children[0]?children[0].values.value?children[0] /], [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro Maximum children node index=1]
Maximum([@bbPrintNode children[0]?children[0].values.value?children[0] /], [@bbPrintNode children[0]?children[1].values.value?children[0] /])
[/#macro]

[#macro MathAbs children node index=1]
MathAbs([@bbPrintNode children[0]?children[0].values.value?children[0] /])
[/#macro]

[#macro MathRound children node index=1]
MathRound([@bbPrintNode children[0]?children[0].values.value?children[0] /], [@pseudoPrintShift children[0] /])
[/#macro]

[#macro IfCond children node index=1]
([@bbPrintNode children[0]?children[0].values.value?children[0] /] ? [@bbPrintNode children[0]?children[1].values.value?children[0] /] : [@bbPrintNode children[0]?children[2].values.value?children[0] /])
[/#macro]

[#macro ConvertToVirtualPips children node index=1]
ConvertToPips([@bbPrintNode children[0]?children[0].values.value?children[0] /])
[/#macro]

[#macro ConvertToRealPips children node index=1]
ConvertToRealPips([@pseudoPrintParams children[0] /])
[/#macro]

[#macro ConvertPLToRealPips children node index=1]
ConvertPLToRealPips([@bbPrintNode children[0]?children[0].values.value?children[0] /])
[/#macro]

[#macro GetTime children node index=1]
GetTime([@pseudoPrintParams children[0] /])
[/#macro]

[#macro GetDate children node index=1]
GetDate([@pseudoPrintParams children[0] /])
[/#macro]

[#macro IsBarOpen children node index=1]
IsBarOpen
[/#macro]

[#macro Constant children node index=1]
[@globalPrintConstant children[0] /]
[/#macro]

[#macro String children node index=1]
[@globalPrintConstant children[0] /]
[/#macro]

[#macro True children node index=1]
True
[/#macro]

[#macro False children node index=1]
False
[/#macro]

[#macro CandlePattern children node index=1]
Candle Pattern[[@pseudoPrintParams children[0] /]]
[/#macro]

[#macro Doji children node index=1]
Doji
[/#macro]

[#macro Hammer children node index=1]
Hammer
[/#macro]

[#macro ShootingStar children node index=1]
Shooting Star
[/#macro]

[#macro DarkCloud children node index=1]
Dark Cloud
[/#macro]

[#macro PiercingLine children node index=1]
Piercing Line
[/#macro]

[#macro BearishEngulfing children node index=1]
Bearish Engulfing
[/#macro]

[#macro BullishEngulfing children node index=1]
Bullish Engulfing
[/#macro]

[#macro BearishHarami children node index=1]
Bearish Harami
[/#macro]

[#macro BullishHarami children node index=1]
Bullish Harami
[/#macro]

[#macro BearishHaramiCross children node index=1]
Bearish Harami Cross
[/#macro]

[#macro BullishHaramiCross children node index=1]
Bullish Harami Cross
[/#macro]

[#macro IsGreater children node index=1]
([@bbPrintNode children[0] /] > [@bbPrintNode children[1] /])
[/#macro]

[#macro IsGreaterOrEquals children node index=1]
([@bbPrintNode children[0] /] >= [@bbPrintNode children[1] /])
[/#macro]

[#macro IsLower children node index=1]
([@bbPrintNode children[0] /] < [@bbPrintNode children[1] /])
[/#macro]

[#macro IsLowerOrEquals children node index=1]
([@bbPrintNode children[0] /] <= [@bbPrintNode children[1] /])
[/#macro]

[#macro Equals children node index=1]
([@bbPrintNode children[0] /] = [@bbPrintNode children[1] /])
[/#macro]

[#macro NotEquals children node index=1]
([@bbPrintNode children[0] /] <> [@bbPrintNode children[1] /])
[/#macro]

[#macro CrossesAbove children node index=1]
([@bbPrintNode children[0] /] Crosses Above [@bbPrintNode children[1] /])
[/#macro]

[#macro CrossesBelow children node index=1]
([@bbPrintNode children[0] /] Crosses Below [@bbPrintNode children[1] /])
[/#macro]

[#macro Is children node index=1]
([@bbPrintNode children[0] /] is [@bbPrintNode children[1] /])
[/#macro]

[#macro Was children node index=1]
([@bbPrintNode children[0] /] was [@bbPrintNode children[1] /])
[/#macro]

[#macro WasNot children node index=1]
([@bbPrintNode children[0] /] was not [@bbPrintNode children[1] /])
[/#macro]

[#macro IsNot children node index=1]
([@bbPrintNode children[0] /] is not [@bbPrintNode children[1] /])
[/#macro]

[#macro And children node index=1]
([@bbPrintNode children[0] /] ___BR___And [@bbPrintNode children[1] /])
[/#macro]

[#macro Or children node index=1]
([@bbPrintNode children[0] /] ___BR___Or [@bbPrintNode children[1] /])
[/#macro]

