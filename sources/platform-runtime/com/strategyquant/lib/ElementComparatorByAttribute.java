/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.XMLUtil;
import java.util.Comparator;
import org.jdom2.Element;

public class ElementComparatorByAttribute
implements Comparator<Element> {
    private String attributeName;

    public ElementComparatorByAttribute(String string) {
        this.attributeName = string;
    }

    @Override
    public int compare(Element element, Element element2) {
        String string = XMLUtil.getAttr(element, this.attributeName, "NA");
        String string2 = XMLUtil.getAttr(element2, this.attributeName, "NA");
        return string.compareTo(string2);
    }
}

