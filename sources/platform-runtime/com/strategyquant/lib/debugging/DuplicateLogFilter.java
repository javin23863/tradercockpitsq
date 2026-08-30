/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.debugging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

public class DuplicateLogFilter
extends Filter<ILoggingEvent> {
    private static final int MAX_KEY_LENGTH = 100;
    private static final int DEFAULT_CACHE_SIZE = 100;
    private static final int DEFAULT_ALLOWED_REPETITIONS = 10;
    private static final int DEFAULT_EXPIRE_AFTER_WRITE_SECONDS = 300;
    private int allowedRepetitions = 10;
    private int cacheSize = 100;
    private int expireAfterWriteSeconds = 300;
    private Cache<String, Boolean> msgTimeCache;
    private Cache<String, Integer> msgWholeCache;

    public void start() {
        this.msgTimeCache = this.buildTimeCache();
        this.msgWholeCache = this.buildWholeCache();
        super.start();
    }

    public void stop() {
        this.msgTimeCache.invalidateAll();
        this.msgTimeCache = null;
        this.msgWholeCache.invalidateAll();
        this.msgWholeCache = null;
        super.stop();
    }

    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        Boolean bl = false;
        Integer n = 0;
        String string = iLoggingEvent.getMessage();
        if (StringUtils.isNotBlank((CharSequence)string)) {
            String string2 = StringUtils.abbreviate((String)string, (int)100);
            bl = (Boolean)this.msgTimeCache.getIfPresent((Object)string2);
            this.msgTimeCache.put((Object)string2, (Object)true);
            if (bl != null) {
                return FilterReply.DENY;
            }
            Integer n2 = (Integer)this.msgWholeCache.getIfPresent((Object)string2);
            if (n2 != null) {
                n = n2 + 1;
            }
            this.msgWholeCache.put((Object)string2, (Object)n);
            if (n >= this.allowedRepetitions) {
                return FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
    }

    public int getAllowedRepetitions() {
        return this.allowedRepetitions;
    }

    public void setAllowedRepetitions(int n) {
        this.allowedRepetitions = n;
    }

    public int getCacheSize() {
        return this.cacheSize;
    }

    public void setCacheSize(int n) {
        this.cacheSize = n;
    }

    public int getExpireAfterWriteSeconds() {
        return this.expireAfterWriteSeconds;
    }

    public void setExpireAfterWriteSeconds(int n) {
        this.expireAfterWriteSeconds = n;
    }

    private Cache<String, Boolean> buildTimeCache() {
        return Caffeine.newBuilder().expireAfterWrite((long)this.expireAfterWriteSeconds, TimeUnit.SECONDS).initialCapacity(this.cacheSize).maximumSize((long)this.cacheSize).build();
    }

    private Cache<String, Integer> buildWholeCache() {
        return Caffeine.newBuilder().initialCapacity(this.cacheSize).maximumSize((long)this.cacheSize).build();
    }
}

