package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Indicator {
   boolean oscillator() default false;

   double middleValue() default 0.0;

   double min() default -1000.0;

   double max() default 1000.0;

   double step() default 1.0;
}
