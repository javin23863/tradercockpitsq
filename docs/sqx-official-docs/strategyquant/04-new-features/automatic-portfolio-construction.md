# Automatic Portfolio Construction

- Source: <https://strategyquant.com/doc/strategyquant/automatic-portfolio-construction/>
- Section: StrategyQuant X › New features
- Fetched: 2026-09-04

---

This is a new feature in a new **Portfolio Composer** module of StrategyQuantX. To learn more about it check [Portfolio Composer](portfolio-composer.md), and the [difference between Portfolio Composer and Portfolio Master](portfolio-composer.md).

In short, Portfolio Composer allows you to simulate portfolio INCLUDING weights (how much money to allocate to each of the strategy in portfolio). You can choose the strategies you want to test, set their weights and hit the Recompute portfolio button to crate a portfolio simulation.

## Automatic computation of portfolio

The new feature of **Automatic Computation** adds an automated approach to this. You no longer need to specify the weights manually – you can use models like **Markowitz Efficient Frontier** (more will be added) to compute the optimal portfolio and strategy weights!

## Portfolio Composer: Automating Markowitz Efficient Frontier for Optimal Strategy Weighting

In the ever-evolving world of trading, constructing an optimal portfolio of strategies requires balancing return potential with acceptable levels of risk.

**Portfolio Composer** introduces a new level of automation by leveraging the **Markowitz Efficient Frontier** to compute the **optimal weights for each strategy**. This advanced feature ensures that traders can systematically achieve the highest possible return for a given level of risk. By automating these complex computations, Portfolio Composer not only simplifies the portfolio optimization process but also empowers traders to make data-driven decisions with confidence.

## What Is the Efficient Frontier?

The **Efficient Frontier** is a key concept in **Modern Portfolio Theory (MPT)** that represents a set of optimal portfolios offering the highest expected return for a given level of risk (or the lowest risk for a given level of return). Portfolios that lie on the efficient frontier dominate those that lie below it because they provide better returns for the same risk or less risk for the same return.

The efficient frontier is typically plotted as a curve on a graph where:

- **X-axis:** Represents risk, measured by portfolio standard deviation (volatility).
- **Y-axis:** Represents expected return of the portfolio.

Portfolios lying below the efficient frontier are considered **inefficient**, as better risk-return combinations are available. Portfolios on the frontier are optimal and are considered efficient.

