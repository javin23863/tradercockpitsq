/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.classInfo;

import com.strategyquant.lib.classInfo.ClazzInfo;
import com.strategyquant.lib.utils.JsonCreator;
import java.util.List;
import java.util.Set;

public class CodeInfo {
    private Set<String> imports;
    private List<ClazzInfo> classes;

    public Set<String> getImports() {
        return this.imports;
    }

    public void setImports(Set<String> set) {
        this.imports = set;
    }

    public List<ClazzInfo> getClasses() {
        return this.classes;
    }

    public void setClasses(List<ClazzInfo> list) {
        this.classes = list;
    }

    public String toJSON() {
        JsonCreator jsonCreator = new JsonCreator();
        jsonCreator.beginObject();
        jsonCreator.put("imports");
        jsonCreator.beginArray();
        int n = 0;
        for (String object : this.imports) {
            jsonCreator.setValue(object, n < this.imports.size() - 1);
            ++n;
        }
        jsonCreator.endArray(true);
        jsonCreator.put("classes");
        jsonCreator.beginArray();
        n = 0;
        for (ClazzInfo clazzInfo : this.classes) {
            jsonCreator.beginObject();
            jsonCreator.put("name", clazzInfo.getName(), true);
            jsonCreator.put("fullName", clazzInfo.getFullName(), true);
            this.addMethods(jsonCreator, "methods", clazzInfo.getMethods(), true);
            this.addMethods(jsonCreator, "staticMethods", clazzInfo.getStaticMethods(), true);
            jsonCreator.put("fields");
            jsonCreator.beginObject();
            this.addProperties(jsonCreator, clazzInfo.getFields());
            jsonCreator.endObject(true);
            jsonCreator.put("staticFields");
            jsonCreator.beginObject();
            this.addProperties(jsonCreator, clazzInfo.getStaticFields());
            jsonCreator.endObject(false);
            jsonCreator.endObject(n < this.classes.size() - 1);
            ++n;
        }
        jsonCreator.endArray(false);
        jsonCreator.endObject(false);
        return jsonCreator.toJson();
    }

    private void addProperties(JsonCreator jsonCreator, List<ClazzInfo.ParamInfo> list) {
        int n = 0;
        for (ClazzInfo.ParamInfo paramInfo : list) {
            jsonCreator.put(paramInfo.getName(), paramInfo.getType(), n < list.size() - 1);
            ++n;
        }
    }

    private void addMethods(JsonCreator jsonCreator, String string, List<ClazzInfo.MethodInfo> list, boolean bl) {
        jsonCreator.put(string);
        jsonCreator.beginArray();
        int n = 0;
        for (ClazzInfo.MethodInfo methodInfo : list) {
            jsonCreator.beginObject();
            jsonCreator.put("name", methodInfo.getName(), true);
            jsonCreator.put("returnType", methodInfo.getResultType(), true);
            jsonCreator.put("vars");
            jsonCreator.beginArray();
            int n2 = 0;
            for (List<ClazzInfo.ParamInfo> list2 : methodInfo.getParams()) {
                jsonCreator.beginObject();
                this.addProperties(jsonCreator, list2);
                jsonCreator.endObject(n2 < methodInfo.getParams().size() - 1);
                ++n2;
            }
            jsonCreator.endArray(false);
            jsonCreator.endObject(n < list.size() - 1);
            ++n;
        }
        jsonCreator.endArray(bl);
    }
}

