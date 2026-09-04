# Broker profiles

- Source: <https://strategyquant.com/doc/strategyquant/broker-profiles/>
- Section: StrategyQuant X › New features
- Fetched: 2026-09-04

---

Broker profile is a new feature in StrategyQuant X introduced in Build 141.

[![](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_overview-750x518.jpg)](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_overview-750x518.jpg)

Generally, the idea is that you can create a “profile” for a given broker, where you can use specific settings of this broker.

It is because different brokers can have different settings – they are in different time zones, they can even have different settings for some symbols or use different trading sessions.

Broker profiles are a way to configure this in StrategyQuant X, moving one more step towards the realistic backtest with your broker.

[![](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_add.jpg)](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_add.jpg)

Broker profile functionality can be divided into two areas:

1. It can limit the groups of stocks traded in Stockpicker engine – because some brokers support only a subset of stocks.
2. Use broker instrument settings and time zone instead of the default one used in SQX

SQX 141 already comes with a set of predefined broker profiles for most common brokers. Here is the list of default brokers:

- IC Markets
- RoboForex
- PepperStone
- OANDA
- Darwinex
- Dukascopy
- 5ers
- FTMO
- Monevis

## Broker profile Stock Picker functionality

It can limit the groups of stocks traded in Stockpicker engine – because some brokers support only a subset of stocks.

An example is XTB broker – they allow trading a few thousands major stocks from tens of thousands that are generally available on stock exchange.

You can use Broker profile to list stocks that are traded on XTB, and then use this broker profile in Stockpicker backtest – it will ignore stocks that are not traded on XTB, leading to a realistic result.

## Broker profile Metatrader 5 functionality

With this functionality you can specify a timezone of your specific broker, and you can also create Instruments and Sessions specifically for this broker.

Some brokers use different instrument settings on certain symbols – for example, they use different Point value, tick step, etc.

### Usage with Instruments and sessions

It is simple for Instruments and Sessions – you can create instruments/sessions that are specific for your broker. Then you can have data that uses these instruments instead of default SQ instruments, and this way your backtests in SQX will match backtests in your MT5 platform as far as instrument configuration or sessions are considered.

### Usage when downloading data from Dukascopy or Darwinex

Predefined broker profiles can be newly used also when adding and downloading data from free sources like Dukascopy and Darwinex.

You can now select a broker profile when adding these data:

[![](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_add_data-750x434.jpg)](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_add_data-750x434.jpg)

And when you select a broker profile different from SQ Default you will be able to choose your own instrument settings for the added data.

Also, the **downloaded data is automatically converted from the timezone of the source to the timezone of your broker.**

**IMPORTANT NOTE:**

PLEASE BE AWARE THAT THIS SCRIPT RESOLVES THE ISSUE EXTRACTING CORRECT DATA SETTINGS, HOWEVER IT DOES NOT RESOLVE THE ISSUE WITH DATA DIFFERENCES.   
PLEASE READ [THIS ARTICLE ABOUT THE DATA DIFFERENCES](https://strategyquant.com/blog/darwinex-and-dukascopy-the-difficulties-with-changes-of-historical-forex-data/).

FOR ACCURATE RESULTS YOU SHOULD USE BROKER DATA FROM PLATFORM IF THEY ARE AVAILABLE IN METATRADER 5 PLATFORM. CHECK THIS TUTORIAL ON [HOW TO EXPORT DATA FROM METATRADER 5 AND IMPORT TO STRATEGYQUANT X](https://youtu.be/EEBXfv8DwrY).

All point values are calculated in USD currency, if you are using different account currency, you need to update the values from script manually. 

### Getting correct Instruments & sessions from broker

How can you get the correct instrument and session settings for your broker?

As said above, there already are predefined profiles for most common brokers. If you use a different broker, there is another simple solution – a script originally created by SQ power user **Karish**, now maintained by StrategyQuant.

You can download the script (see below) – for MetaTrader 5 version, and when you’ll run it in MT it will go through all the symbols currently on your Market watch, checks instrument info + sessions for all of them and outputs this info to a set of XML files.

You can then import these files in SQX to your broker profile – and you’ll have the correct settings of your broker in the SQX.

[![](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_import.jpg)](https://strategyquant.com/wp-content/uploads/2024/10/broker_profile_import.jpg)

The script is available online or directly in SQ X installation folder**\custom\_indicators\BrokerProfileInstrumentsSessionsScripts\Update\_SQX\_Instruments\_information.ex5**
