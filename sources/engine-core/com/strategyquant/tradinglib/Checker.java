package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.debug.Debugger;
import org.jdom2.Element;

public abstract class Checker extends Debugger {
   public abstract boolean check(Element var1);
}
