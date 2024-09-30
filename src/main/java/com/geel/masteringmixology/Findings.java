package com.geel.masteringmixology;

public class Findings {
    /**
     * Some dude gave me shit in the RL discord for this file, so I'm keeping it around forever now.
     *
     * VARBIT FINDINGS:
     *
     * PASTE COUNT
     * MOX: Varbit 11321
     * AGA: Varbit 11322
     * LYE: Varbit 11323
     *
     * POINT COUNT
     * MOX: 4416
     * AGA: 4415
     * LYE: 4414
     *
     *
     * CONTRACTS
     * 1st contract potion: 11315
     * 1st contract cook type: 11316?
     * 2nd contract potion: 11317
     * 2nd contract cook type: 11318?
     * 3rd contract potion: 11319
     * 3rd contract cook type: 11320?
     *
     * Cook types:
     * 0 - none
     * 1 - homogenize
     * 2 - concentrate
     * 3 - crystalize
     *
     * MIXERS
     * Vats west to east: 11324, 11325, 11326
     * Value is 1 for Mox, 2 for Aga, 3 for Lye
     *
     * Final vat: 11339
     * Value is 0 if no potion
     * - 1: MMM
     * - 2: MMA
     * - 3: MML
     * - 4: AAA
     * - 5: AAM
     * - 6: AAL
     * - 7: LLL
     * - 8: LLM
     * - 9: LLA
     * - 10: MLA
     *
     *
     * PROGRESS
     * Concentrate potion-being-contentrated: 11341
     * Concentrate progress: 11327 0->15
     *
     * Crystalize potion-being-crystalized: 11342
     * Crystalize progress: 11328 0->15
     *
     * Homogenize potion-being-homogenized: 11340
     * Homogenize progress: 11329 0->15
     *
     * HERBS:
     * 11332: Southwest herb?
     *
     *
     * --- OBJECTS ---
     *
     * Mixing vessel: 55395
     *
     * Retort: 55389
     * Agitator: 55390
     * Alembic: 55391
     *
     * Conveyor belt: 54917
     *
     * Aga Lever: 54867 (decoration, not object)
     * Mox Lever: 54868 (object)
     * Lye Lever: 54869 (object)
     *
     *
     * --- WIDGETS ---
     * (Note: don't think I need to do anything with widget parsing, a lot of varbits are exposed)
     * Overlay container: Id	57802754 | ParentId	57802753
     * CHILDREN:
     * - 0: "Potion Orders" label
     * - 1: Potion 1 Cook Type indicator (Type 5)
     * - 2: Potion 1 Name Label (Type 4)
     * - 3: Potion 2 Cook Type
     * - 4: Potion 2 Name Label
     * - 5: Potion 3 Cook Type
     * - 6: Potion 3 Name Label
     *
     * Cook type sprites:
     * Homogenize: 5674
     * Crystalize: 5673
     * Concentrate: ??
     */
}
