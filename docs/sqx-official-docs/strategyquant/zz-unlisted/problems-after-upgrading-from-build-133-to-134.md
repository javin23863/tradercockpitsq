# Problems after upgrading from build 133 to 134

- Source: <https://strategyquant.com/doc/strategyquant/problems-after-upgrading-from-build-133-to-134/>
- Section: StrategyQuant X
- Fetched: 2026-09-04

---

**Problems after upgrading from build 133 to 134**

We had an issue with the updater which caused multiple problems after upgrading from build 133 to build 134.

**The safest solution is to wait until tomorrow build 135 which will fix all mentioned issues.** Second option is to perform multiple updates of the files in C: /StrategyQuant X/folder

or download 134 zip file and make fresh installation

**Known issues and their fixes**

Problem with compiling MT4 source code

Please delete file:

C: /StrategyQuant X/internal/extend/Snippets/SQ/TradingOptions/UseInitialSL.java

After running custom project, it automatically changes symbols and timeframes

Please delete folders:

C: /StrategyQuant X/internal/plugins/SettingsSQXRangerStart

C: /StrategyQuant X/internal/plugins/TaskSQXRangerStart

Application crashes during loading plugins

Please delete folder:

C: /StrategyQuantX\_134\_Dev3\_win/internal/web/PAYMENTDIALOG

Or you can use automatic script which should delete all these files from this url:

https://cdn.strategyquant.com/install/fixBuild134.bat

Just download the script and copy to the SQ X install folder and run from there. If your windows will show the popup dialog with security warning then click for more info and choose run anyway. After running all these issues should be fixed.
