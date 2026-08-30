/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.volumeProfile;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jdom2.Element;

public class VolumeProfileBlocks {
    private static final Set<String> BLOCKS = new HashSet<String>(Arrays.asList("TPOProfile", "VolumeProfile", "VolumeProfileCustomHours", "VolumeProfileCustomHoursMultiSession", "VPFallingPOC", "VPRisingPOC", "VPValueShiftDown", "VPValueShiftUp"));

    public static boolean contains(String string) {
        return BLOCKS.contains(string);
    }

    public static Set<String> getBlocks() {
        return Collections.unmodifiableSet(BLOCKS);
    }

    public static void checkStrategyAllowed(Element element, String string) throws Exception {
        if (element == null || VolumeProfileSubscription.getInstance().isActive()) {
            return;
        }
        String string2 = XMLUtil.elementToString(element);
        for (String string3 : BLOCKS) {
            if (!string2.contains("key=\"" + string3 + "\"")) continue;
            String string4 = string != null ? string : "??????";
            throw new Exception(L.t("Your strategy '%s' contains Volume & Market Profile blocks, but you don't have this addon enabled.", string4));
        }
    }
}

