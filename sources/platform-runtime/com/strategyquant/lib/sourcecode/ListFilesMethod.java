/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import freemarker.template.SimpleList;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.File;
import java.util.List;

public class ListFilesMethod
implements TemplateMethodModel {
    public TemplateModel exec(List list) throws TemplateModelException {
        if (list.size() != 1) {
            throw new TemplateModelException("Wrong arguments");
        }
        String string = (String)list.get(0);
        File file = new File(string);
        SimpleList simpleList = new SimpleList();
        File[] fileArray = file.listFiles();
        if (fileArray != null) {
            for (File file2 : fileArray) {
                if (!file2.isFile()) continue;
                simpleList.add((Object)file2.getName());
            }
        }
        return simpleList;
    }
}

