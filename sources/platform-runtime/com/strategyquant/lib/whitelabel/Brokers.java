/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.whitelabel;

import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.lib.whitelabel.broker.BlackwellBroker;
import com.strategyquant.lib.whitelabel.broker.RoboForexBroker;
import java.util.HashMap;

public class Brokers {
    private static HashMap<String, AbstractBroker> brokers = new HashMap();

    public static void init() {
        RoboForexBroker roboForexBroker = new RoboForexBroker();
        brokers.put(roboForexBroker.getCode(), roboForexBroker);
        BlackwellBroker blackwellBroker = new BlackwellBroker();
        brokers.put(blackwellBroker.getCode(), blackwellBroker);
    }

    public static AbstractBroker get(String string) {
        return brokers.get(string);
    }
}

