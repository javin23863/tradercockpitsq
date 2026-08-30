/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.L;
import com.strategyquant.lib.sourcecode.ConditionsFixer;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ConditionsFixerDirective
implements TemplateDirectiveModel {
    public ArrayList<String> variables;
    public String condition = "";

    public void execute(Environment environment, Map map, TemplateModel[] templateModelArray, TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException {
        if (!map.isEmpty()) {
            throw new TemplateModelException(L.t("This directive doesn't allow parameters.", new Object[0]));
        }
        if (templateModelArray.length != 0) {
            throw new TemplateModelException(L.t("This directive doesn't allow loop variables.", new Object[0]));
        }
        if (templateDirectiveBody == null) {
            throw new RuntimeException(L.t("missing body", new Object[0]));
        }
        ConditionsFixer conditionsFixer = new ConditionsFixer();
        conditionsFixer.process(templateDirectiveBody, environment.getOut());
    }
}

