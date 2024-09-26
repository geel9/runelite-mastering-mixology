package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyBuilding;
import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(MasteringMixologyConfig.GROUP)
public interface MasteringMixologyConfig extends Config {
    String GROUP = "masteringmixology";


    @ConfigItem(
            position = 0,
            keyName = "prioritisedBuilding",
            name = "Prioritised Building",
            description = "Which building you most prefer to use"
    )
    default AlchemyBuilding prioritisedBuilding() {
        return AlchemyBuilding.RETORT_CONCENTRATOR;
    }
}
