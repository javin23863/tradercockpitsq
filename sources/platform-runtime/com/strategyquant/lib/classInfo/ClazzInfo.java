/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.classInfo;

import java.util.LinkedList;
import java.util.List;

public class ClazzInfo {
    private String name;
    private String fullName;
    private List<ParamInfo> fields;
    private List<ParamInfo> staticFields;
    private List<MethodInfo> methods;
    private List<MethodInfo> staticMethods;

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public List<MethodInfo> getMethods() {
        return this.methods;
    }

    public void setMethods(List<MethodInfo> list) {
        this.methods = list;
    }

    public List<MethodInfo> getStaticMethods() {
        return this.staticMethods;
    }

    public void setStaticMethods(List<MethodInfo> list) {
        this.staticMethods = list;
    }

    public List<ParamInfo> getFields() {
        return this.fields;
    }

    public void setFields(List<ParamInfo> list) {
        this.fields = list;
    }

    public List<ParamInfo> getStaticFields() {
        return this.staticFields;
    }

    public void setStaticFields(List<ParamInfo> list) {
        this.staticFields = list;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String string) {
        this.fullName = string;
    }

    public static class ParamInfo {
        private String name;
        private String type;

        public String getName() {
            return this.name;
        }

        public void setName(String string) {
            this.name = string;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String string) {
            this.type = string;
        }
    }

    public static class MethodInfo {
        private String name;
        private String resultType;
        private List<List<ParamInfo>> params = new LinkedList<List<ParamInfo>>();

        public MethodInfo(String string) {
            this.name = string;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String string) {
            this.name = string;
        }

        public List<List<ParamInfo>> getParams() {
            return this.params;
        }

        public void setParams(List<List<ParamInfo>> list) {
            this.params = list;
        }

        public String getResultType() {
            return this.resultType;
        }

        public void setResultType(String string) {
            this.resultType = string;
        }
    }
}

