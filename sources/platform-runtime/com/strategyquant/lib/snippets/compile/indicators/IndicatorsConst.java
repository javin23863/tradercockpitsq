/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.indicators;

public class IndicatorsConst {
    public static final String INDICATOR_TEMPLATE = "\tpublic [indicator_name] [indicator_name]([indicator_parameters]) throws TradingException {\n\t\tlong key = [indicator_key]\n\t\t[indicator_name] indicator;\n\n\t\tif(!indicatorsCache.containsKey(key)) {\n\t\t\tindicator = new [indicator_name]();\n[indicator_initialization]\n\t\t\tindicator.initialize(this, hasZeroShift);\n\t\t\tindicator.initializeStrategy(Strategy);\n\n\t\t\tindicatorsCache.put(key, indicator);\n\t\t}\n\n\t\tindicator = ([indicator_name]) indicatorsCache.get(key);\n\n\t\tindicator.refreshShift();\n\n\t\treturn indicator;\n\t}";
    public static final String INDICATOR_PACKAGE_TEMPLATE = "package SQ.Internal;\n\n[imports]\n/**\n * this is a cache class that caches all indicators used in a trading setup\n * @author Mark Fric\n */\n public class Indicators extends IndicatorsObj {\n     private boolean hasZeroShift;\n\n     public void setHasZeroShift(boolean hasZeroShift) {\n         this.hasZeroShift = hasZeroShift;\n     }\n\n\t private StrategyBase Strategy;\n\t public Indicators(StrategyBase strategy) {\n\t\t this.Strategy = strategy;\n\t }\n\n[indicators]}";
    public static final String INTERNAL_INCLUDE_PKGNAME = "SQ.Internal";

    private IndicatorsConst() {
    }
}

