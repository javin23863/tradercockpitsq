package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParameterSets.class)
public @interface ParameterSet {
   int weight() default 1;

   String set() default "";
}
