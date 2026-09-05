# How to enable or disable GPU acceleration in StrategyQuant X

- Source: <https://strategyquant.com/doc/strategyquant/how-enable-or-disable-gpu-acceleration-in-strategyquant-x/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

Sometimes you might experience a situation that your graphic card has some problems during using StrategyQuant X. In this short tutorial I will show you how to enable or disable GPU. There are two ways how to do that.

### The first way

is to set this option in StrategyQuant X user interface.

Go to the options and click on ‘Configuration’:

[![](https://strategyquant.com/wp-content/uploads/2021/08/config.png)](https://strategyquant.com/wp-content/uploads/2021/08/config.png)

**By default** the GPU acceleration is enabled (as you can see in the following screen):

[![](https://strategyquant.com/wp-content/uploads/2021/08/config-gpu.png)](https://strategyquant.com/wp-content/uploads/2021/08/config-gpu.png)

So if you want to disable the GPU acceleration, just turn off the switch as you can see on the next screen:

[![](https://strategyquant.com/wp-content/uploads/2021/08/config-gpu-off.png)](https://strategyquant.com/wp-content/uploads/2021/08/config-gpu-off.png)

**For applying this option you have to restart SQ X.**

### The second way

is to set the GPU acceleration directly in the config file which is stored here:

```
C:/StrategyQuantX/user/settings/settings.xml
Open it in a text editor and change this line
<gpuAccelerated>true</gpuAccelerated>
to
<gpuAccelerated>false</gpuAccelerated>
after saving the file you can launch the StrategyQuant X.
```
