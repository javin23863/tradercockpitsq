/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomBlocksAnalyzer {
    public static final Logger Log = LoggerFactory.getLogger((String)"CustomBlocksAnalyzer");

    public static void fixCBParameters(Element element) {
        for (Element element2 : element.getChildren()) {
            String string = element2.getAttributeValue("key");
            if (string != null && string.startsWith("CBlock_")) {
                CustomBlocksAnalyzer.fixCBlockParams(string, element2);
            }
            CustomBlocksAnalyzer.fixCBParameters(element2);
        }
    }

    private static void fixCBlockParams(String string, Element element) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        Element element2 = null;
        for (Element element3 : element.getChildren()) {
            if (element3.getName().equals("Param")) {
                String string2 = element3.getAttributeValue("key");
                String string3 = element3.getText();
                hashMap.put(string2, string3);
                continue;
            }
            if (!element3.getName().equals("Contents")) continue;
            element2 = element3;
        }
        if (element2 == null) {
            Log.error("No Contents found for CBlock with key: {}", (Object)string);
            return;
        }
        if (hashMap.size() == 0) {
            Log.debug("No params to replace for CBlock with key: {}", (Object)string);
            return;
        }
        CustomBlocksAnalyzer.fixCBlockSingleParam(string, element2, hashMap);
    }

    private static void fixCBlockSingleParam(String string, Element element, HashMap<String, String> hashMap) {
        for (Element element2 : element.getChildren()) {
            String string2;
            if (element2.getName().equals("Param") && (string2 = element2.getAttributeValue("value")) != null && string2.startsWith("#")) {
                if (hashMap.containsKey(string2)) {
                    String string3 = hashMap.get(string2);
                    if (Log.isDebugEnabled()) {
                        String string4 = element2.getText();
                        String string5 = element2.getAttributeValue("key");
                        Log.debug("CBlock: {}, param: {} - replacing {} with {}", new Object[]{string, string5, string4, string3});
                    }
                    element2.setText(string3);
                } else {
                    Log.debug("No param {} found for CBlock with key: {}", (Object)string2, (Object)string);
                }
            }
            CustomBlocksAnalyzer.fixCBlockSingleParam(string, element2, hashMap);
        }
    }
}

