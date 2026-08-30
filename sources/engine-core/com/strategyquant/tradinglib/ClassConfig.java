package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ClassConfig {
   String name() default "Null";

   String category() default "Null";

   String display() default "Null";

   int returnType() default 1;

   int order() default 100;
}
