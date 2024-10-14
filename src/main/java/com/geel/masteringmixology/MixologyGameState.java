package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@Singleton
public class MixologyGameState {
    @Inject
    private Client client;

    @Inject
    private MasteringMixologyConfig config;

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

    @Getter
    private Map<AlchemyPaste, TileObject> leverObjects;

    @Getter
    private Map<AlchemyBuilding, TileObject> buildingObjects;

    @Getter
    private Set<TileObject> conveyorObjects = new HashSet<>();

    @Getter
    private TileObject mixerObject;

    @Getter
    private TileObject hopperObject;

    @Getter
    private int lastAgitatorSoundEffectPlayedTick = 0;

    private AlchemyBuilding lastUsedBuilding = AlchemyBuilding.NONE;
    private int lastUsedBuildingTick = 0;

    private final AlchemyContract[] rawContracts = new AlchemyContract[]
            {
                    new AlchemyContract(AlchemyPotion.NONE, AlchemyBuilding.NONE, ContractState.NONE),
                    new AlchemyContract(AlchemyPotion.NONE, AlchemyBuilding.NONE, ContractState.NONE),
                    new AlchemyContract(AlchemyPotion.NONE, AlchemyBuilding.NONE, ContractState.NONE)
            };

    private AlchemyContract[] effectiveContracts = new AlchemyContract[0];

    public void start() {
        leverObjects = new HashMap<>();
        buildingObjects = new HashMap<>();
        conveyorObjects = new HashSet<>();
        mixerObject = null;
        hopperObject = null;
        lastAgitatorSoundEffectPlayedTick = 0;
        refreshContractsFromVarbits();
        scanForObjects();
    }

    public void stop() {
        leverObjects.clear();
        buildingObjects.clear();
        conveyorObjects.clear();
        mixerObject = null;
        hopperObject = null;
        clearContracts();
    }

    public Map<AlchemyPaste, Integer> getCurrentMix() {
        var ret = new HashMap<AlchemyPaste, Integer>();

        var westVat = client.getVarbitValue(Constants.VB_VAT_WEST);
        var midVat = client.getVarbitValue(Constants.VB_VAT_MID);
        var eastVat = client.getVarbitValue(Constants.VB_VAT_EAST);

        var vats = new int[]{westVat, midVat, eastVat};

        for (var vat : vats) {
            if (vat == 0) {
                continue;
            }

            AlchemyPaste paste = AlchemyPaste.FromId(vat);
            if (!ret.containsKey(paste)) {
                ret.put(paste, 0);
            }

            ret.put(paste, ret.get(paste) + 1);
        }

        return ret;
    }

    public AlchemyContract[] getEffectiveContracts() {
        if (effectiveContracts == null || effectiveContracts.length != 3) {
            return null;
        }

        for (var contract : effectiveContracts) {
            if (contract == null || contract.getPotion() == AlchemyPotion.NONE || contract.getType() == AlchemyBuilding.NONE) {
                return null;
            }
        }

        return effectiveContracts;
    }

    public AlchemyContract getBestContract(AlchemyContract[] contracts) {
        var bestIndex = getBestContractIndex(contracts);
        if (bestIndex == -1) {
            return null;
        }

        return contracts[bestIndex];
    }

    public int getBestContractIndex(AlchemyContract[] contracts) {
        if (contracts == null || contracts.length == 0) {
            return -1;
        }

        int bestLyeIndex = -1;
        int bestLyeCount = -1;
        for (var i = 0; i < contracts.length; i++) {
            var contract = contracts[i];
            if (!canMakePotion(contract.getPotion())) {
                continue;
            }

            if (contract.getPotion().getLyeRequired() > bestLyeCount) {
                bestLyeIndex = i;
                bestLyeCount = contract.getPotion().getLyeRequired();
            }
        }

        return bestLyeIndex;
    }

