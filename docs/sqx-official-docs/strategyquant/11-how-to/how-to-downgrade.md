# How to downgrade

- Source: <https://strategyquant.com/doc/strategyquant/how-to-downgrade/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

The steps to downgrade your existing installation are simple:

1. Download the older build as a ZIP from our **[Download](https://strategyquant.com/download)** page – section **Previous builds**
2. Rename the folder where your current version fo SQ is installed, for example from ***C:StrategyQuant*** to ***C:StrategyQuant\_backup***
3. Extract the downloaded older build ZIP to ***C:StrategyQuant*** – this creates a complete clean installation
4. Copy the whole **/user** folder from ***C:StrategyQuant\_backup*** to ***C:StrategyQuant***. This copies all your data and project settings from your backup installation to the new one.

Then just start StrategyQuantX.exe from ***C:StrategyQuant*** folder and everything should be working. You can then delete the backup folder.

Note – SQ builds are backwards compatible, but not forward compatible. If you are using for example version 126 and you’d copy its /user contents to older build 120, some of the configs or data might not be recognized properly in the old 120 version.
