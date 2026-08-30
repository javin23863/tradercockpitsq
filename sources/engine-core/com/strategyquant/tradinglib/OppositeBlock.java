package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OppositeBlock {
   String value() default "Null";

   boolean oscillator() default false;

   double middleValue() default 0.0;

   String field() default "Null";
}
