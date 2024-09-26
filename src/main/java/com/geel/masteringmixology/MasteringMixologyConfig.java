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
        return AlchemyBuilding.NONE;
    }

    @ConfigItem(
            position = 1,
            keyName = "inventoryOverlay",
            name = "Herbs In Inventory",
            description = "Shows a small colored box in the bottom right of the items in your inventory to display the paste it can be turned in to."
    )
    default boolean inventoryOverlay() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "bankOverlay",
            name = "Herbs In Bank",
            description = "Shows a small colored box  in the bottom right of the items in your bank to display the paste it can be turned in to."
    )
    default boolean bankOverlay() {
        return true;
    }
}
