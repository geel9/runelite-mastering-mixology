package com.geel.masteringmixology;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(MasteringMixologyConfig.GROUP)
public interface MasteringMixologyConfig extends Config {
    String GROUP = "masteringmixology";


    @ConfigItem(
            position = 0,
            keyName = "moxRatio",
            name = "Mox Ratio",
            description = "Mox Ratio"
    )
    default int moxRatio() {
        return 1;
    }

    @ConfigItem(
            position = 1,
            keyName = "agaRatio",
            name = "Aga Ratio",
            description = "Aga Ratio"
    )
    default int agaRatio() {
        return 1;
    }

    @ConfigItem(
            position = 2,
            keyName = "lyeRatio",
            name = "Lye Ratio",
            description = "Lye Ratio"
    )
    default int lyeRatio() {
        return 1;
    }

}
