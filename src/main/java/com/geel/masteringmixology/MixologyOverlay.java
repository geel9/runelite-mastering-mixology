package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyContract;
import com.geel.masteringmixology.enums.AlchemyPaste;
import com.geel.masteringmixology.enums.AlchemyBuilding;
import com.geel.masteringmixology.enums.ContractState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
class MixologyOverlay extends Overlay {
    private final Client client;
    private final MixologyGameState gameState;
    private final MasteringMixologyPlugin plugin;

    @Inject
    private MixologyOverlay(Client client, MixologyGameState gameState, MasteringMixologyPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.gameState = gameState;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!gameState.isInArea())
            return null;

        final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
        if (itemContainer == null) {
            return null;
        }

        var contracts = gameState.getContracts();
        var bestContract = gameState.getBestContract(contracts);

        if (bestContract == null) {
            return null;
        }

        /**
         * Logic (in this order):
         *
         * If contract in PROCESSING_BASE state, active highlight object
         * If contract in BASE_IN_MIXER state, highlight mixer
         * If contract in SHOULD_MAKE_BASE state, highlight levers (for _first_ contract)
         * If contract in SHOULD_PROCESS_BASE state, passive highlight object
         * If contract in READY state, highlight conveyor
         */

        var needResourcesForContracts = gameState.contractsInStates(new ContractState[]{ContractState.SELECTED, ContractState.SHOULD_MAKE_BASE, ContractState.BASE_IN_MIXER});
        var neededMox = 0;
        var neededAga = 0;
        var neededLye = 0;

        for(var resourceContract : needResourcesForContracts) {
            neededMox += resourceContract.getPotion().getMoxRequired() * 10;
            neededAga += resourceContract.getPotion().getAgaRequired() * 10;
            neededLye += resourceContract.getPotion().getLyeRequired() * 10;
        }

        var hopperTooLow = (neededMox > gameState.getHopperMox()) || (neededAga > gameState.getHopperAga()) || (neededLye > gameState.getHopperLye());

        var processingContracts = gameState.contractsInState(ContractState.PROCESSING_BASE);
        var baseInMixerContracts = gameState.contractsInState(ContractState.BASE_IN_MIXER);
        var shouldMakeBaseContracts = gameState.contractsInState(ContractState.SHOULD_MAKE_BASE);
        var shouldProcessBaseContracts = gameState.contractsInState(ContractState.SHOULD_PROCESS_BASE);
        var readyContracts = gameState.contractsInState(ContractState.READY);

        if (hopperTooLow) {
            hopperHighlight(graphics);
            return null;
        }

        if (processingContracts.length > 0) {
            buildingActiveHighlight(graphics, processingContracts[0].getType());
            return null;
        }

        if (baseInMixerContracts.length > 0) {
            mixerHighlight(graphics);
            return null;
        }

        if (shouldMakeBaseContracts.length > 0) {
            leverHighlight(graphics, shouldMakeBaseContracts[0]);
            return null;
        }

        if (shouldProcessBaseContracts.length > 0) {
            buildingPassiveHighlight(graphics, shouldProcessBaseContracts[0]);
            return null;
        }

        // TODO: Better state tracking than "any infused of the right type in inventory"
        if (readyContracts.length > 0) {
            conveyorHighlight(graphics);
            return null;
        }

        return null;
    }

    private void buildingActiveHighlight(Graphics2D graphics, AlchemyBuilding building) {
        var buildingObject = gameState.getBuildingObjects().get(building);

        if (buildingObject == null) {
            return;
        }

        Color highlightColor = Color.GREEN;
        if (building == AlchemyBuilding.ALEMBIC_CRYSTALIZER && gameState.getCrystalizerProgress() == 4) {
            highlightColor = Color.YELLOW;
        }

        if(building == AlchemyBuilding.AGITATOR_HOMOGENIZER && (client.getTickCount() - gameState.getLastAgitatorSoundEffectPlayedTick()) <= 1) {
            highlightColor = Color.YELLOW;
        }

        highlightObject(graphics, buildingObject, highlightColor);
    }

    private void buildingPassiveHighlight(Graphics2D graphics, AlchemyContract contract) {
        var building = contract.getType();
        var buildingObject = gameState.getBuildingObjects().get(building);

        if (buildingObject == null) {
            return;
        }

        highlightObject(graphics, buildingObject, Color.YELLOW);
    }

    private void hopperHighlight(Graphics2D graphics) {
        highlightObject(graphics, gameState.getHopperObject(), Color.RED);
    }

    private void mixerHighlight(Graphics2D graphics) {
        highlightObject(graphics, gameState.getMixerObject(), Color.GREEN);
    }

    private void leverHighlight(Graphics2D graphics, AlchemyContract contract) {
        var moxLever = gameState.getLeverObjects().get(AlchemyPaste.MOX);
        var agaLever = gameState.getLeverObjects().get(AlchemyPaste.AGA);
        var lyeLever = gameState.getLeverObjects().get(AlchemyPaste.LYE);

        if (moxLever == null || agaLever == null || lyeLever == null) {
            return;
        }

        var currentMix = gameState.getCurrentMix();
        var currentMox = currentMix.getOrDefault(AlchemyPaste.MOX, 0);
        var currentAga = currentMix.getOrDefault(AlchemyPaste.AGA, 0);
        var currentLye = currentMix.getOrDefault(AlchemyPaste.LYE, 0);
        var requiredMox = contract.getPotion().getMoxRequired();
        var requiredAga = contract.getPotion().getAgaRequired();
        var requiredLye = contract.getPotion().getLyeRequired();

        if (currentMox < requiredMox) {
            highlightObject(graphics, moxLever, leverDeltaToColor(requiredMox - currentMox));
        }
        if (currentAga < requiredAga) {
            highlightObject(graphics, agaLever, leverDeltaToColor(requiredAga - currentAga));
        }
        if (currentLye < requiredLye) {
            highlightObject(graphics, lyeLever, leverDeltaToColor(requiredLye - currentLye));
        }
    }

    private Color leverDeltaToColor(int delta) {
        switch(delta) {
            case 1:
                return Color.RED;
            case 2:
                return Color.YELLOW;
            case 3:
                return Color.GREEN;
            default:
                return Color.PINK;
        }
    }

    private void conveyorHighlight(Graphics2D graphics) {
        for (var conveyor : gameState.getConveyorObjects()) {
            highlightObject(graphics, conveyor, Color.GREEN);
        }
    }


    private void highlightObject(Graphics2D graphics, TileObject object, Color color) {
        Point mousePosition = client.getMouseCanvasPosition();

        Shape objectClickbox = object.getClickbox();
        if (objectClickbox != null) {
            if (objectClickbox.contains(mousePosition.getX(), mousePosition.getY())) {
                graphics.setColor(color.darker());
            } else {
                graphics.setColor(color);
            }

            graphics.draw(objectClickbox);
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            graphics.fill(objectClickbox);
        }
    }
}
