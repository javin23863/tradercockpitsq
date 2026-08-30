/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm.banners;

import org.json.JSONObject;

public class Banner {
    public int id;
    public String name;
    public String type;
    public String image;
    public String url;
    public int weight;

    public JSONObject toJSON() {
        return new JSONObject().put("id", this.id).put("name", (Object)this.name).put("name", (Object)this.name).put("type", (Object)this.type).put("image", (Object)("../QDM/data/" + this.image)).put("url", (Object)this.url);
    }
}

