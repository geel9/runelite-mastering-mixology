package com.geel.masteringmixology.enums;

import java.awt.*;

import java.util.Arrays;

import lombok.AllArgsConstructor;

import lombok.Getter;

import static net.runelite.api.ItemID.*;

@AllArgsConstructor
public enum HerbPastes {
    Mox("Mox Paste", new int[]{GUAM_LEAF, GUAM_POTION_UNF, MARRENTILL, MARRENTILL_POTION_UNF, TARROMIN, TARROMIN_POTION_UNF, HARRALANDER, HARRALANDER_POTION_UNF}, Color.decode("#584c87")),
    Lye("Lye Paste", new int[]{RANARR_WEED, RANARR_POTION_UNF, TOADFLAX, TOADFLAX_POTION_UNF, AVANTOE, AVANTOE_POTION_UNF, KWUARM, KWUARM_POTION_UNF, SNAPDRAGON, SNAPDRAGON_POTION_UNF}, Color.PINK.darker()),
    Aga("Aga Paste", new int[]{IRIT_LEAF, IRIT_POTION_UNF, HUASCA, HUASCA_POTION_UNF, CADANTINE, CADANTINE_POTION_UNF, DWARF_WEED, DWARF_WEED_POTION_UNF, TORSTOL, TORSTOL_POTION_UNF}, Color.GREEN.darker());


    String name;
    int[] herbIds;
    @Getter
    Color color;

    public boolean CanItemBeUsedAsPaste(int itemId) {
        return Arrays.stream(herbIds).anyMatch(x -> x == itemId);
    }
}
