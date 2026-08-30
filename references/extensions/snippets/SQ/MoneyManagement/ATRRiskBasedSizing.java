package SQ.MoneyManagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="ATR Risk-Based Sizing", display="ATR Risk-Based Sizing: Risk #Risk#% of equity, Stop = #ATRMult# × ATR(#ATRPeriod#)")
@Help("<b>ATR Risk-Based Position Sizing</b><br>Adjusts trade size so each trade risks a fixed % of equity based on ATR volatility.")
@Description("Risk-based position sizing using ATR.")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class ATRRiskBasedSizing extends MoneyManagementMethod {

    public static final Logger Log = LoggerFactory.getLogger("ATRRiskBasedSizing");

    @Parameter(name="Risk in %", category="Risk", defaultValue="2", minValue=0.1, maxValue=100000, step=0.1)
    @Help("Percent of account equity to risk per trade")
    public double Risk;

    @Parameter(defaultValue="14", minValue=2, name="ATR Period", maxValue=480, step=1, category="ATR")
    @Help("ATR lookback period")
    public int ATRPeriod;

    @Parameter(defaultValue="2.0", minValue=0.1, name="ATR Multiplier", maxValue=10d, step=0.1, category="ATR")
    @Help("ATR multiple for stop distance")
    public double ATRMult;

    @Parameter(defaultValue="1", minValue=0d, name="Size Decimals", maxValue=6d, step=1d, category="Default")
    @Help("Order size rounding decimals. Use 2 for microlots, 1 for mini lots, 0 for stocks/futures.")
    public int Decimals;

    @Parameter(name="Size if no MM", category="Lots", defaultValue="1", minValue=0, maxValue=1000000000, step=0.1)
    @Help("Lots traded if computed trade size is 0 or MM disabled")
    public double LotsIfNoMM;

    @Parameter(name="Maximum lots", category="Lots", defaultValue="5", minValue=0.01, maxValue=1000000000, step=0.1)
    @Help("Maximum lot size allowed")
    public double MaxLots;

    //------------------------------------------------------------------------
    @Override
    public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
        if(Risk < 0) {
            throw new Exception("Money management wasn't properly initialized. Call init() before computing trade size!");
        }

        // 1) Get ATR value
        double atrValue = strategy.getATRValue(strategy.MarketData.Chart(symbol), ATRPeriod, 1);

        // 2) Calculate stop distance in ticks (ATR × ATRMult) ÷ tickSize))
        double stopDistanceTicks = (atrValue * ATRMult) / tickSize;

        // 3) Stop loss in money
        double slInMoney = SQUtils.round7(stopDistanceTicks * tickSize * pointValue);

        // 4) Max risk in money
        double riskPct = Risk * weight;
        if(riskPct > 100) riskPct = 100;
        double maxRisk = SQUtils.round7(strategy.getAccountEquity() * (riskPct / 100));

        // 5) Position size in lots
        double tradeSize = SQUtils.round7(maxRisk / slInMoney);

        // 6) Rounding and limits
        tradeSize = round(tradeSize, sizeStep, Decimals);
        if(tradeSize <= 0) tradeSize = LotsIfNoMM;
        if(tradeSize > MaxLots) tradeSize = MaxLots;

        return tradeSize;
    }
}
