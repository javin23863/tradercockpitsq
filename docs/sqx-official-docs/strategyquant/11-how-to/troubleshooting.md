# Troubleshooting

- Source: <https://strategyquant.com/doc/strategyquant/troubleshooting/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

Have you started Builder and no strategy appears in databank for a long time?

It depends on your setting, if you set testing precision too high or complex cross checks and filtering, it might take a lot of time to generate strategies that pass all the filters, but normally, you should see new strategies added into databank every few seconds or minutes.

If no strategies are added into databank for a very long time there might be some problem with your configuration.

Few of the possible configuration problems:

## Using Genetic evolution with too big population

Initial population in Genetic evolution serves as a starting point of evolution – it is not stored into databank. If you set it wrong, you might end up with SQ spending hours or even days generating strategies only to create this initial population, before it even gets to evolution.

[![Troubleshooting - genetic settings](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_1.png)](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_1.png)

On the screenshot above you can see that there are 8 islands x 1000 population on each, which means 8000 strategies for initial population.

Moreover, it has Decimation set to 2, which means that it will generate double this amount of strategies and chooses the best 8000 from them.

So SQ must generate 16.000 strategies that have to pass the Initial population filter. This task alone could take hours or days.

***Recommendation:****Think about your genetic settings, start with smaller population, and Decimation=1. Also, monitor your rejection statistics – isn’t your Initial population filter too strict?**You can try to use Random generation first, to see how quickly it generates strategies with these conditions.*

## Using Genetic evolution with too strict filters

Similar problem as the previous one – you let generation run for some time, but no strategy passes the filters. This probably means that filters are either configured incorrectly or they are too strict.

[![Troubleshgooting - check rejection metrics](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_2.png)](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_2.png)

You can check the detailed Rejection statistics to see why the strategies are getting rejected, but good guess is that the Initial population filter is too strict.

***Recommendation:***  
*Try to use Random generation first, with the same filter settings, to see how quickly it generates strategies with these conditions.*  
*If it takes too long then there might be some problem with settings and you might need to alter it.*

## Getting too many rejections based on Automatic filtering

Automatic filters in SQ serves to filter out strategies with obvious flaws. You should check the Rejection statistics to see if there aren’t too many strategies rejected because of this.

[![Automatic filter: No trades](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_3.png)](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_3.png)

There are few very common rejection reason and their causes:

### Automatic filter: No trades

This simply means that generated strategy doesn’t trade at all. Why is that? Very probably the strategy conditions are generated in such a way that they are never true.

An example of no trades strategy:

```
LongEntrySignal = ((((BearsPower(Main chart,36) > 10.0)
  and (AwesomeOscillator(Main chart,) crosses 0.0 upwards))
  and (RSI(Main chart,20)[3] crosses below 75))
  and Ichimoku(Main chart,9, 26, 52) price crosses KijunSen bearish);

ShortEntrySignal = ((((BearsPower(Main chart,36) < 10)
  and (AwesomeOscillator(Main chart,) crosses 0 downwards))
  and (RSI(Main chart,20)[3] crosses above 75))
  and Ichimoku(Main chart,9, 26, 52) price crosses KijunSen bullish);
```

This strategy was generated with 4 conditions for Long and Short signal, and it seems that they are never true at the same time.

***Recommendation:***  
*Configure SQ to generate less conditions. The more conditions you let it generate the more prone the strategy will be to curve fitting, and the more time you’ll see no trades problem. It is recommended to use 1 to maximum 2 conditions.*

How to configure it – go to Settings -> What to build and there edit # of Conditions. In the opened dialog set the maximum to 1 or 2.

### Automatic filter: Too many trades closing at the same bar / ambiguous trades

Another common problem is when trades open and close at the same bar. This is a problem, because such strategy cannot be reliably backtested.   
It again suggests problems with settings. Most probable cause is that your generated Stop Loss and Profit Target values are too small.

***Recommendation:***  
*Configure SQ to generate bigger SL and PT. The correct size depends on your market and timeframe, so if you use fixed values you should configure its min and max according to this. If you use ATR-based values, use at least 1.5 as the minimum.*

How to configure it – go to Settings -> What to build and there edit Stop Loss and Profit Target. In the opened dialog use at least 1.5 for ATR Multiple minimum and appropriate value for fixed pips minimum.

[![Automatic filter: Too many trades closing at the same bar](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_4.png)](https://strategyquant.com/wp-content/uploads/2020/05/troubleshooting_4.png)
