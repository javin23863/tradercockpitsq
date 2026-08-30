/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.utils.WizardConfigWorker;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;

public class XMLIndicatorsAnalyzer {
    private static final String IndyNameAttribute = "mI";
    private static final ArrayList<String> indicatorNames = new ArrayList();
    private static final HashMap<String, String> nameTranslations = new HashMap();
    private static final String ExtendedTemplateAttribute = "mt5ExtendedTemplate";
    private static final ArrayList<String> extendedTemplateKeys = new ArrayList();
    private static boolean initialized = false;

    public static synchronized void init() throws Exception {
        if (initialized) {
            return;
        }
        try {
            indicatorNames.clear();
            extendedTemplateKeys.clear();
            Element element = WizardConfigWorker.getWizardConfig();
            for (Element element2 : WizardConfigWorker.getElementsFromCategory(element, "Indicators", "indicator")) {
                indicatorNames.add(element2.getAttributeValue("key"));
            }
            indicatorNames.add("MovingAverage");
            indicatorNames.add("ATR");
            indicatorNames.add("TrueRange");
            indicatorNames.add("MTATR");
            indicatorNames.add("BBWidthRatio");
            indicatorNames.add("HighestInRange");
            indicatorNames.add("LowestInRange");
            indicatorNames.add("Trend");
            indicatorNames.add("HighestIndex");
            indicatorNames.add("LowestIndex");
            for (Element element2 : XMLUtil.getNestedElements(element, "Item")) {
                String string = element2.getAttributeValue(IndyNameAttribute);
                String string2 = element2.getAttributeValue("key");
                if (string != null && string2 != null && indicatorNames.contains(string)) {
                    nameTranslations.put(string2, string);
                }
                if (!XMLUtil.elementIs(element2, ExtendedTemplateAttribute)) continue;
                extendedTemplateKeys.add(string2);
            }
            initialized = true;
        }
        catch (Exception exception) {
            throw new Exception("XMLIndicatorsAnalyst init failed. Reason: " + exception.getMessage(), exception);
        }
    }

    public static void fixItemAttributes(Element element) {
        for (Element element2 : element.getChildren()) {
            String string;
            String string2 = element2.getAttributeValue("key");
            String string3 = element2.getAttributeValue(IndyNameAttribute);
            String string4 = indicatorNames.contains(string2) ? string2 : (indicatorNames.contains(string3) ? string3 : (string = nameTranslations.containsKey(string2) ? nameTranslations.get(string2) : null));
            if (element2.getName().equals("Block") || element2.getName().equals("Item")) {
                boolean bl;
                boolean bl2 = bl = string2 != null && (string2.equals("Volume") || string2.equals("VolumeRising") || string2.equals("VolumeFalling"));
                if (!bl && string != null) {
                    element2.setAttribute("indicatorName", string);
                }
                if (XMLUtil.elementIs(element2, ExtendedTemplateAttribute) && !extendedTemplateKeys.contains(string2)) {
                    element2.removeAttribute(ExtendedTemplateAttribute);
                }
            }
            XMLIndicatorsAnalyzer.fixItemAttributes(element2);
        }
    }
}

