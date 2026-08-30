package com.strategyquant.tradinglib.blocks.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DataSeriesChoice {
   String key() default "ComputedFrom";

   String name() default "Computed From";

   String category() default "Default";

   String help() default "Null";
}
