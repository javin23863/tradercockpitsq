
extern string smmslpt = "----------- Min/Max SL/PT -----------";
<#if tradingOptionExists("MinMaxSLPT", "MinimumSLPT")>
extern int MinimumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumSLPT"/>;   //Minimum SL in pips
extern int MinimumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumSLPT"/>;   //Minimum PT in pips
<#else>
extern int MinimumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumSL"/>;   //Minimum SL in pips
extern int MinimumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MinimumPT"/>;   //Minimum PT in pips
</#if>
<#if tradingOptionExists("MinMaxSLPT", "MaximumSLPT")>
extern int MaximumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumSLPT"/>;   //Maximum SL in pips
extern int MaximumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumSLPT"/>;   //Maximum PT in pips
<#else>
extern int MaximumSL = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumSL"/>;   //Maximum SL in pips
extern int MaximumPT = <@printTradingOptionIntValue "MinMaxSLPT" "MaximumPT"/>;   //Maximum PT in pips
</#if>