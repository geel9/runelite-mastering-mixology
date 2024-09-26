package com.geel.masteringmixology.enums;

import com.geel.masteringmixology.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
public enum PotionType {
    CRYSTALIZED(3, 55391, new WorldPoint(1392,9325,0)), // Alembic
    CONCENTRATRED(2, 55389, new WorldPoint(1396,9326,0)), // Retort
    HOMOGENIZED(1, 55390, new WorldPoint(1395,9328,0)), // Agitator
    NONE(0, 0, new WorldPoint(0,0,0));

    @Getter
    private int id;

    @Getter
    private int buildingId;

    @Getter
    private WorldPoint interactionLocation;

    public static PotionType FromId(int id) {
        switch (id) {
            case 3:
                return CRYSTALIZED;
            case 2:
                return CONCENTRATRED;
            case 1:
                return HOMOGENIZED;
            case 0:
                return NONE;
            default:
                return null;
        }
    }

    public static PotionType FromProgressVarbitId(int varbitId)
    {
        if(varbitId == Constants.VB_ALEMBIC_PROGRESS) {
            return PotionType.CRYSTALIZED;
        }

        if(varbitId == Constants.VB_RETORT_PROGRESS) {
            return PotionType.CONCENTRATRED;
        }

        if(varbitId == Constants.VB_AGITATOR_PROGRESS) {
            return PotionType.HOMOGENIZED;
        }

        return PotionType.NONE;
    }
}
