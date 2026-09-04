# Import history data from MetaTrader 4

- Source: <https://strategyquant.com/doc/quantdatamanager/import-history-data-metatrader-4/>
- Section: QuantDataManager › How to...
- Fetched: 2026-09-04

---

## Step 1: Export data from Metatrader

Open your Metatrader and go to Tools -> **History Center.**

[![](https://strategyquant.com/wp-content/uploads/2018/12/export_from_metatrader1.png)](https://strategyquant.com/wp-content/uploads/2018/12/export_from_metatrader1.png)

There, open the currency you want to export (for example GBPUSD) and double click on **1 Minute (M1)** so that it is refreshed on the right side of the screen.  
Then just click on the Export button and choose your destination file.

> StrategyQuant supports import of only 1 Minute data, it will compute the higher timeframes automatically.

Now we have the data ready to be imported to StrategyQuant.

## Step 2: Create a new symbol in StrategyQuant

We can import data to already existing symbol, but that will overwrite its data, so it is better to define a new symbol.  
Start StrategyQuant, go to **Data manager** screen, **File import** and click on the button “Add symbol”.

[![](https://strategyquant.com/wp-content/uploads/2018/12/export2.png)](https://strategyquant.com/wp-content/uploads/2018/12/export2.png)

In the appeared window you can type in Data symbol name and choose predefined instrument setting or create the new one clicking on „Add new symbol“ option.  
If you decide to add new instrument then complete the setting of the new symbol

[![](https://strategyquant.com/wp-content/uploads/2018/12/export3.png)](https://strategyquant.com/wp-content/uploads/2018/12/export3.png)

[![](https://strategyquant.com/wp-content/uploads/2018/12/export4.png)](https://strategyquant.com/wp-content/uploads/2018/12/export4.png)

### Instrument

Name of the instrument

### Data type

The type of imported data.

### Pip/Tick size

is the value of pip. It means what number is 1 pip. is usually 0.0001. For JPY based pairs 1 pip is 0.01.

### Default spread

Default spread of the instrument in pips

### Point value in $

is a value how much is one pip worth in money, it is usually 10 for all the currencies.

### Pip/Tick Step

is a value by how much one pip can move. Virtually all brokers now use 5-digit data, so the value will be 0.00001 (or 0.001 for JPY based pairs).

### Point value in $

is a value how much is one pip worth in money, it is usually 10 for all the currencies.

### Default commission model

Define commission model of the instrument.

Click Save and the new symbol will be created. The symbol doesn’t have any data yet, but we are going to import them in the next step.

## Step 3: Import data to StrategyQuant

Now select your new row with the symbol and click on **Import file** … button on the top. This will open the import dialog.

[![](https://strategyquant.com/wp-content/uploads/2018/12/export5.png)](https://strategyquant.com/wp-content/uploads/2018/12/export5.png)

Import dialog is configurable; it allows importing data from various file formats. Since we use data from MetaTrader, choose MetaTrader4 as a Predefined File Format.

[![](https://strategyquant.com/wp-content/uploads/2018/12/export6.png)](https://strategyquant.com/wp-content/uploads/2018/12/export6.png)

Choose the data file and click on Start Import button. This will start the import process. It could take few minutes, depending on speed of your computer and data size.

[![](https://strategyquant.com/wp-content/uploads/2018/12/export7.png)](https://strategyquant.com/wp-content/uploads/2018/12/export7.png)  
When the import is finished it will display information window and asks you to close the dialog.  
Now we have the new data successfully imported into StrategyQuant and we can use them for tests or new strategies generation.
