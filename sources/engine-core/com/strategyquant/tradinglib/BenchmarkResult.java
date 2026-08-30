package com.strategyquant.tradinglib;

import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;

public class BenchmarkResult {
   public Long2DoubleAVLTreeMap equity = new Long2DoubleAVLTreeMap();
   public Long2DoubleAVLTreeMap ddMapMoney = new Long2DoubleAVLTreeMap();
   public Long2DoubleAVLTreeMap ddMapPct = new Long2DoubleAVLTreeMap();
}
