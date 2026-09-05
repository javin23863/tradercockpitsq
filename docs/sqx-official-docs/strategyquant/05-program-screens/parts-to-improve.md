# Settings – Parts to improve

- Source: <https://strategyquant.com/doc/strategyquant/parts-to-improve/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

This setting is visible only if you chose that you want to improve an existing strategy in **What to build** setting.

Here you can configure which parts of the strategy should be improved. You can further configure if you want to replace the whole part, or add new blocks to it

[![](https://strategyquant.com/wp-content/uploads/2019/01/parts_to_improve.png)](https://strategyquant.com/wp-content/uploads/2019/01/parts_to_improve.png)

It simply allows you to choose which parts of the strategy you want to improve. You can choose to improve Entry rule, Exit rule or Order type, and you can choose one of the three options:

- **Add** – add new conditions to the existing ones
- **Replace** – delete the existing conditions and generates new ones
- **Add or Replace** – randomly decides whether to use Add or Replace.

Strategy generator in this mode will simply take your strategy, tries to recognize what are the entry and exit rules, and then depending on your configuration generates additional conditions or replaces for of the conditions in your existing strategy with new ones.

**Note that there is another more flexible way to improve your strategies** – using **Build from strategy template**.

In this case you’ll have to open your existing strategy in AlgoWizard, and put a generation placeholders to the parts you want to change. Then start build with generation using this template and it will produce new strategies that are based on your original strategy template, but with placeholders replaced with newly generated conditions or actions.
