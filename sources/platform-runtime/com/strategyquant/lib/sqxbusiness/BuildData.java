/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class BuildData {
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    public static final int STATUS_WAITING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_STOPPED = 3;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_ERROR = 5;
    public String jobGroupID;
    public String jobID;
    public String project;
    public String outputName;
    public boolean isMT5;
    public int status;
    public boolean changed;
    private String[] logLines = new String[100];
    private int logLineIndex = 0;

    public BuildData(String string, String string2, String string3, String string4, boolean bl) {
        this.project = string;
        this.jobGroupID = string2;
        this.jobID = string3;
        this.outputName = string4;
        this.isMT5 = bl;
        this.status = 0;
        this.addLogLine("Waiting for execution...");
        this.changed = true;
    }

    public void addLogLine(String string) {
        string = String.format("[%s] %s", format.format(new Date()), string);
        int n = this.logLines.length - 1;
        if (this.logLineIndex < n) {
            this.logLines[this.logLineIndex] = string;
            ++this.logLineIndex;
        } else {
            for (int i = 0; i < n; ++i) {
                this.logLines[i] = this.logLines[i + 1];
            }
            this.logLines[n] = string;
            this.logLineIndex = n;
        }
        this.changed = true;
    }

    public JSONObject toJSON() {
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < this.logLineIndex; ++i) {
            jSONArray.put((Object)this.logLines[i]);
        }
        return new JSONObject().put("jobGroupID", (Object)this.jobGroupID).put("jobID", (Object)this.jobID).put("project", (Object)this.project).put("outputName", (Object)this.outputName).put("isMT5", this.isMT5).put("status", this.status).put("logs", (Object)jSONArray).put("outputPath", (Object)(MQLMarketConst.getProjectDirPath(this.project) + "/" + "output" + "/" + this.outputName + "." + (this.isMT5 ? "ex5" : "ex4")));
    }

    public void clearLog() {
        this.logLineIndex = 0;
        this.changed = true;
    }
}

