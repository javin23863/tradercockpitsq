package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Formula {
   String name() default "Null";

   String formula() default "Null";

   int order() default 100;

   boolean noneValue() default false;

   boolean priceLevel() default false;
}
