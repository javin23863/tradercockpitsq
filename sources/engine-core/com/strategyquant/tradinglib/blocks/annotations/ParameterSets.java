package com.strategyquant.tradinglib.blocks.annotations;

import com.strategyquant.tradinglib.ParameterSet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterSets {
   ParameterSet[] value();
}
