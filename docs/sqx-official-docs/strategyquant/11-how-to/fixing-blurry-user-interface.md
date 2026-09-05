# Fixing blurry user interface

- Source: <https://strategyquant.com/doc/strategyquant/fixing-blurry-user-interface/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

On Windows 10, when you have screen scaling turned on and set to value different than 100% (ie 125%) as shown in the screenshot below, the SQ user interface might become blurry and hardly readable.

[![](https://images.idgesg.net/images/article/2018/07/scalingwindows10-100764460-large.jpg)](https://images.idgesg.net/images/article/2018/07/scalingwindows10-100764460-large.jpg)

From SQ X version 132 dev2 we have included a batch script that will fix this problem. Simply run the file in the location below and the problem will be resolved. The script needs to be run only once:

```
StrategyQuantX install folder/fixDPI.bat
```
