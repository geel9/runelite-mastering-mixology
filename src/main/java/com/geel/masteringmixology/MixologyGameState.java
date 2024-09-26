package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.Contract;
import com.geel.masteringmixology.enums.PastePotion;
import com.geel.masteringmixology.enums.PotionType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class MixologyGameState {
    @Inject
    private Client client;

    @Getter
    private int hopperMox = -1;

    @Getter
    private int hopperAga = -1;

    @Getter
    private int hopperLye = -1;

    @Getter
    private int pointsMox = -1;

    @Getter
    private int pointsAga = -1;

    @Getter
    private int pointsLye = -1;

    @Getter
    private int crystalizerProgress = -1;

    @Getter
    private int homogenizerProgress = -1;

    @Getter
    private int concentratorProgress = -1;

    private PotionType lastUsedBuilding = PotionType.NONE;
    private int lastUsedBuildingTick = 0;

    public Contract[] getContracts() {
        var firstContract = getContractFromVarbitIDs(Constants.VB_CONTRACT_1_POTION, Constants.VB_CONTRACT_1_TYPE);
        var secondContract = getContractFromVarbitIDs(Constants.VB_CONTRACT_2_POTION, Constants.VB_CONTRACT_2_TYPE);
        var thirdContract = getContractFromVarbitIDs(Constants.VB_CONTRACT_3_POTION, Constants.VB_CONTRACT_3_TYPE);

        if (firstContract == null || secondContract == null || thirdContract == null) {
            return new Contract[0];
        }

        // My allocations!! My heap!! Look what you've _done_ to it!
        return new Contract[]{
                firstContract,
                secondContract,
                thirdContract
        };
    }

    public Contract getBestContract(Contract[] contracts) {
        if(contracts == null || contracts.length == 0) {
            return null;
        }

        // For now, just return first contract we can do baybee
        for(var contract : contracts) {
            if(canMakePotion(contract.getPotion())) {
                return contract;
            }
        }

        return null;
    }

    public PotionType getCurrentlyUsingBuilding() {
        if (lastUsedBuilding == PotionType.NONE || lastUsedBuildingTick <= 0) {
            return PotionType.NONE;
        }

        if (lastUsedBuildingTick > client.getTickCount() || (client.getTickCount() - lastUsedBuildingTick) >= 2) {
            return PotionType.NONE;
        }

        if (!client.getLocalPlayer().getWorldLocation().equals(lastUsedBuilding.getInteractionLocation())) {
            return PotionType.NONE;
        }

        return lastUsedBuilding;
    }

    /**
     * @return True if the player is currently located within the minigame area
     */
    public boolean isInArea() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }

        Player local = client.getLocalPlayer();
        if (local == null) {
            return false;
        }

        WorldPoint location = local.getWorldLocation();
        if (location.getPlane() != 0) {
            return false;
        }

        int x = location.getX();
        int y = location.getY();

        return x >= 1384 && x <= 1404 && y >= 9319 && y <= 9334;
    }

    public boolean canMakePotion(PastePotion potion) {
        if (client.getBoostedSkillLevel(Skill.HERBLORE) < potion.getHerbloreLevel()) {
            return false;
        }

        if (potion.getMoxRequired() > 0 && hopperMox < (potion.getMoxRequired() * 10)) {
            return false;
        }
        if (potion.getAgaRequired() > 0 && hopperAga < (potion.getAgaRequired() * 10)) {
            return false;
        }
        if (potion.getLyeRequired() > 0 && hopperLye < (potion.getLyeRequired() * 10)) {
            return false;
        }

        return true;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        var varbitId = event.getVarbitId();
        var varbitValue = event.getValue();

        // Java isn't letting me do a switch statement on these constants????
        // This fucking language man
        if (varbitId == Constants.VB_PASTE_COUNT_MOX) {
            hopperMox = varbitValue;
            return;
        }
        if (varbitId == Constants.VB_PASTE_COUNT_AGA) {
            hopperAga = varbitValue;
            return;
        }
        if (varbitId == Constants.VB_PASTE_COUNT_LYE) {
            hopperLye = varbitValue;
            return;
        }

        if (varbitId == Constants.VB_POINTS_COUNT_MOX) {
            pointsMox = varbitValue;
            return;
        }
        if (varbitId == Constants.VB_POINTS_COUNT_AGA) {
            pointsAga = varbitValue;
            return;
        }
        if (varbitId == Constants.VB_POINTS_COUNT_LYE) {
            pointsLye = varbitValue;
            return;
        }

        var buildingBeingUsed = PotionType.FromProgressVarbitId(varbitId);
        if (buildingBeingUsed != PotionType.NONE) {
            handleBuildingUsed(buildingBeingUsed, varbitValue);
        }
    }

    private Contract getContractFromVarbitIDs(int potionVarbitId, int cookTypeVarbitId) {
        var potion = client.getVarbitValue(potionVarbitId);
        var potionType = client.getVarbitValue(cookTypeVarbitId);

        if (potion <= 0 || potionType <= 0) {
            return null;
        }

        return new Contract(PastePotion.FromId(potion), PotionType.FromId(potionType));
    }

    private void handleBuildingUsed(PotionType building, int progress) {
        var player = client.getLocalPlayer();
        var interacting = player.getInteracting();

        if (building == PotionType.HOMOGENIZED) {
            homogenizerProgress = progress;
        }
        if (building == PotionType.CONCENTRATRED) {
            concentratorProgress = progress;
        }
        if (building == PotionType.CRYSTALIZED) {
            crystalizerProgress = progress;
        }

        if (progress != 0) {
            lastUsedBuilding = building;
            lastUsedBuildingTick = client.getTickCount();
        }
    }
}
