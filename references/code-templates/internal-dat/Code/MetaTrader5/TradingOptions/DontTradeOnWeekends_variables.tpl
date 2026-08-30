
input string sdtw = "----------- Dont Trade On Weekends -----------";
input bool DontTradeOnWeekends = <@printTradingOptionBoolValue "DontTradeOnWeekends" "DontTradeOnWeekends"/>;
input string FridayCloseTime = "<@printTradingOptionTimeValue "DontTradeOnWeekends" "FridayCloseTime"/>";
input string SundayOpenTime = "<@printTradingOptionTimeValue "DontTradeOnWeekends" "SundayOpenTime"/>";