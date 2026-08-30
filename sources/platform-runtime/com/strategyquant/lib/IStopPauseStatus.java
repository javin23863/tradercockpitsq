/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

public interface IStopPauseStatus {
    public boolean isStopped();

    public boolean isStoppedNotFinished();

    public boolean isPaused();

    public boolean isFinished();

    public boolean checkRunning();

    public void checkPaused();
}

