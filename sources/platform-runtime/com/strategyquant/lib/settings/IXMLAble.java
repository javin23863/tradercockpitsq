/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.settings;

import org.jdom2.Element;

public interface IXMLAble {
    public Element getXML();

    public void setFromXML(Element var1) throws Exception;
}

