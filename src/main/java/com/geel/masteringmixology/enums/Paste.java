package com.geel.masteringmixology.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

@AllArgsConstructor
public enum Paste {
    NONE(0, ""),
    MOX(ItemID.MOX_PASTE, "Mox"),
    AGA(ItemID.AGA_PASTE, "Aga"),
    LYE(ItemID.LYE_PASTE, "Lye");

    @Getter
    private final int ItemId;

    @Getter
    private final String Name;
}
