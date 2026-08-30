/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

public interface IProgressListener {
    public void onStart();

    public void onProgress(double var1);

    public void onPause();

    public void onContinue();

    public void onFinish();

    public void onError(String var1);

    public void onConfirm(String var1);

    public void setMessage(String var1);

    public void setStep(int var1);
}

