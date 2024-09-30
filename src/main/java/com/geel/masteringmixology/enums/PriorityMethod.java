package com.geel.masteringmixology.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PriorityMethod {
    NONE("None"),
    POINTS("Optimal Points Ratio"),
    BUILDING_AGITATOR("Agitator (Homogenize)"),
    BUILDING_ALEMBIC("Alembic (Crystallize)"),
    BUILDING_RETORT("Retort (Concentrate)");

    @Getter
    private String name;

    public AlchemyBuilding GetBuilding() {
        switch (this) {
            case BUILDING_AGITATOR:
                return AlchemyBuilding.AGITATOR_HOMOGENIZER;
            case BUILDING_ALEMBIC:
                return AlchemyBuilding.ALEMBIC_CRYSTALIZER;
            case BUILDING_RETORT:
                return AlchemyBuilding.RETORT_CONCENTRATOR;
            case NONE:
            case POINTS:
            default:
                return AlchemyBuilding.NONE;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
