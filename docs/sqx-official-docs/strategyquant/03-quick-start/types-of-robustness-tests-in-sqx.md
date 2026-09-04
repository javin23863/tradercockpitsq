# Types of robustness tests in SQX

- Source: <https://strategyquant.com/doc/strategyquant/types-of-robustness-tests-in-sqx/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

## **In the Sample / Out Of the sample**

In-Sample (IS) and Out-of-Sample (OOS) testing are essential concepts in the development, testing, and validation of trading strategies. They help assess the robustness of strategies and ensure that they are not overfit to a specific dataset. The following is an extensive explanation of In-Sample and Out-of-Sample testing:

### **Definition of IS and OOS Data:**

**In-Sample (IS) Data:** The IS data is the portion of historical data used for developing and optimizing the trading strategy. During the strategy development process, various parameters, rules, and indicators are adjusted and fine-tuned to achieve the best possible performance on the IS data.

**Out-of-Sample (OOS) Data:** The OOS data is a separate, unseen portion of the historical data reserved for testing and validating the trading strategy. The OOS data is not used in the development or optimization process to ensure that the performance of the strategy on this data is an unbiased evaluation of its effectiveness and robustness**.**

The primary goal of using IS and OOS data is to assess the robustness and generalizability of trading strategies. By evaluating the strategy’s performance on both IS and OOS data, users can identify potential overfitting and ensure that the strategy is not overly optimized for a specific dataset. A strategy that performs well on both IS and OOS data is more likely to be resilient and adaptable to changing market conditions.

In the context of trading strategy development and testing, the historical data is divided into IS and OOS segments. The strategy is developed and optimized using the IS data, and its performance is then validated using the OOS data. Users can compare the performance metrics from both datasets to assess the strategy’s robustness and avoid overfitting.

### **Benefits of In-Sample and Out-of-Sample Testing:**

1. Robustness Testing: By comparing the strategy’s performance on IS and OOS data, users can assess its robustness and generalizability. A strategy that performs well on both datasets is less likely to be overfit and more likely to perform well in live trading.
2. Validation of Strategy Concepts: If a trading strategy performs well on OOS data, it provides additional confidence that the underlying trading idea is sound and not just a result of curve-fitting or random chance.
3. Prevention of Overfitting: The use of OOS data helps prevent overfitting, as it forces the strategy to prove its effectiveness on an unseen dataset. This ensures that the strategy is not overly optimized for a specific dataset and can adapt to changing market conditions.

## **Monte Carlo Tests**

Monte Carlo Tests are an integral part of StrategyQuant X’s robustness testing tools, allowing users to assess the stability and reliability of their trading strategies under various random scenarios. By performing Monte Carlo simulations, users can gain insights into the potential range of outcomes for their strategies and evaluate their resilience to unexpected market events and conditions. The following is an extensive explanation of Monte Carlo Tests in StrategyQuant X:

**What are Monte Carlo Tests:**

Monte Carlo Tests are a statistical method that involves simulating a large number of random scenarios to evaluate the performance of a trading strategy. These tests generate random variations in factors such as trade order, trade slippage, starting capital, and position sizing to analyze the strategy’s performance under different conditions. By examining the outcomes of these simulations, users can assess the stability and reliability of their trading strategies.

The primary goal of Monte Carlo Tests is to evaluate the robustness and adaptability of trading strategies. By analyzing the strategy’s performance under various random scenarios, users can gain insights into its potential range of outcomes and identify potential weaknesses or vulnerabilities. This information helps users understand the risks associated with their strategies and make informed decisions about whether to deploy them in live trading environments.

In StrategyQuant X you can use two types of Monte Carlo tests:

### **Monte Carlo trade manipulation**

This cross-check runs simulations where in each simulation the existing trades are manipulated – they are reshuffled, some are omitted, etc.  
This function is very fast, as it does not require backtests, but works with the existing trades from the main backtest.  
The idea behind it is to check how much the curve of the strategy depends on the order of the trades and what happens if some trades are not executed.  
  
You can perform these trade manipulations in any simulation:

