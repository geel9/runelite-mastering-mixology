package com.geel.masteringmixology.enums;

import com.geel.masteringmixology.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.NullObjectID;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
public enum DigweedSpawn {
    NONE(0, 0, new WorldPoint(0, 0, 0)),
    NORTH_EAST(NullObjectID.NULL_55396, Constants.VB_DIGWEED_NORTH_EAST, new WorldPoint(1399, 9331, 0)),
    SOUTH_EAST(NullObjectID.NULL_55397, Constants.VB_DIGWEED_SOUTH_EAST, new WorldPoint(1399, 9322, 0)),
    SOUTH_WEST(NullObjectID.NULL_55398, Constants.VB_DIGWEED_SOUTH_WEST, new WorldPoint(1389, 9322, 0)),
    NORTH_WEST(NullObjectID.NULL_55399, Constants.VB_DIGWEED_NORTH_WEST, new WorldPoint(1389, 9331, 0));

    @Getter
    private int objectID;

    @Getter
    private int varbitId;

    @Getter
    private WorldPoint spawnPoint;
}
