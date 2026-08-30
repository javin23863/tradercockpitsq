/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils.mousehook;

import java.awt.Point;

public interface IMouseHookListener {
    public void onGlobalMousePress(Point var1);

    public void onComponentMousePress(Point var1);

    public void onGlobalMouseRelease(Point var1);

    public void onComponentMouseRelease(Point var1);

    public void onGlobalMouseDrag(Point var1, int var2, int var3);

    public void onComponentMouseDrag(Point var1, int var2, int var3);

    public void onGlobalMouseMove(Point var1);

    public void onComponentMouseMove(Point var1);
}

