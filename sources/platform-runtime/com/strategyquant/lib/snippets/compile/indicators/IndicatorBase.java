/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.indicators;

import com.strategyquant.lib.snippets.compile.indicators.IndicatorParam;
import java.util.ArrayList;
import java.util.List;

public class IndicatorBase {
    private String name;
    private final List<IndicatorParam> params = new ArrayList<IndicatorParam>();
    private final List<String> imports = new ArrayList<String>();
    private final List<String> outputs = new ArrayList<String>();

    public void setName(String string) {
        this.name = string;
    }

    public String getName() {
        return this.name;
    }

    public List<IndicatorParam> getParamList() {
        return this.params;
    }

    public List<String> getImportsList() {
        return this.imports;
    }

    public List<String> getOutputsList() {
        return this.outputs;
    }
}

