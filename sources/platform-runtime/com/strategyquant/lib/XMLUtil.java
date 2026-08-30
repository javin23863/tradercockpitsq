/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLUtil {
    private static final Logger Log = LoggerFactory.getLogger(SQUtils.class);
    private static final XMLOutputter rawOutputter = new XMLOutputter(Format.getRawFormat());

    public static Element getChildElem(Element element, String string) throws Exception {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            throw new Exception("Element '" + string + "' not found");
        }
        return element2;
    }

    public static Element getChildElem(Element element, String string, String string2) throws Exception {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return XMLUtil.stringToElement(string2);
        }
        return element2;
    }

    public static boolean hasChildElem(Element element, String string) {
        Element element2 = element.getChild(string);
        return element2 != null;
    }

    public static boolean elementIsNot(Element element, String string) {
        String string2 = element.getAttributeValue(string);
        return string2 == null || !string2.equals("true");
    }

    public static boolean elementIs(Element element, String string) {
        return !XMLUtil.elementIsNot(element, string);
    }

    public static Element valueToElement(String string, Object object) {
        Element element = null;
        if (object instanceof IXMLAble) {
            element = new Element(string);
            element.setAttribute("type", object.getClass().getName());
            element.addContent((Content)((IXMLAble)object).getXML());
        } else if (object instanceof String) {
            element = new Element(string);
            element.setAttribute("type", "String");
            element.addContent((String)object);
        } else if (object instanceof Byte) {
            element = new Element(string);
            element.setAttribute("type", "Byte");
            element.addContent(((Byte)object).toString());
        } else if (object instanceof Short) {
            element = new Element(string);
            element.setAttribute("type", "Short");
            element.addContent(((Short)object).toString());
        } else if (object instanceof Integer) {
            element = new Element(string);
            element.setAttribute("type", "Integer");
            element.addContent(((Integer)object).toString());
        } else if (object instanceof Long) {
            element = new Element(string);
            element.setAttribute("type", "Long");
            element.addContent(((Long)object).toString());
        } else if (object instanceof Float) {
            element = new Element(string);
            element.setAttribute("type", "Float");
            element.addContent(((Float)object).toString());
        } else if (object instanceof Double) {
            element = new Element(string);
            element.setAttribute("type", "Double");
            element.addContent(((Double)object).toString());
        } else if (object instanceof Element) {
            element = new Element(string);
            element.setAttribute("type", "Element");
            element.addContent((Content)((Element)object).clone());
        } else if (object instanceof Boolean) {
            element = new Element(string);
            element.setAttribute("type", "Boolean");
            element.addContent(((Boolean)object).toString());
        } else {
            return null;
        }
        return element;
    }

    public static Object elementToValue(Element element) {
        Content content = element.getContent(0);
        String string = element.getAttributeValue("type");
        Object object = null;
        if (string == null) {
            return null;
        }
        switch (string) {
            case "String": {
                object = content.getValue().intern();
                break;
            }
            case "Byte": {
                object = Byte.parseByte(content.getValue());
                break;
            }
            case "Short": {
                object = Short.parseShort(content.getValue());
                break;
            }
            case "Integer": {
                object = Integer.parseInt(content.getValue());
                break;
            }
            case "Long": {
                object = Long.parseLong(content.getValue());
                break;
            }
            case "Float": {
                object = Float.valueOf(Float.parseFloat(content.getValue()));
                break;
            }
            case "Double": {
                object = Double.parseDouble(content.getValue());
                break;
            }
            case "Element": {
                object = (Element)element.getChildren().get(0);
                break;
            }
            case "Boolean": {
                object = Boolean.parseBoolean(content.getValue());
                break;
            }
            default: {
                Throwable throwable = null;
                try {
                    return XMLUtil.createInstance(string, (Element)element.getChildren().get(0));
                }
                catch (Exception exception) {
                    throwable = null;
                    try {
                        return XMLUtil.createInstance(string, element);
                    }
                    catch (Exception exception2) {
                        Log.error("Error while instantiating object of type using child: '" + string + "'. Exc.", throwable);
                        Log.error("Error while instantiating object of type using element: '" + string + "'. Exc.", (Throwable)exception2);
                    }
                }
            }
        }
        return object;
    }

    public static int valueHash(int n, Object object) {
        if (object instanceof IXMLAble) {
            return n + ((IXMLAble)object).getXML().hashCode();
        }
        if (object instanceof String) {
            return n + ((String)object).hashCode();
        }
        if (object instanceof Byte) {
            return n + ((Byte)object).hashCode();
        }
        if (object instanceof Short) {
            return n + ((Short)object).hashCode();
        }
        if (object instanceof Integer) {
            return n + ((Integer)object).hashCode();
        }
        if (object instanceof Long) {
            return n + ((Long)object).hashCode();
        }
        if (object instanceof Float) {
            return n + ((Float)object).hashCode();
        }
        if (object instanceof Double) {
            return n + ((Double)object).hashCode();
        }
        if (object instanceof Element) {
            return n + ((Element)object).hashCode();
        }
        return 0;
    }

    private static Object createInstance(String string, Element element) throws Exception {
        URLClassLoader uRLClassLoader;
        Class<?> clazz = null;
        if (!string.startsWith("SQ.")) {
            if (string.contains("com.strategyquant.lib.trading.")) {
                string = string.replace("com.strategyquant.lib.trading.", "com.strategyquant.tradinglib.");
            } else if (string.contains("com.strategyquant.lib.optimization.")) {
                string = string.replace("com.strategyquant.lib.optimization.", "com.strategyquant.tradinglib.optimization.");
            }
            if (string.endsWith("SQStats")) {
                string = "com.strategyquant.tradinglib.SQStats";
            }
            clazz = Class.forName(string);
        } else {
            uRLClassLoader = MainApp.getSnippetsClassLoader();
            clazz = uRLClassLoader.loadClass(string);
        }
        if (clazz != null) {
            uRLClassLoader = clazz.newInstance();
            Method method = clazz.getMethod("setFromXML", Element.class);
            method.invoke((Object)uRLClassLoader, element);
            return uRLClassLoader;
        }
        return null;
    }

    public static Element stringToElement(String string) throws JDOMException, IOException {
        SAXBuilder sAXBuilder = new SAXBuilder();
        sAXBuilder.setIgnoringElementContentWhitespace(true);
        sAXBuilder.setIgnoringBoundaryWhitespace(true);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        try {
            Document document = sAXBuilder.build((InputStream)byteArrayInputStream);
            return document.detachRootElement();
        }
        catch (JDOMParseException jDOMParseException) {
            string = string.replaceAll("&(?!(amp;|lt;|gt;|quot;|apos;))", "&amp;");
            byteArrayInputStream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
            Document document = sAXBuilder.build((InputStream)byteArrayInputStream);
            return document.detachRootElement();
        }
    }

    public static String elementToString(Element element) {
        if (element == null) {
            return "<<null>>";
        }
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xMLOutputter = new XMLOutputter(format);
        return xMLOutputter.outputString(element);
    }

    public static String elementLineToString(Element element) {
        if (element == null) {
            return "<<null>>";
        }
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        element = element.clone();
        element.clone();
        XMLOutputter xMLOutputter = new XMLOutputter(format);
        return xMLOutputter.outputString(element);
    }

    public static String elementToString(Element element, boolean bl) {
        Format format = bl ? Format.getPrettyFormat() : Format.getRawFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xMLOutputter = new XMLOutputter(format);
        return xMLOutputter.outputString(element);
    }

    public static ArrayList<Element> getNestedElements(Element element, String string) {
        ArrayList<Element> arrayList = new ArrayList<Element>();
        XMLUtil.loadAllChildElements(element, string, null, null, arrayList);
        return arrayList;
    }

    public static ArrayList<Element> getNestedElements(Element element, String string, String string2, String string3) {
        ArrayList<Element> arrayList = new ArrayList<Element>();
        XMLUtil.loadAllChildElements(element, string, string2, string3, arrayList);
        return arrayList;
    }

    private static void loadAllChildElements(Element element, String string, String string2, String string3, ArrayList<Element> arrayList) {
        List list = element.getChildren();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                Element element2 = (Element)list.get(i);
                if (element2.getName().equals(string) && (string3 == null || string3.equals(element2.getAttributeValue(string2)))) {
                    arrayList.add(element2);
                }
                XMLUtil.loadAllChildElements(element2, string, string2, string3, arrayList);
            }
        }
    }

    public static Element findFirst(Element element, String string) {
        if (element.getName().equals(string)) {
            return element;
        }
        List list = element.getChildren();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                Element element2 = (Element)list.get(i);
                if (element2.getName().equals(string)) {
                    return element2;
                }
                Element element3 = XMLUtil.findFirst(element2, string);
                if (element3 == null) continue;
                return element3;
            }
        }
        return null;
    }

    public static void findAll(Element element, String string, ArrayList<Element> arrayList) {
        List list = element.getChildren();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                Element element2 = (Element)list.get(i);
                if (element2.getName().equals(string)) {
                    arrayList.add(element2);
                }
                XMLUtil.findAll(element2, string, arrayList);
            }
        }
    }

    public static void findAllWithKey(Element element, String string, String string2, ArrayList<Element> arrayList) {
        List list = element.getChildren();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                String string3;
                Element element2 = (Element)list.get(i);
                if (element2.getName().equals(string) && (string3 = element2.getAttributeValue("key")) != null && string3.equals(string2)) {
                    arrayList.add(element2);
                }
                XMLUtil.findAllWithKey(element2, string, string2, arrayList);
            }
        }
    }

    public static Element findFirstWithKey(Element element, String string, String string2) {
        Object object;
        if (element.getName().equals(string) && (object = element.getAttributeValue("key")) != null && ((String)object).equals(string2)) {
            return element;
        }
        object = element.getChildren();
        if (object != null && object.size() > 0) {
            for (int i = 0; i < object.size(); ++i) {
                String string3;
                Element element2 = (Element)object.get(i);
                if (element2.getName().equals(string) && (string3 = element2.getAttributeValue("key")) != null && string3.equals(string2)) {
                    return element2;
                }
                string3 = XMLUtil.findFirstWithKey(element2, string, string2);
                if (string3 == null) continue;
                return string3;
            }
        }
        return null;
    }

    public static String getAttr(Element element, String string) throws Exception {
        if (element == null) {
            throw new Exception("Null element.");
        }
        String string2 = element.getAttributeValue(string);
        if (string2 == null) {
            throw new Exception("Attribute '" + string + "' not found.");
        }
        return string2;
    }

    public static String getAttr(Element element, String string, String string2) {
        try {
            if (element == null) {
                return string2;
            }
            String string3 = element.getAttributeValue(string);
            if (string3 == null) {
                return string2;
            }
            return string3;
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return string2;
        }
    }

    public static String getStringAttr(Element element, String string, String string2) {
        try {
            String string3 = element.getAttributeValue(string);
            if (string3 == null) {
                return string2;
            }
            return string3;
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return string2;
        }
    }

    public static byte getByteAttr(Element element, String string, byte by) {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                return by;
            }
            return Byte.parseByte(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return by;
        }
    }

    public static int getIntAttr(Element element, String string, int n) {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                return n;
            }
            return Integer.parseInt(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return n;
        }
    }

    public static int getIntAttr(Element element, String string) throws Exception {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                throw new Exception("Attribute '" + string + "' not found.");
            }
            return Integer.parseInt(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get int attribute '" + string + "'", (Throwable)exception);
            throw new Exception("Cannot get int attribute '" + string + "'", exception);
        }
    }

    public static long getLongAttr(Element element, String string) throws Exception {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                throw new Exception("Attribute '" + string + "' not found.");
            }
            return Long.parseLong(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get long attribute '" + string + "'", (Throwable)exception);
            throw new Exception("Cannot get long attribute '" + string + "'", exception);
        }
    }

    public static boolean getBooleanAttr(Element element, String string) throws Exception {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                throw new Exception("Attribute '" + string + "' not found.");
            }
            return Boolean.parseBoolean(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get long attribute '" + string + "'", (Throwable)exception);
            throw new Exception("Cannot get long attribute '" + string + "'", exception);
        }
    }

    public static byte getIntAttr(Element element, String string, byte by) {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                return by;
            }
            return Byte.parseByte(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return by;
        }
    }

    public static double getDoubleAttr(Element element, String string, double d) {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                return d;
            }
            return Double.parseDouble(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return d;
        }
    }

    public static boolean getBooleanAttr(Element element, String string, boolean bl) {
        try {
            String string2 = element.getAttributeValue(string);
            if (string2 == null) {
                return bl;
            }
            return Boolean.parseBoolean(string2);
        }
        catch (Exception exception) {
            Log.debug("Cannot get attribute '" + string + "'", (Throwable)exception);
            return bl;
        }
    }

    public static double tryGetDoubleAttr(Element element, String string) {
        try {
            return Double.parseDouble(element.getAttributeValue(string));
        }
        catch (Exception exception) {
            Log.error("Cannot get attribute '" + string + "'", (Throwable)exception);
            return -1.0;
        }
    }

    public static String tryGetAttr(Element element, String string, String string2) {
        String string3 = element.getAttributeValue(string);
        if (string3 == null) {
            element.setAttribute(string, string2);
            return string2;
        }
        return string3;
    }

    public static long tryGetLongAttr(Element element, String string) {
        try {
            return Long.parseLong(element.getAttributeValue(string));
        }
        catch (Exception exception) {
            Log.error("Cannot get attribute '" + string + "'", (Throwable)exception);
            return -1L;
        }
    }

    public static int tryGetIntAttr(Element element, String string) {
        String string2 = element.getAttributeValue(string);
        if (string2 == null) {
            Log.debug("Cannot get attribute '" + string + "', it doesn't exist");
            return -1;
        }
        if (string2.equals("undefined")) {
            Log.debug("Cannot get attribute '" + string + "', it is 'undefined'");
            return -1;
        }
        try {
            return Integer.parseInt(string2);
        }
        catch (Exception exception) {
            Log.error("Cannot get attribute '" + string + "'", (Throwable)exception);
            return -1;
        }
    }

    public static boolean tryGetBoolAttr(Element element, String string) {
        try {
            return Boolean.parseBoolean(element.getAttributeValue(string));
        }
        catch (Exception exception) {
            Log.error("Cannot get attribute '" + string + "'", (Throwable)exception);
            return false;
        }
    }

    public static byte tryGetByteAttr(Element element, String string) {
        try {
            return Byte.parseByte(element.getAttributeValue(string));
        }
        catch (Exception exception) {
            Log.error("Cannot get attribute '" + string + "'", (Throwable)exception);
            return -1;
        }
    }

    public static void trySetAttr(Element element, String string, Object object) {
        if (object != null) {
            element.setAttribute(string, String.valueOf(object));
        }
    }

    public static Document stringToXml(String string) throws JDOMException, IOException {
        SAXBuilder sAXBuilder = new SAXBuilder();
        sAXBuilder.setIgnoringElementContentWhitespace(true);
        sAXBuilder.setIgnoringBoundaryWhitespace(true);
        string = SQUtils.removeUTF8BOM(string);
        string = string.replaceAll(">\\s*<", "><");
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        Document document = sAXBuilder.build((InputStream)byteArrayInputStream);
        return document;
    }

    public static Element stringToXmlElement(String string) throws JDOMException, IOException {
        Document document = XMLUtil.stringToXml(string);
        return document.getRootElement();
    }

    public static Document fileToXml(File file) throws JDOMException, IOException, Exception {
        return XMLUtil.stringToXml(SQUtils.fileToString(file));
    }

    public static Element fileToXmlElement(File file) throws JDOMException, IOException, Exception {
        return XMLUtil.stringToXmlElement(SQUtils.fileToString(file));
    }

    public static void xmlToFile(Element element, File file) throws Exception {
        try {
            String string = XMLUtil.xmlToString(element);
            SQUtils.stringToFile(file, string);
        }
        catch (Exception exception) {
            throw new Exception("Cannot save xml to file '" + file.getAbsolutePath() + "'.", exception);
        }
    }

    public static void xmlToFile(Document document, File file) throws Exception {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), StandardCharsets.UTF_8));
            XMLOutputter xMLOutputter = new XMLOutputter();
            xMLOutputter.output(document, (Writer)bufferedWriter);
            bufferedWriter.close();
        }
        catch (Exception exception) {
            throw new Exception("Cannot save xml to file '" + file.getAbsolutePath() + "'.", exception);
        }
    }

    public static String xmlToString(Element element) {
        return XMLUtil.elementToString(element);
    }

    public static String xmlToStringRaw(Element element) {
        return rawOutputter.outputString(element);
    }

    public static byte getByte(Element element, String string, byte by) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return by;
        }
        return Byte.parseByte(element2.getValue());
    }

    public static int getInt(Element element, String string, int n) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return n;
        }
        return Integer.parseInt(element2.getValue());
    }

    public static long getLong(Element element, String string, int n) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return n;
        }
        return Long.parseLong(element2.getValue());
    }

    public static float getFloat(Element element, String string, float f) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return f;
        }
        return Float.parseFloat(element2.getValue());
    }

    public static double getDouble(Element element, String string, double d) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return d;
        }
        return Double.parseDouble(element2.getValue());
    }

    public static boolean getBoolean(Element element, String string, boolean bl) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            return bl;
        }
        return Boolean.parseBoolean(element2.getValue());
    }

    public static Element tryAddElement(Element element, String string) {
        try {
            return XMLUtil.getChildElem(element, string);
        }
        catch (Exception exception) {
            Element element2 = new Element(string);
            element.addContent((Content)element2);
            return element2;
        }
    }

    public static Element tryAddNode(Element element, String string, String string2) {
        try {
            return XMLUtil.getChildElem(element, string);
        }
        catch (Exception exception) {
            Element element2 = new Element(string).setText(string2);
            element.addContent((Content)element2);
            return element2;
        }
    }

    public static Element tryAddBooleanNode(Element element, String string, boolean bl) {
        try {
            Element element2 = XMLUtil.getChildElem(element, string);
            try {
                Boolean.parseBoolean(element2.getText());
            }
            catch (Exception exception) {
                Log.error("Node value must be boolean. " + element.getName() + "->" + string + "=" + element2.getText() + " fixed to " + bl);
                element2.setText(bl + "");
            }
            return element2;
        }
        catch (Exception exception) {
            Element element3 = new Element(string).setText(bl + "");
            element.addContent((Content)element3);
            return element3;
        }
    }

    public static Element tryAddIntNode(Element element, String string, int n) {
        try {
            Element element2 = XMLUtil.getChildElem(element, string);
            try {
                Integer.parseInt(element2.getText());
            }
            catch (Exception exception) {
                Log.error("Node value must be integer. " + element.getName() + "->" + string + "=" + element2.getText() + " fixed to " + n);
                element2.setText(n + "");
            }
            return element2;
        }
        catch (Exception exception) {
            Element element3 = new Element(string).setText(n + "");
            element.addContent((Content)element3);
            return element3;
        }
    }

    public static Element tryAddDoubleNode(Element element, String string, double d) {
        try {
            Element element2 = XMLUtil.getChildElem(element, string);
            try {
                Double.parseDouble(element2.getText());
            }
            catch (Exception exception) {
                Log.error("Node value must be double. " + element.getName() + "->" + string + "=" + element2.getText() + " fixed to " + d);
                element2.setText(d + "");
            }
            return element2;
        }
        catch (Exception exception) {
            Element element3 = new Element(string).setText(d + "");
            element.addContent((Content)element3);
            return element3;
        }
    }

    public static String getNodeValue(Element element, String string) throws Exception {
        Element element2 = XMLUtil.getChildElem(element, string);
        return element2.getText();
    }

    public static String getNodeValue(Element element, String string, String string2) {
        Element element2 = element.getChild(string);
        return element2 != null ? element2.getText() : string2;
    }

    public static int getNodeIntValue(Element element, String string) throws Exception {
        Element element2 = element.getChild(string);
        return Integer.parseInt(element2.getText());
    }

    public static int getNodeIntValue(Element element, String string, int n) throws Exception {
        try {
            Element element2 = element.getChild(string);
            return element2 != null ? Integer.parseInt(element2.getText()) : n;
        }
        catch (Exception exception) {
            Log.debug("Cannot get node value '" + string + "'", (Throwable)exception);
            return n;
        }
    }

    public static boolean getNodeBooleanValue(Element element, String string, boolean bl) throws Exception {
        try {
            Element element2 = element.getChild(string);
            return element2 != null ? Boolean.parseBoolean(element2.getText()) : bl;
        }
        catch (Exception exception) {
            Log.debug("Cannot get node value '" + string + "'", (Throwable)exception);
            return bl;
        }
    }

    public static Element getItemParameterNoException(Element element, String string) {
        List list = element.getChildren("Param");
        for (int i = 0; i < list.size(); ++i) {
            Element element2 = (Element)list.get(i);
            if (!element2.getAttributeValue("key").equals(string)) continue;
            return element2;
        }
        return null;
    }

    public static Element getItemParameter(Element element, String string, boolean bl) throws Exception {
        List list = element.getChildren("Param");
        for (int i = 0; i < list.size(); ++i) {
            Element element2 = (Element)list.get(i);
            if (!element2.getAttributeValue("key").equals(string)) continue;
            return element2;
        }
        if (bl) {
            throw new Exception(String.format("Parameter with key '%s'not found!", string));
        }
        return null;
    }

    public static Element getOptionParameter(Element element, String string) {
        for (Element element2 : element.getChildren("Parameter")) {
            if (!element2.getAttributeValue("key").equals(string)) continue;
            return element2;
        }
        return null;
    }

    public static void printXml(Element element) {
        System.out.println("----------------------------------------");
        System.out.println(XMLUtil.xmlToString(element));
        System.out.println("----------------------------------------");
    }

    public static Element copyElement(Element element) {
        Element element2 = new Element(element.getName());
        XMLUtil.copyAttributes(element, element2, true);
        return element2;
    }

    public static void copyAttributes(Element element, Element element2, boolean bl) {
        List list = element.getAttributes();
        for (int i = 0; i < list.size(); ++i) {
            Attribute attribute = (Attribute)list.get(i);
            if (!bl && element2.getAttributeValue(attribute.getName()) != null) continue;
            element2.setAttribute(attribute.getName(), attribute.getValue());
        }
    }

    public static void copySettings(Element element, Element element2, String string) throws Exception {
        Element element3;
        Element element4 = XMLUtil.findFirst(element, string);
        Element element5 = XMLUtil.findFirst(element2, string);
        if (element5 == null) {
            throw new Exception("Element '" + string + "' not found in template XML config");
        }
        if (element4 == null) {
            element3 = element;
            Log.info("Target element is null, adding contents to " + element.getName());
        } else {
            element3 = element4.getParentElement();
            element3.removeContent((Content)element4);
        }
        element3.addContent((Content)element5.clone());
    }

    public static boolean elementsEqual(Element element, Element element2) {
        if (!XMLUtil.attributesEqual(element, element2)) {
            return false;
        }
        List list = element.getChildren();
        List list2 = element2.getChildren();
        if (list.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (XMLUtil.elementsEqual((Element)list.get(i), (Element)list2.get(i))) continue;
            return false;
        }
        return true;
    }

    public static boolean attributesEqual(Element element, Element element2) {
        List list = element.getAttributes();
        List list2 = element2.getAttributes();
        if (list.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (XMLUtil.JSValuesEqual(((Attribute)list.get(i)).getValue(), ((Attribute)list2.get(i)).getValue())) continue;
            return false;
        }
        return true;
    }

    public static boolean JSValuesEqual(String string, String string2) {
        if (XMLUtil.JSValueIsUndefined(string) && XMLUtil.JSValueIsUndefined(string2)) {
            return true;
        }
        return string.equals(string2);
    }

    private static boolean JSValueIsUndefined(String string) {
        return string.equals("undefined") || string.equals("null");
    }

    public static Element readXmlFile(InputStream inputStream) throws Exception {
        SAXBuilder sAXBuilder = new SAXBuilder();
        sAXBuilder.setIgnoringElementContentWhitespace(true);
        sAXBuilder.setIgnoringBoundaryWhitespace(true);
        Document document = sAXBuilder.build(inputStream);
        return document.getRootElement();
    }

    public static int countElements(String string, Element element, int n) {
        List list;
        if (element.getName().equals(string)) {
            ++n;
        }
        if ((list = element.getChildren()) != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                Element element2 = (Element)list.get(i);
                n = XMLUtil.countElements(string, element2, n);
            }
        }
        return n;
    }

    public static Element findFirstParent(Element element, String string) {
        int n = 100;
        while (n > 0) {
            --n;
            if (element == null) {
                return null;
            }
            if (element.getName().equals(string)) {
                return element;
            }
            element = element.getParentElement();
        }
        return null;
    }

    public static void removeAttribute(Element element, String string) {
        element.removeAttribute(string);
        List list = element.getChildren();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); ++i) {
                Element element2 = (Element)list.get(i);
                element2.removeAttribute(string);
                XMLUtil.removeAttribute(element2, string);
            }
        }
    }

    public static int getItemIntParam(Element element, String string, int n) {
        try {
            Element element2 = XMLUtil.getItemParameter(element, string, false);
            if (element2 == null) {
                return n;
            }
            return Integer.parseInt(element2.getText());
        }
        catch (Exception exception) {
            Log.info("Error readign parameter {} from item. Exception: ", (Object)string, (Object)exception);
            return n;
        }
    }

    public static void setItemIntParam(Element element, String string, int n) {
        Element element2 = new Element("Param");
        element2.setAttribute("key", string);
        element2.setText(Integer.toString(n));
        element.addContent((Content)element2);
    }

    public static double getItemDoubleParam(Element element, String string, double d) {
        try {
            Element element2 = XMLUtil.getItemParameter(element, string, false);
            if (element2 == null) {
                return d;
            }
            return Double.parseDouble(element2.getText());
        }
        catch (Exception exception) {
            Log.info("Error readign parameter {} from item. Exception: ", (Object)string, (Object)exception);
            return d;
        }
    }

    public static void setItemDoubleParam(Element element, String string, double d) {
        Element element2 = new Element("Param");
        element2.setAttribute("key", string);
        element2.setText(Double.toString(d));
        element.addContent((Content)element2);
    }

    public static boolean getItemBoolParam(Element element, String string, boolean bl) {
        try {
            Element element2 = XMLUtil.getItemParameter(element, string, false);
            if (element2 == null) {
                return bl;
            }
            return Boolean.parseBoolean(element2.getText());
        }
        catch (Exception exception) {
            Log.info("Error readign parameter {} from item. Exception: ", (Object)string, (Object)exception);
            return bl;
        }
    }

    public static void setItemBoolParam(Element element, String string, boolean bl) {
        Element element2 = new Element("Param");
        element2.setAttribute("key", string);
        element2.setText(Boolean.toString(bl));
        element.addContent((Content)element2);
    }

    public static Element getSubchildElementEx(Element element, String string, String string2) throws Exception {
        String[] stringArray = string.split("/");
        for (int i = 0; i < stringArray.length; ++i) {
            String string3 = stringArray[i];
            if (string2 != null && i == stringArray.length - 1) {
                List list = element.getChildren(string3);
                if (list == null || list.size() == 0) {
                    throw new Exception(String.format("Leafs on path '%s' not found in XML, stopped at (%d) '%s'", string, i + 1, string3));
                }
                for (int j = 0; j < list.size(); ++j) {
                    Element element2 = (Element)list.get(j);
                    String string4 = element2.getAttributeValue("key");
                    if (string4 == null || !string4.equals(string2)) continue;
                    return element2;
                }
                continue;
            }
            if ((element = element.getChild(string3)) == null) {
                throw new Exception(String.format("Path '%s' not found in XML, stopped at (%d) '%s'", string, i + 1, string3));
            }
            if (i != stringArray.length - 1) continue;
            return element;
        }
        return null;
    }

    public static Element getSubchildElement(Element element, String string, String string2) throws Exception {
        try {
            return XMLUtil.getSubchildElementEx(element, string, string2);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public static boolean getBooleanFromElText(Element element, boolean bl) {
        if (element == null) {
            return bl;
        }
        String string = element.getText();
        if (string == null) {
            return bl;
        }
        return Boolean.parseBoolean(string);
    }

    public static String removeXMLCharacters(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    public static Element getOrCreateChild(Element element, String string) {
        Element element2 = element.getChild(string);
        if (element2 == null) {
            element2 = new Element(string);
            element.addContent((Content)element2);
        }
        return element2;
    }

    public static void replaceElementsWithAttribute(Element element, String string, String string2, Element element2, String string3) {
        if (string3 != null && element.getName().equals(string3)) {
            return;
        }
        List list = element.getChildren();
        for (int i = 0; i < list.size(); ++i) {
            Element element3 = (Element)list.get(i);
            String string4 = element3.getAttributeValue(string);
            if (string4 != null && string4.equals(string2)) {
                Element element4 = element2.clone();
                XMLUtil.replaceButKeepRandomGeneration(element3, element4);
                element.removeContent(i);
                element.addContent(i, (Content)element4);
                continue;
            }
            XMLUtil.replaceElementsWithAttribute(element3, string, string2, element2, string3);
        }
    }

    private static void replaceButKeepRandomGeneration(Element element, Element element2) {
        String string;
        List list = element.getChildren();
        List list2 = element2.getChildren();
        for (int i = 0; i < list2.size(); ++i) {
            Element element3 = (Element)list2.get(i);
            string = element3.getAttributeValue("key");
            if (string == null) continue;
            for (int j = 0; j < list.size(); ++j) {
                Element element4 = (Element)list.get(j);
                String string2 = element4.getAttributeValue("key");
                if (string2 == null || !string2.equals(string)) continue;
                String string3 = element4.getAttributeValue("generate");
                String string4 = element4.getAttributeValue("randomValue");
                if (string3 != null) {
                    element3.setAttribute("generate", string3);
                }
                if (string4 == null) continue;
                element3.setAttribute("randomValue", string4);
            }
        }
        String string5 = null;
        for (int i = 0; i < list.size(); ++i) {
            string = (Element)list.get(i);
            String string6 = string.getAttributeValue("key");
            if (string6 == null || !string6.equals("#Identification#")) continue;
            string5 = string;
        }
        if (string5 != null) {
            element2.addContent((Content)string5.clone());
        }
    }

    public static long parseDate(String string) {
        if (string == null || string.equals("") || string.equals("undefined")) {
            return 0L;
        }
        try {
            return SQTime.parseDateToMilis(string);
        }
        catch (ParseException parseException) {
            return 0L;
        }
    }

    public static boolean attributeValid(Element element, String string) {
        return element.getAttributeValue(string) != null && !element.getAttributeValue(string).equals("") && !element.getAttributeValue(string).equals("not set") && !element.getAttributeValue(string).equals("null") && !element.getAttributeValue(string).equals("undefined");
    }
}