- **Randomize Trades Order** – this is the simplest test, where the order of trades is randomly shuffled. This does not change the net profit, but it is very useful to study the different variations of drawdown that can result from the different order of trades.
- **Skip trades randomly** – randomly skip trades with a certain probability. In real trading, you may miss a trade due to a platform or internet error, or simply because you stop trading for some time. This test gives you an idea of what the stock curve might look like if some trades are skipped randomly.

### **Monte Carlo retest methods**

This is another type of Monte Carlo simulation. In this case, random changes in properties are simulated that require strategy re-testing – e.g., changes in spread, slippage, strategy parameters, or historical data.  
  
Since a full backtest is required for each simulation, this cross-check can take a long time.  
For example, if the backtest with the main data took 0.5 seconds and you want to run 100 simulations in this cross-check, you can assume that it will take 100 x 0.5 = 50 seconds for each strategy where it is applied.  
  
We will test the following types of **Monte Carlo retest methods**

- **Randomize Strategy Parameters –** each strategy uses parameters, such as the period of an indicator or a constant, for comparison. This test checks how sensitive the strategy is to a small change in the parameter value. The probability of change is the probability that a parameter will change its value. Max. Parameter Change is the maximum percentage by which the parameter changes its value. For example, if you set Max. Parameter Change to 10%, a parameter with the value 60 can be randomly changed to a range of 54 – 66 (+- 10% of its original value of 60).
- **Randomize history data** – a very common case of curve fitting is when the strategy depends too much on the history data. This option checks the behavior of the strategy when the history data is changed.

## **OOS/IS Ratios**

Represents the ratio of metrics in the out-of-sample period to metrics in the in-sample period

The ratio OOS / IS expresses the degree of deterioration of the strategy in the out-of-sample period compared to the in-sample period

Strategy deterioration refers to the deterioration in the performance of a trading strategy when it is applied to new data that has not yet been seen. This deterioration often occurs when a strategy that has been optimized and fine-tuned for in-sample data (IS) is tested on out-of-sample data (OOS). The deterioration may be due to overfitting the strategy to the IS data or to changing market conditions to which the strategy cannot adapt.

Out-of-sample (OOS) and in-sample (IS) metrics are critical concepts in the development, testing and validation of trading strategies. These metrics help evaluate the robustness of strategies and ensure that they are not overfitted to a particular data set.

The main objective of using OOS / IS ratios is to evaluate the robustness and generalizability of trading strategies. By evaluating the performance of the strategy on both IS and OOS data, you can identify potential overfitting and ensure that the strategy is not overly optimized for a particular data set. A strategy that performs well on both IS and OOS is likely to be more resilient and adaptable to changing market conditions.

## **Multi Market Tests ( Testing on additional markets )**

Multi-Market Testing is an essential feature of StrategyQuant X that allows you to evaluate the performance and adaptability of your trading strategies for different financial instruments or market conditions. This testing approach aims to identify strategies that are not only effective in a single market, but also perform well in different market environments, reducing the risk of overfitting and increasing the probability of successful trading performance.

The main purpose of the multi-market test is to determine the robustness and flexibility of a trading strategy. A strategy that performs well in multiple markets is likely to be more resilient and adaptable to changing market conditions because it has proven effective in a variety of circumstances. Testing multiple markets helps users identify strategies that are not overly optimized for a particular market. This reduces the risk of over-adaptation and improves the chances of success in live trading.

Multi-market testing is where you test your trading strategies on different financial instruments, such as stocks, currency pairs, commodities, and indices. With StrategyQuant X, you can select multiple instruments and run backtests on each instrument to evaluate the strategy’s performance. Then you’ll be able to analyze the results, compare performance metrics for different markets and identify strategies that show consistent performance and adaptability.

By testing strategies across multiple markets, users can identify systems that are more robust and less prone to over-adaptation. A strategy that performs well in multiple markets is likely to be more resilient to changing market conditions. Multi-market testing helps users build a diversified portfolio by identifying strategies that perform well in different instruments. This diversification can help reduce overall portfolio risk and improve long-term performance.

When a strategy concept performs well in multiple markets, it provides additional assurance that the underlying trading idea is sound and not just the result of curve fitting or chance.
