/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import com.strategyquant.lib.sqxbusiness.EAOption;
import java.io.Serializable;
import java.util.ArrayList;
import org.jdom2.Element;

public class MQLMarketBuildSettings
implements Serializable {
    public String projectName;
    public Element strategyXML;
    public String outputName;
    public String platform;
    public String copyright;
    public String link;
    public String version;
    public String comment;
    public String description;
    public boolean demoOnly;
    public double fixedSize;
    public String expirationDate;
    public int account;
    public ArrayList<EAOption> eaOptions;

    public MQLMarketBuildSettings clone() {
        MQLMarketBuildSettings mQLMarketBuildSettings = new MQLMarketBuildSettings();
        mQLMarketBuildSettings.account = this.account;
        mQLMarketBuildSettings.demoOnly = this.demoOnly;
        mQLMarketBuildSettings.expirationDate = this.expirationDate;
        mQLMarketBuildSettings.fixedSize = this.fixedSize;
        mQLMarketBuildSettings.outputName = this.outputName;
        mQLMarketBuildSettings.platform = this.platform;
        mQLMarketBuildSettings.projectName = this.projectName;
        mQLMarketBuildSettings.strategyXML = this.strategyXML != null ? this.strategyXML.clone() : null;
        mQLMarketBuildSettings.copyright = this.copyright;
        mQLMarketBuildSettings.link = this.link;
        mQLMarketBuildSettings.version = this.version;
        mQLMarketBuildSettings.comment = this.comment;
        mQLMarketBuildSettings.description = this.description;
        mQLMarketBuildSettings.eaOptions = new ArrayList();
        for (int i = 0; i < this.eaOptions.size(); ++i) {
            mQLMarketBuildSettings.eaOptions.add(this.eaOptions.get(i).clone());
        }
        return mQLMarketBuildSettings;
    }
}

