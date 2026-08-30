/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import java.io.Serializable;

public class MQLMarketBuildResult
implements Serializable {
    public String projectName;
    public Exception exception;

    public MQLMarketBuildResult(String string, Exception exception) {
        this.projectName = string;
        this.exception = exception;
    }
}