    public AlchemyBuilding getCurrentlyUsingBuilding() {
        if (lastUsedBuilding == AlchemyBuilding.NONE || lastUsedBuildingTick <= 0) {
            return AlchemyBuilding.NONE;
        }

        if (lastUsedBuildingTick > client.getTickCount() || (client.getTickCount() - lastUsedBuildingTick) >= 2) {
            return AlchemyBuilding.NONE;
        }

        if (!client.getLocalPlayer().getWorldLocation().equals(lastUsedBuilding.getInteractionLocation())) {
            return AlchemyBuilding.NONE;
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

    public boolean canMakePotion(AlchemyPotion potion) {
        if (client.getBoostedSkillLevel(Skill.HERBLORE) < potion.getHerbloreLevel()) {
            return false;
        }

        return true;
    }


    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event) {
        if (event.getSoundId() == 2655 && client.getLocalPlayer().getWorldLocation().equals(AlchemyBuilding.AGITATOR_HOMOGENIZER.getInteractionLocation())) {
            lastAgitatorSoundEffectPlayedTick = client.getTickCount();
        }
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
        handleTileObject(event.getDecorativeObject());
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
        TileObject newObject = event.getDecorativeObject();

        if (newObject.getId() == Constants.OBJECT_LEVER_AGA) {
            leverObjects.remove(AlchemyPaste.AGA);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        handleTileObject(event.getGameObject());
    }

    private void handleTileObject(TileObject newObject) {
        if (newObject.getId() == Constants.OBJECT_MIXER) {
            mixerObject = newObject;
        }
        if (newObject.getId() == Constants.OBJECT_LEVER_MOX) {
            leverObjects.put(AlchemyPaste.MOX, newObject);
        }
        if (newObject.getId() == Constants.OBJECT_LEVER_LYE) {
            leverObjects.put(AlchemyPaste.LYE, newObject);
        }

        var building = AlchemyBuilding.FromBuildingObjectId(newObject.getId());
        if (building != AlchemyBuilding.NONE) {
            buildingObjects.put(building, newObject);
        }

        if (newObject.getId() == Constants.OBJECT_CONVEYOR_BELT) {
            conveyorObjects.add(newObject);
        }

        if (newObject.getId() == Constants.OBJECT_HOPPER) {
            hopperObject = newObject;
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        TileObject oldObject = event.getGameObject();
        if (oldObject == mixerObject) {
            mixerObject = null;
        }
        if (oldObject.getId() == Constants.OBJECT_LEVER_MOX) {
            leverObjects.remove(AlchemyPaste.MOX);
        }
        if (oldObject.getId() == Constants.OBJECT_LEVER_AGA) {
            leverObjects.remove(AlchemyPaste.AGA);
        }
        if (oldObject.getId() == Constants.OBJECT_LEVER_LYE) {
            leverObjects.remove(AlchemyPaste.LYE);
        }

        var building = AlchemyBuilding.FromBuildingObjectId(oldObject.getId());
        if (building != AlchemyBuilding.NONE) {
            buildingObjects.remove(building);
        }

        if (oldObject.getId() == Constants.OBJECT_CONVEYOR_BELT) {
            conveyorObjects.remove(oldObject);
        }

        if (oldObject.getId() == Constants.OBJECT_HOPPER) {
            hopperObject = null;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        if (!isInArea()) {
            return;
        }

        var message = event.getMessage();

        // Collect base
        if (message.startsWith("You collect some ") && message.endsWith("from the mixing vessel.")) {
            var potionName = event.getMessage()
                    .replace("You collect some <col=6800bf>", "")
                    .replace("You collect some <col=a53fff>", "")
                    .replace("</col> from the mixing vessel.", "");
            var collectedPotion = AlchemyPotion.FromName(potionName);

            for (var contract : contractsInState(ContractState.BASE_IN_MIXER)) {
                if (contract.consumeBasePotionAddedToInventoryEvent(collectedPotion)) {
                    break;
                }
            }
        }

        // Finish processing item
        if (message.startsWith("You finish ") && message.endsWith("</col>.")) {
            message = message.replace("You finish ", "");
            var crystalising = message.startsWith("crystallising ");
            var homogenizing = message.startsWith("homogenising ");
            var concentrating = message.startsWith("concentrating ");

            var potionName = message
                    .replace("crystallising ", "")
                    .replace("homogenising ", "")
                    .replace("concentrating ", "")
                    .replace("the <col=6800bf>", "")
                    .replace("the <col=a53fff>", "")
                    .replace("</col>.", "");
            var collectedPotion = AlchemyPotion.FromName(potionName);
            AlchemyBuilding collectedPotionType = crystalising ? AlchemyBuilding.ALEMBIC_CRYSTALIZER : (homogenizing ? AlchemyBuilding.AGITATOR_HOMOGENIZER : AlchemyBuilding.RETORT_CONCENTRATOR);

            for (var contract : contractsInState(ContractState.PROCESSING_BASE)) {
                if (contract.consumeBuildingProcessingFinished(collectedPotion, collectedPotionType)) {
                    break;
                }
            }
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        handleVarbit(event.getVarbitId(), event.getValue());
    }

    public boolean hasAllContracts() {
        for (var contract : this.rawContracts) {
            if (contract == null || contract.getType() == AlchemyBuilding.NONE || contract.getPotion() == AlchemyPotion.NONE) {
                return false;
            }
        }

        return true;
    }

    private void handleVarbit(int varbitId, int varbitValue) {
        var buildingBeingUsed = AlchemyBuilding.FromProgressVarbitId(varbitId);
        if (buildingBeingUsed != AlchemyBuilding.NONE) {
            handleBuildingUsed(buildingBeingUsed, varbitValue);
            return;
        }

        // Handle digweed
        for(var digweed : DigweedSpawn.values()) {
            if(!isInArea()) {
                break;
            }

            if(digweed == DigweedSpawn.NONE) {
                continue;
            }

            if(digweed.getVarbitId() != varbitId) {
                continue;
            }

            if(varbitValue == 0) {
                client.clearHintArrow();
            } else {
                client.setHintArrow(digweed.getSpawnPoint());
                break;
            }
        }

        switch (varbitId) {
            case Constants.VB_PASTE_COUNT_MOX:
                hopperMox = varbitValue;
                return;
            case Constants.VB_PASTE_COUNT_AGA:
                hopperAga = varbitValue;
                return;

            case Constants.VB_PASTE_COUNT_LYE:
                hopperLye = varbitValue;
                return;


            case Constants.VB_POINTS_COUNT_MOX:
                pointsMox = varbitValue;
                return;
            case Constants.VB_POINTS_COUNT_AGA:
                pointsAga = varbitValue;
                return;
            case Constants.VB_POINTS_COUNT_LYE:
                pointsLye = varbitValue;
                return;

            case Constants.VB_MIXER_POTION:
                handleNewMixerPotion(AlchemyPotion.FromId(varbitValue));
                return;

            case Constants.VB_ALEMBIC_POTION:
                handleBuildingPotionChanged(AlchemyBuilding.ALEMBIC_CRYSTALIZER, AlchemyPotion.FromId(varbitValue));
                return;
            case Constants.VB_RETORT_POTION:
                handleBuildingPotionChanged(AlchemyBuilding.RETORT_CONCENTRATOR, AlchemyPotion.FromId(varbitValue));
                return;
            case Constants.VB_AGITATOR_POTION:
                handleBuildingPotionChanged(AlchemyBuilding.AGITATOR_HOMOGENIZER, AlchemyPotion.FromId(varbitValue));
                return;

            case Constants.VB_CONTRACT_1_POTION:
                rawContracts[0].setPotion(AlchemyPotion.FromId(varbitValue));
                rethinkContracts();
                return;
            case Constants.VB_CONTRACT_1_TYPE:
                rawContracts[0].setType(AlchemyBuilding.FromId(varbitValue));
                rethinkContracts();
                return;
            case Constants.VB_CONTRACT_2_POTION:
                rawContracts[1].setPotion(AlchemyPotion.FromId(varbitValue));
                rethinkContracts();
                return;
            case Constants.VB_CONTRACT_2_TYPE:
                rawContracts[1].setType(AlchemyBuilding.FromId(varbitValue));
                rethinkContracts();
                return;
            case Constants.VB_CONTRACT_3_POTION:
                rawContracts[2].setPotion(AlchemyPotion.FromId(varbitValue));
                rethinkContracts();
                return;
            case Constants.VB_CONTRACT_3_TYPE:
                rawContracts[2].setType(AlchemyBuilding.FromId(varbitValue));
                rethinkContracts();
                return;
        }

    }

    private void handleNewMixerPotion(AlchemyPotion newPotion) {
        // If the new potion in the mixer isn't no-potion, and there are contracts in the BASE_IN_MIXER state,
        // we need to set them back to the "waiting for mixer" state
        if (newPotion != AlchemyPotion.NONE) {
            for (var contract : contractsInState(ContractState.BASE_IN_MIXER)) {
                if (contract.consumeBasePotionNoLongerInMixerEvent()) {
                    break;
                }
            }

            for (var contract : contractsInState(ContractState.SHOULD_MAKE_BASE)) {
                if (contract.consumeBasePotionInMixerEvent(newPotion)) {
                    break;
                }
            }
        }
    }

    private void handleBuildingPotionChanged(AlchemyBuilding building, AlchemyPotion potion) {
        if (potion == AlchemyPotion.NONE) {
            return;
        }

        for (var contract : contractsInState(ContractState.SHOULD_PROCESS_BASE)) {
            if (contract.consumeBuildingProcessingStarted(potion, building)) {
                break;
            }
        }
    }

    private void handleBuildingUsed(AlchemyBuilding building, int progress) {
        var player = client.getLocalPlayer();
        var interacting = player.getInteracting();

        if (building == AlchemyBuilding.AGITATOR_HOMOGENIZER) {
            homogenizerProgress = progress;
        }
        if (building == AlchemyBuilding.RETORT_CONCENTRATOR) {
            concentratorProgress = progress;
        }
        if (building == AlchemyBuilding.ALEMBIC_CRYSTALIZER) {
            crystalizerProgress = progress;
        }

        if (progress != 0) {
            lastUsedBuilding = building;
            lastUsedBuildingTick = client.getTickCount();
        } else if (progress == 0) {
            lastUsedBuilding = AlchemyBuilding.NONE;
        }
    }

    private void clearContracts() {
        for (var i = 0; i < rawContracts.length; i++) {
            rawContracts[i].reset();
        }

        effectiveContracts = null;
    }

    private void refreshContractsFromVarbits() {
        handleVarbit(Constants.VB_CONTRACT_1_POTION, client.getVarbitValue(Constants.VB_CONTRACT_1_POTION));
        handleVarbit(Constants.VB_CONTRACT_1_TYPE, client.getVarbitValue(Constants.VB_CONTRACT_1_TYPE));
        handleVarbit(Constants.VB_CONTRACT_2_POTION, client.getVarbitValue(Constants.VB_CONTRACT_2_POTION));
        handleVarbit(Constants.VB_CONTRACT_2_TYPE, client.getVarbitValue(Constants.VB_CONTRACT_2_TYPE));
        handleVarbit(Constants.VB_CONTRACT_3_POTION, client.getVarbitValue(Constants.VB_CONTRACT_3_POTION));
        handleVarbit(Constants.VB_CONTRACT_3_TYPE, client.getVarbitValue(Constants.VB_CONTRACT_3_TYPE));
        handleVarbit(Constants.VB_PASTE_COUNT_MOX, client.getVarbitValue(Constants.VB_PASTE_COUNT_MOX));
        handleVarbit(Constants.VB_PASTE_COUNT_AGA, client.getVarbitValue(Constants.VB_PASTE_COUNT_AGA));
        handleVarbit(Constants.VB_PASTE_COUNT_LYE, client.getVarbitValue(Constants.VB_PASTE_COUNT_LYE));
    }

    private void rethinkContracts() {
        if (!hasAllContracts()) {
            return;
        }

        int mixalot = -1;
        int xxy = 0;
        int lastXXY = -1;
        int i = -1;
        for (var contract : rawContracts) {
            i++;
            if (!contract.isCompleteRecipe()) {
                continue;
            }

            if (contract.getPotion().getMoxRequired() == 3 || contract.getPotion().getAgaRequired() == 3 || contract.getPotion().getLyeRequired() == 3) {
            } else if(contract.getPotion().getMoxRequired() == 2 || contract.getPotion().getAgaRequired() == 2 || contract.getPotion().getLyeRequired() == 2) {
                xxy++;
                lastXXY = i;
            }
            else {
                mixalot = i;
            }
        }

        boolean hasMAL = mixalot != -1;

        if(hasMAL || xxy == 3) {
            rawContracts[0].select();
            rawContracts[1].select();
            rawContracts[2].select();
        } else {
            rawContracts[lastXXY == -1 ? 0 : lastXXY].select();
        }

        resortContracts();
    }

    private void resortContracts() {
        effectiveContracts = Arrays
                .stream(rawContracts)
                .sorted(Comparator.comparingInt(c -> c.getType().ordinal()))
                .toArray(AlchemyContract[]::new);
    }

    public void scanForObjects() {
        var worldView = client.getTopLevelWorldView();
        var scene = worldView.getScene();
        var tiles = scene.getTiles();
        for (var tileX : tiles) {
            for (var tileY : tileX) {
                for (var tile : tileY) {
                    if(tile == null) {
                        continue;
                    }

                    var decorativeObject = tile.getDecorativeObject();
                    if (decorativeObject != null) {
                        handleTileObject(decorativeObject);
                    }

                    for (var gameObject : tile.getGameObjects()) {
                        if (gameObject == null) {
                            break;
                        }
                        handleTileObject(gameObject);
                    }
                }
            }
        }
    }

    public AlchemyContract[] selectedContracts() {
        return contractsWhere(c -> c.getState().ordinal() >= ContractState.SELECTED.ordinal());
    }

    public AlchemyContract[] selectedContractsForBuilding(AlchemyBuilding building) {
        return contractsWhere(c -> c.getState().ordinal() >= ContractState.SELECTED.ordinal() && c.getType() == building);
    }

    public AlchemyContract[] contractsInState(ContractState state) {
        return contractsWhere(c -> c.getState() == state);
    }

    public AlchemyContract[] contractsInStates(ContractState[] states) {
        return contractsWhere(c -> Arrays.stream(states).anyMatch(s -> s == c.getState()));
    }

    public AlchemyContract[] contractsWhere(Predicate<AlchemyContract> predicate) {
        var contracts = getEffectiveContracts();
        if (contracts == null || contracts.length != 3) {
            return new AlchemyContract[0];
        }

        var ret = new ArrayList<>();
        for (var contract : contracts) {
            if (predicate.test(contract)) {
                ret.add(contract);
            }
        }
        return ret.toArray(new AlchemyContract[0]);
    }
}
