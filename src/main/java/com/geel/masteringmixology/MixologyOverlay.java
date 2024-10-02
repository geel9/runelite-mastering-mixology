package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyContract;
import com.geel.masteringmixology.enums.AlchemyPaste;
import com.geel.masteringmixology.enums.AlchemyBuilding;
import com.geel.masteringmixology.enums.ContractState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

/**
 * NOTE:
 * <p>
 * This is a _perfect_ example of spaghetti code. Good lord, this is bad.
 * <p>
 * You are witnessing the results of a man who is writing something solely for personal use and which he does not
 * expect to have to modify in the future, because the content this plugin is for _sucks_ and I'm only making this plugin so
 * I can do the content as efficiently as possible and be done with it forever.
 * <p>
 * Originally, this overlay only highlighted the _current_ action. Then I made it highlight the "next" action, with as little refactoring as possible.
 * This means that the resultant code is quite shit and spaghettified and hard to follow. Sorry.
 * <p>
 * I know how to do it better, I just don't want to spend the effort.
 * <p>
 * Again, sorry.
 */
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

        var contracts = gameState.getEffectiveContracts();
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

        for (var resourceContract : needResourcesForContracts) {
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
            buildingPassiveHighlight(graphics);

            if (processingContracts.length == 1 && shouldProcessBaseContracts.length == 0) {
                conveyorHighlight(graphics, true);
            }
            return null;
        }

        if (shouldMakeBaseContracts.length > 0 || baseInMixerContracts.length > 0) {
            leverOrMixerHighlight(
                    graphics,
                    shouldMakeBaseContracts.length == 0 ? null : shouldMakeBaseContracts[0],
                    baseInMixerContracts.length == 0 ? null : baseInMixerContracts[0]
            );

            if (shouldMakeBaseContracts.length == 0) {
                buildingPassiveHighlight(graphics);
            }
            return null;
        }

        if (shouldProcessBaseContracts.length > 0) {
            buildingPassiveHighlight(graphics);
            return null;
        }

        // TODO: Better state tracking than "any infused of the right type in inventory"
        if (readyContracts.length > 0) {
            conveyorHighlight(graphics, false);
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

        if (building == AlchemyBuilding.AGITATOR_HOMOGENIZER && (client.getTickCount() - gameState.getLastAgitatorSoundEffectPlayedTick()) <= 2) {
            highlightColor = Color.YELLOW;
        }

        highlightObject(graphics, buildingObject, highlightColor);
    }

    private void buildingPassiveHighlight(Graphics2D graphics) {
        var processingContracts = gameState.contractsInState(ContractState.PROCESSING_BASE);
        var mixerReadyContracts = gameState.contractsInState(ContractState.BASE_IN_MIXER);

        var passiveHighlightIsSecondary = processingContracts.length > 0 || mixerReadyContracts.length > 0;

        // Fuck me jesus christ this is bad
        var buildingContracts = mixerReadyContracts.length == 0
                ? gameState.contractsInState(ContractState.SHOULD_PROCESS_BASE)
                : gameState.selectedContracts();

        var primaryOutlineOpacity = !passiveHighlightIsSecondary ? 100 : 50;
        var primaryFillOpacity = !passiveHighlightIsSecondary ? 50 : 25;
        var otherOutlineOpacity = !passiveHighlightIsSecondary ? 50 : 33;
        var otherFillOpacity = !passiveHighlightIsSecondary ? 25 : 15;

        var primaryColor = !passiveHighlightIsSecondary ? Color.GREEN : Color.YELLOW;
        var secondaryColor = !passiveHighlightIsSecondary ? Color.YELLOW : Color.RED;
        var tertiaryColor = Color.RED;

        if (buildingContracts.length > 0) {
            highlightObject(graphics, gameState.getBuildingObjects().get(buildingContracts[0].getType()), primaryColor, primaryOutlineOpacity, primaryFillOpacity);
        }

        if (buildingContracts.length > 1 && buildingContracts[1].getType() != buildingContracts[0].getType()) {
            highlightObject(graphics, gameState.getBuildingObjects().get(buildingContracts[1].getType()), secondaryColor, otherOutlineOpacity, otherFillOpacity);
        }

        if (buildingContracts.length > 2 && buildingContracts[2].getType() != buildingContracts[0].getType() && buildingContracts[2].getType() != buildingContracts[1].getType()) {
            highlightObject(graphics, gameState.getBuildingObjects().get(buildingContracts[2].getType()), tertiaryColor, otherOutlineOpacity, otherFillOpacity);
        }
    }

    private void hopperHighlight(Graphics2D graphics) {
        highlightObject(graphics, gameState.getHopperObject(), Color.RED);
    }

    private void leverOrMixerHighlight(Graphics2D graphics, AlchemyContract leverContract, AlchemyContract mixerContract) {
        var mixerReady = mixerContract != null;

        if (leverContract != null) {
            var moxLever = gameState.getLeverObjects().get(AlchemyPaste.MOX);
            var agaLever = gameState.getLeverObjects().get(AlchemyPaste.AGA);
            var lyeLever = gameState.getLeverObjects().get(AlchemyPaste.LYE);

            if (moxLever == null || agaLever == null || lyeLever == null) {
                return;
            }

            var currentMix = gameState.getCurrentMix();
            var currentMox = mixerReady ? 0 : currentMix.getOrDefault(AlchemyPaste.MOX, 0);
            var currentAga = mixerReady ? 0 : currentMix.getOrDefault(AlchemyPaste.AGA, 0);
            var currentLye = mixerReady ? 0 : currentMix.getOrDefault(AlchemyPaste.LYE, 0);
            var requiredMox = leverContract.getPotion().getMoxRequired();
            var requiredAga = leverContract.getPotion().getAgaRequired();
            var requiredLye = leverContract.getPotion().getLyeRequired();

            var leverHighlightOpacityFactor = mixerReady ? 0.5d : 1.0d;
            var leverHighlightOutline = (int) (leverHighlightOpacityFactor * 100);
            var leverHighlightFill = (int) (leverHighlightOpacityFactor * 50);

            if (currentMox < requiredMox) {
                highlightObject(graphics, moxLever, leverDeltaToColor(requiredMox - currentMox), leverHighlightOutline, leverHighlightFill);
            }
            if (currentAga < requiredAga) {
                highlightObject(graphics, agaLever, leverDeltaToColor(requiredAga - currentAga), leverHighlightOutline, leverHighlightFill);
            }
            if (currentLye < requiredLye) {
                highlightObject(graphics, lyeLever, leverDeltaToColor(requiredLye - currentLye), leverHighlightOutline, leverHighlightFill);
            }
        }

        if (mixerReady && gameState.getMixerObject() != null) {
            highlightObject(graphics, gameState.getMixerObject(), Color.GREEN);
        }
    }

    private Color leverDeltaToColor(int delta) {
        switch (delta) {
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

    private void conveyorHighlight(Graphics2D graphics, boolean isSecondaryHighlight) {
        for (var conveyor : gameState.getConveyorObjects()) {
            highlightObject(graphics, conveyor, !isSecondaryHighlight ? Color.GREEN : Color.YELLOW);
        }
    }

    private void highlightObject(Graphics2D graphics, TileObject object, Color color) {
        highlightObject(graphics, object, color, 100, 50);
    }

    private void highlightObject(Graphics2D graphics, TileObject object, Color color, int outlineOpacity, int fillOpacity) {
        Point mousePosition = client.getMouseCanvasPosition();

        Shape objectClickbox = object.getClickbox();
        if (objectClickbox == null) return;


        Color outlineColor = color;
        if (objectClickbox.contains(mousePosition.getX(), mousePosition.getY())) {
            outlineColor = color.darker();
        }

        outlineColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineOpacity);
        graphics.setColor(outlineColor);

        graphics.draw(objectClickbox);
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillOpacity));
        graphics.fill(objectClickbox);
    }

}
