/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import freemarker.core.Environment;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;

public class CheckMacroExistsMethod
implements TemplateMethodModel {
    public TemplateModel exec(List list) throws TemplateModelException {
        if (list.size() != 1) {
            throw new TemplateModelException("Wrong arguments");
        }
        Environment environment = Environment.getCurrentEnvironment();
        if (environment.getMainNamespace().get((String)list.get(0)) != null) {
            return new SimpleNumber(1);
        }
        throw new TemplateModelException("Missing macro for '" + (String)list.get(0) + "'. You have to add this macro to building blocks include.");
    }
}

