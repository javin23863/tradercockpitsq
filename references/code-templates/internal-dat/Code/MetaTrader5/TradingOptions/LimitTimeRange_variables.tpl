

input string sltr = "----------- Limit Time Range -----------";
input bool LimitTimeRange = <@printTradingOptionBoolValue "LimitTimeRange" "LimitTimeRange"/>;
input string SignalTimeRangeFrom = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeFrom"/>";
input string SignalTimeRangeTo = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeTo"/>";
input bool ExitAtEndOfRange = <@printTradingOptionBoolValue "LimitTimeRange" "ExitAtEndOfRange"/>;
input int OrderTypeToExit = <@printTradingOptionIntValue "LimitTimeRange" "OrderTypeToExit"/>;
