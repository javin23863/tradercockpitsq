/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import com.strategyquant.lib.XMLUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WizardConfigWorker {
    private static final String ignoreInBuilder = "ignoreInBuilder";

    public static ArrayList<Element> getElementsFromElement(Element element, String string, String string2) throws Exception {
        ArrayList<Element> arrayList = XMLUtil.getNestedElements(element, string);
        if (arrayList.size() == 0) {
            throw new Exception("Category elements not found in wizard config");
        }
        int n = 0;
        for (int i = 0; i < arrayList.size(); ++i) {
            String string3 = arrayList.get(i).getAttributeValue("key");
            if (string3 == null || !string3.equals(string)) continue;
            n = i;
            break;
        }
        ArrayList<Element> arrayList2 = XMLUtil.getNestedElements(arrayList.get(n), "Item");
        return WizardConfigWorker.filterElements(arrayList2, string2);
    }

    public static ArrayList<Element> getElementsFromCategory(Element element, String string, String string2) throws Exception {
        ArrayList<Element> arrayList = XMLUtil.getNestedElements(element, "Category");
        if (arrayList.size() == 0) {
            throw new Exception("Category elements not found in wizard config");
        }
        for (Element element2 : arrayList) {
            String string3 = element2.getAttributeValue("key");
            if (string3 == null || !string3.equals(string)) continue;
            return WizardConfigWorker.filterElements(element2.getChildren("Item"), string2);
        }
        return null;
    }

    private static ArrayList<Element> filterElements(List<Element> list, String string) throws Exception {
        ArrayList<Element> arrayList = new ArrayList<Element>();
        for (Element element : list) {
            if (!XMLUtil.elementIsNot(element, ignoreInBuilder) || !WizardConfigWorker.elementIsOfCategoryType(element, string)) continue;
            arrayList.add(element);
        }
        return arrayList;
    }

    public static Element getWizardConfig() throws Exception {
        File file = new File(System.getProperty("user.dir") + "/internal/web/SQWIZARD/branding/global/config.xml");
        if (!file.exists()) {
            throw new Exception("Wizard config file '" + file.getAbsolutePath() + "' not found.");
        }
        return XMLUtil.fileToXmlElement(file);
    }

    public static JSONObject getBlockInfo(Element element, int n) throws JSONException, Exception {
        Object object;
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("key", (Object)element.getAttributeValue("key"));
        jSONObject.put("name", (Object)element.getAttributeValue("name"));
        jSONObject.put("returnType", (Object)element.getAttributeValue("returnType"));
        jSONObject.put("customSnippet", XMLUtil.elementIs(element, "customSnippet"));
        jSONObject.put("weight", n);
        jSONObject.put("originalWeight", n);
        jSONObject.put("params", (Object)WizardConfigWorker.getBlockParameters(element));
        String string = element.getAttributeValue("indicatorMin");
        if (string != null) {
            object = element.getAttributeValue("indicatorMax");
            String string2 = element.getAttributeValue("indicatorStep");
            jSONObject.put("indicatorMin", (Object)string);
            jSONObject.put("indicatorMax", object);
            jSONObject.put("indicatorStep", (Object)string2);
            jSONObject.put("originalIndicatorMin", (Object)string);
            jSONObject.put("originalIndicatorMax", object);
            jSONObject.put("originalIndicatorStep", (Object)string2);
        }
        if ((object = element.getParentElement()) != null && object.getName().equals("Category")) {
            jSONObject.put("group", (Object)object.getAttributeValue("name"));
            jSONObject.put("groupKey", (Object)object.getAttributeValue("key"));
        }
        jSONObject.put("forEngine", (Object)XMLUtil.getAttr(element, "forEngine", "*"));
        return jSONObject;
    }

    private static JSONArray getBlockParameters(Element element) throws Exception {
        JSONArray jSONArray = new JSONArray();
        String string = element.getAttributeValue("key");
        if (string.equals("SQ.Formulas.RangeLevel.PriceLevel")) {
            return jSONArray;
        }
        for (Element element2 : XMLUtil.getNestedElements(element, "Param")) {
            JSONObject jSONObject = new JSONObject();
            String string2 = element2.getAttributeValue("key");
            if (string2 == null) continue;
            boolean bl = string2.equals("#Direction#");
            boolean bl2 = string2.equals("#MagicNumber#");
            if (bl || bl2) continue;
            String string3 = element2.getAttributeValue("defaultValue");
            jSONObject.put("key", (Object)string2);
            jSONObject.put("name", (Object)element2.getAttributeValue("name"));
            jSONObject.put("type", (Object)element2.getAttributeValue("type"));
            jSONObject.put("weight", 1);
            jSONObject.put("originalWeight", 1);
            jSONObject.put("generation", (Object)WizardConfigWorker.getGeneration(element2));
            jSONObject.put("defaultValue", (Object)string3);
            jSONObject.put("step", (Object)element2.getAttributeValue("step"));
            jSONObject.put("values", (Object)element2.getAttributeValue("values"));
            jSONObject.put("exitMethod", (Object)element2.getAttributeValue("exitMethod"));
            String string4 = element2.getAttributeValue("minValue");
            String string5 = element2.getAttributeValue("maxValue");
            String string6 = element2.getAttributeValue("genMinValue");
            String string7 = element2.getAttributeValue("genMaxValue");
            String string8 = element2.getAttributeValue("builderMinValue");
            String string9 = element2.getAttributeValue("builderMaxValue");
            String string10 = element2.getAttributeValue("builderStep");
            if (string.equals("SQ.Formulas.Size.DefineOwnSize") && string2.equals("#Value#")) {
                jSONObject.put("generation", (Object)"fixed");
                jSONObject.put("fixedValue", (Object)string3);
            }
            if (string2.equals("#AllowDuplicateTrades#") || string2.equals("#ReplaceExisting#") && (string.equals("EnterAtStop") || string.equals("EnterAtLimit"))) {
                jSONObject.put("generation", (Object)"fixed");
                jSONObject.put("fixedValue", (Object)string3);
            }
            if (string2.equals("#Chart#")) {
                jSONObject.put("allCharts", true);
            }
            jSONObject.put("originalMinValue", (Object)string4);
            jSONObject.put("originalMaxValue", (Object)string5);
            if (string6 != null) {
                jSONObject.put("genMinValue", (Object)string6);
                jSONObject.put("minValue", (Object)string6);
            } else {
                jSONObject.put("minValue", (Object)(string8 != null && Double.parseDouble(string8) > Double.parseDouble(string4) ? string8 : string4));
            }
            if (string7 != null) {
                jSONObject.put("genMaxValue", (Object)string7);
                jSONObject.put("maxValue", (Object)string7);
            } else {
                jSONObject.put("maxValue", (Object)(string9 != null && Double.parseDouble(string9) < Double.parseDouble(string5) ? string9 : string5));
            }
            if (string10 != null) {
                jSONObject.put("step", (Object)string10);
            }
            jSONArray.put((Object)jSONObject);
        }
        return jSONArray;
    }

    private static String getGeneration(Element element) {
        String string = element.getAttributeValue("type");
        boolean bl = XMLUtil.elementIs(element, "isFormula");
        if (bl) {
            return "formula";
        }
        if (string.equals("string")) {
            return "fixed";
        }
        return "random";
    }

    public static JSONObject getSpecialItemInfo(Element element) throws JSONException, Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("key", (Object)element.getAttributeValue("key"));
        jSONObject.put("name", (Object)element.getAttributeValue("name"));
        jSONObject.put("type", (Object)element.getAttributeValue("type"));
        jSONObject.put("probability", (Object)element.getAttributeValue("probability"));
        jSONObject.put("customSnippet", XMLUtil.elementIs(element, "customSnippet"));
        jSONObject.put("forEngine", (Object)XMLUtil.getAttr(element, "forEngine", "*"));
        JSONArray jSONArray = WizardConfigWorker.getSpecialItemParameters(element);
        jSONObject.put("items", (Object)jSONArray);
        return jSONObject;
    }

    private static JSONArray getSpecialItemParameters(Element element) throws Exception {
        JSONArray jSONArray = new JSONArray();
        ArrayList<Element> arrayList = XMLUtil.getNestedElements(element, "Item");
        if (arrayList.size() > 0) {
            for (Element element2 : arrayList) {
                if (!XMLUtil.elementIsNot(element2, "noneValue") || !XMLUtil.elementIsNot(element2, ignoreInBuilder)) continue;
                JSONObject jSONObject = new JSONObject();
                String string = element2.getAttributeValue("key");
                if (string == null) continue;
                jSONObject.put("key", (Object)string);
                jSONObject.put("name", (Object)element2.getAttributeValue("name"));
                jSONObject.put("weight", 1);
                jSONObject.put("originalWeight", 1);
                jSONObject.put("generatedWeight", 1);
                jSONObject.put("use", !string.endsWith("PriceLevel"));
                jSONObject.put("probability", (Object)element2.getAttributeValue("probability"));
                jSONObject.put("params", (Object)WizardConfigWorker.getBlockParameters(element2));
                jSONArray.put((Object)jSONObject);
            }
        } else {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("params", (Object)WizardConfigWorker.getBlockParameters(element));
            jSONArray.put((Object)jSONObject);
        }
        return jSONArray;
    }

    private static boolean elementIsOfCategoryType(Element element, String string) {
        String string2 = element.getAttributeValue("categoryType");
        return string == null || string != null && string2 != null && string2.equals(string);
    }
}

