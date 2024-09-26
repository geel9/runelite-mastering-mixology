package com.geel.masteringmixology.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Contract {
    @Getter
    private PastePotion potion;

    @Getter
    private PotionType type;
}
