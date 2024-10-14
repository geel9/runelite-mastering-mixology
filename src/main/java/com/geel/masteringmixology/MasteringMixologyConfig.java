package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyBuilding;
import com.geel.masteringmixology.enums.PriorityMethod;
import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(MasteringMixologyConfig.GROUP)
public interface MasteringMixologyConfig extends Config {
    String GROUP = "masteringmixology";
//
//    @ConfigItem(
//            position = 0,
//            keyName = "prioritization",
//            name = "Prioritization",
//            description = "How to prioritize tasks: none, by point ratio, or by a specific building type"
//    )
//    default PriorityMethod prioritization() {
//        return PriorityMethod.NONE;
//    }
//
//    @ConfigItem(
//            position = 1,
//            keyName = "moxGoal",
//            name = "Mox Goal",
//            description = "Goal mox points"
//    )
//    default int moxGoal() {
//        return 0;
//    }
//
//    @ConfigItem(
//            position = 2,
//            keyName = "agaGoal",
//            name = "Aga Goal",
//            description = "Goal aga points"
//    )
//    default int agaoGoal() {
//        return 0;
//    }
//
//    @ConfigItem(
//            position = 3,
//            keyName = "lyeGoal",
//            name = "Lye Goal",
//            description = "Goal lye points"
//    )
//    default int lyeGoal() {
//        return 0;
//    }
}