[![](https://strategyquant.com/wp-content/uploads/2025/01/pc1-750x558.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/pc1-750x558.jpg)

**The Expected Return / standard deviation (volatility) represent the Sharpe ratio of the Portfolio.**

**The Optimal Portfolio is the portfolio with the highest Sharpe Ratio.**

**Who Created It?**

The efficient frontier was introduced by **Harry Markowitz**, an American economist, in his groundbreaking work on portfolio theory. His 1952 paper, **“Portfolio Selection”**, published in the *Journal of Finance*, laid the foundation for Modern Portfolio Theory. For this work, he was later awarded the **Nobel Memorial Prize in Economic Sciences** in **1990**.

## The Efficient Frontier is based on Daily Return, Portfolio Volatility, and Value at Risk:

### 1. Daily Return Calculation

The **daily return** measures the percentage change in the value of an asset or portfolio from one trading day to the next. For an asset, it is calculated as:

[![](https://strategyquant.com/wp-content/uploads/2025/01/picture2.png)](https://strategyquant.com/wp-content/uploads/2025/01/picture2.png)

- Pt​ is the price of the asset at time t.
- Pt−1 is the price of the asset on the previous day.

### 2. Strategy Volatility Calculation

The **volatility of a strategy** refers to how much the returns of that strategy fluctuate over time. It is commonly measured using the **standard deviation of daily returns**. High volatility indicates larger swings in returns, while low volatility suggests more stable performance. For a single trading strategy, the daily volatility can be calculated as follows:

[![](https://strategyquant.com/wp-content/uploads/2025/01/picture3.png)](https://strategyquant.com/wp-content/uploads/2025/01/picture3.png)

- Rt​ is the daily return of the strategy on day t.
- is the average daily return of the strategy over the period.
- N is the total number of trading days.
- Sigma of the strategy​ is the standard deviation, representing the strategy’s volatility. (or the risk)

[![](https://strategyquant.com/wp-content/uploads/2025/01/pc2-750x400.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/pc2-750x400.jpg)

As the return is fluctuating around the average daily return, we are calculating the volatility of the strategy with a 95 % confidence level, meaning that 95% of the point will be in this range and only 5 days out of 100 could be above the calculated volatility.

[![](https://strategyquant.com/wp-content/uploads/2025/01/pc3.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/pc3.jpg)

This calculation is assuming we are having a normal distribution of the return around the average daily return in normal market condition.

### 3. Confidence Level and VaR (Value at Risk)

At a **95% confidence level**, we can estimate the potential loss of the portfolio using **Value at Risk (VaR)**. VaR estimates the maximum expected loss over a specific time period, assuming normal market conditions. For a portfolio with normally distributed returns, VaR at 95% confidence is given by:

[![](https://strategyquant.com/wp-content/uploads/2025/01/picture7.png)](https://strategyquant.com/wp-content/uploads/2025/01/picture7.png)

Where:

- 1.65 the **z-score** corresponding to a 95% confidence level. (using a  z-table or a calculator to obtain the z-score)
- Sigma Portfolio​ is the daily portfolio volatility.
- T is the time horizon (e.g., 1 day, 5 days).

From the Expected Return, the Volatility and the Value at Risk, we can select the most efficient Portfolio.

## Application in Portfolio Composer:

### 1. First step a Portfolio Calculation:

[![](https://strategyquant.com/wp-content/uploads/2025/01/image4-750x426.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image4-750x426.jpg)

This first method analyzes one portfolio

If we recompute some selected strategies, we can read in the last line of the log for each strategy:

- The Daily expected return
- The Daily Standard Deviation (the risk of the strategy)
- The Value at Risk
- The correlation between strategies

Then we can read the portfolio information:

[![](https://strategyquant.com/wp-content/uploads/2025/01/picture8.png)](https://strategyquant.com/wp-content/uploads/2025/01/picture8.png)

- The **Daily expected return**
- The **Daily Standard Deviation** (the risk of the Portfolio)
- The **Value at Risk** of the Portfolio (VaR)
- The **Sharpe Ratio** of the Portfolio
- Then the **Expected Shortfall** (or the Conditional Value at Risk)

We are adding the expected shortfall when the market is not in normal condition and do not fit the normal distribution.

### 2. Automatic Portfolio Calculation:

This second method analyzes multiple portfolios with different weight combinations.

#### 2.1 The **Sharpe Ratio fitness** calculation:

We can select the strategies, configure the money management with its settings, then select the fitness type and the number of simulations, for example:

- We select 3 strategies
- In the Money Management tab, we enter a leverage of 100
- In the Automatic Computation tab, we select as **fitness: Sharpe Ratio**
- **500 or 1000 Simulations**
- **The Risk-Free Rate,** is based on a minimum risk asset like Bonds.

[![](https://strategyquant.com/wp-content/uploads/2025/01/image5-750x334.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image5-750x334.jpg)

We obtain the following results: the optimal portfolio is highlighted in yellow, representing the best Sharpe ratio (Returns/Standard Deviation).

Additionally, the portfolio with the minimum risk is highlighted in green.

**The optimal portfolios lie on the efficient frontier line. Depending on the standard deviation (Volatility), we can choose the portfolio that offers the maximum return.**

[![](https://strategyquant.com/wp-content/uploads/2025/01/image6-750x422.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image6-750x422.jpg)

#### 2.2 The **Return vs Drawdown Ratio** fitness calculation:

Alternatively, we can select the **Return/Drawdown Ratio fitness** to gain a different perspective on the results:

The optimal portfolio, in yellow, would be the lowest drawdown with the maximum profit. We can compare it to the minimum risk portfolio (in green).

**The optimal portfolios lie on the efficient frontier line. Depending on the drawdown, we can choose the portfolio that offers the maximum profit.**

[![](https://strategyquant.com/wp-content/uploads/2025/01/image7-750x425.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image7-750x425.jpg)

#### 2.3 The **Compound Annual Growth Rate vs Max Drawdown** fitness calculation:

Using **Compound Annual Growth Rate (CAGR)** vs. **Max** **Drawdown-based fitness** to compare portfolio weighting provides a good perspective between **Long-Term Growth and Risk sensitivity.**

To obtain the optimal portfolio, we will look for the **highest CAGR and the lowest Max drawdown**. (in yellow)

**The optimal portfolios lie on the efficient frontier line. Depending on the drawdown, we can choose the portfolio that offers the maximum CAGR.**

#### ![](https://strategyquant.com/wp-content/uploads/2025/01/image8-750x427.jpg)

#### 2.4 The **Compound Annual Growth Rate vs Average Drawdown** fitness calculation:

Using **the Compound Annual Growth Rate (CAGR) vs. Average Drawdown ratio** will be less sensitive to portfolios with outlier drawdowns.  
The optimal portfolios lie on the efficient frontier line. Depending on the drawdown, we can choose the portfolio that offers the maximum **CAGR**.

[![](https://strategyquant.com/wp-content/uploads/2025/01/image9-750x426.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image9-750x426.jpg)

### 3. AlgoCloud Stockpicker

For AlgoCloud Stockpicker, in the “***Configuration – Account MM***” tab, **the leverage should be set to 1,** as we are trading stocks. An initial capital of $25,000 can be used as a starting point for optimization.

The shape of the efficient frontier will be slightly different, as we are working with optimized strategies that trade across multiple instruments.

Since each strategy involves hundreds of instruments, more time is required to run the optimization when using multiple strategies.

Here is the **Efficient Frontier of the Sharpe Ratio** Fitness:

Here is the Efficient Frontier of the **CAGR Max Draw Down** Fitness:

[![](https://strategyquant.com/wp-content/uploads/2025/01/image10-750x423.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image10-750x423.jpg)

[![](https://strategyquant.com/wp-content/uploads/2025/01/image11-750x384.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image11-750x384.jpg)

Here is the Efficient Frontier of the **Return vs Drawdown** Fitness:

[![](https://strategyquant.com/wp-content/uploads/2025/01/image12-750x413.jpg)](https://strategyquant.com/wp-content/uploads/2025/01/image12-750x413.jpg)

***For references and to gain a more complete understanding:***

***Books***

- *“Portfolio Selection” by Harry Markowitz, Part III: “Efficient Portfolios”, Chapter VII: “Geometric Analysis” (Yale University Press)*[*An excerpt*](https://www.math.hkust.edu.hk/~maykwok/courses/ma362/07F/markowitz_JF.pdf)
- *“Modern Portfolio Theory and Investment Analysis” by Edwin Elton, Martin Gruber, Stephen Brown, and William Goetzmann (Wiley Custom Publishing), Part II: “Portfolio Analysis”, Chapters 6, 9, and 11: “Techniques for Calculating the Efficient Frontier”*
- *“The Complete Guide to Portfolio Performance” by Pascal François and Georges Hubner (Wiley Custom Publishing), with its “Classical Performance Measures Revisited” (Chapter 6)*
- *“Smart Portfolios” and “Systematic Trading” by Robert Carver*

*Note: We have other good books by Harry Markowitz and others, but they are more theoretical.*

***On the Web***

- ***The Efficient Frontier:***[*Investopedia – Efficient Frontier*](https://www.investopedia.com/terms/h/harrymarkowitz.asp)[*Corporate Finance Institute – Efficient Frontier*](https://corporatefinanceinstitute.com/resources/career-map/sell-side/capital-markets/efficient-frontier/)

*Darwinex and Ryan O’Connell made excellent videos on YouTube regarding portfolio optimization and the efficient frontier.*

- ***The Confidence Interval:***[*Scribbr – Confidence Interval*](https://www.scribbr.com/statistics/confidence-interval/)
- ***To get the Z variable from a confidence level:***[*Study.com – How to Find the Critical Z-Value*](https://study.com/skill/learn/how-to-find-the-critical-z-value-for-a-given-confidence-level-explanation.html)
