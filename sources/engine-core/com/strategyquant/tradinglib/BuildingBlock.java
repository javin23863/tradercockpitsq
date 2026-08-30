package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BuildingBlock {
   String name() default "Null";

   String display() default "Null";

   int returnType() default 1;

   String mainIndicator() default "Null";
}
