

extern string sltr = "----------- Limit Time Range -----------";
extern bool LimitTimeRange = <@printTradingOptionBoolValue "LimitTimeRange" "LimitTimeRange"/>;
extern string SignalTimeRangeFrom = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeFrom"/>";
extern string SignalTimeRangeTo = "<@printTradingOptionTimeValue "LimitTimeRange" "SignalTimeRangeTo"/>";
extern bool ExitAtEndOfRange = <@printTradingOptionBoolValue "LimitTimeRange" "ExitAtEndOfRange"/>;
extern int OrderTypeToExit = <@printTradingOptionIntValue "LimitTimeRange" "OrderTypeToExit"/>;
