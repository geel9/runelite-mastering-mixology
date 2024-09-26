package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyPotion;
import com.geel.masteringmixology.enums.AlchemyBuilding;
import com.geel.masteringmixology.overlays.BankOverlay;
import com.geel.masteringmixology.overlays.InventoryOverlay;
import com.geel.masteringmixology.overlays.MixologyOverlay;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Mastering Mixology",
        description = "Mixing and mastering",
        tags = {"mastering", "mixology", "herblore", "alchemy", "minigame", "herb", "paste", "mox", "aga", "lye"}
)
@Slf4j
public class MasteringMixologyPlugin extends Plugin {

    @Inject
    @Getter
    private MasteringMixologyConfig config;

    @Inject
    private EventBus eventBus;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MixologyOverlay overlay;

    @Inject
    private InventoryOverlay inventoryOverlay;

    @Inject
    private BankOverlay bankOverlay;

    @Inject
    private MixologyGameState mixologyGameState;


    @Provides
    MasteringMixologyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MasteringMixologyConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(mixologyGameState);
//        clientThread.invoke(this::loadFromConfig);
        mixologyGameState.start();
        overlayManager.add(overlay);
        overlayManager.add(inventoryOverlay);
        overlayManager.add(bankOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(mixologyGameState);
//        clientThread.invoke(this::resetParams);
        mixologyGameState.stop();
        overlayManager.remove(overlay);
        overlayManager.remove(inventoryOverlay);
        overlayManager.remove(bankOverlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
//            clientThread.invoke(this::loadFromConfig);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (!configChanged.getGroup().equals(MasteringMixologyConfig.GROUP)) {
            return;
        }

//        clientThread.invoke(this::loadFromConfig);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event) {
        if (event.getCommand().equals("contracts")) {
            clientThread.invoke(this::printContracts);
        }
    }

    public void printContracts() {
        var contracts = mixologyGameState.getContracts();
        if (contracts == null || contracts.length == 0) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Mixology Master", "No active contracts?", "");
            return;
        }

        for (var i = 0; i < contracts.length; i++) {
            var contract = contracts[i];
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", i + ": " + contract.getType().name() + " " + contract.getPotion().getName(), "");
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
//        if (event.getGroupId() == InterfaceID.FAIRY_RING_PANEL && config.autoJumpFairyring()) {
//            clientThread.invokeLater(this::handleFairyRingPanel);
//        }
//
//        if (event.getGroupId() == InterfaceID.DIALOG_OPTION && isInBurrows()) {
//            clientThread.invokeLater(this::handleBackToBackDialog);
//        }

        log.info("Widget loaded");
    }


    @Subscribe
    public void onGameTick(GameTick event) {
        if (!mixologyGameState.isInArea()) {
            return;
        }

        var building = mixologyGameState.getCurrentlyUsingBuilding();
        if(building == AlchemyBuilding.ALEMBIC_CRYSTALIZER) {
            if(mixologyGameState.getCrystalizerProgress() == 4) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NOW", "");
            }
        }
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event) {
        if(!mixologyGameState.isInArea()) {
            return;
        }

        // Try to fix up that there widget
        var containerWidget = client.getWidget(Constants.WIDGET_MIXOLOGY_OVERLAY_CONTAINER);
        if (containerWidget == null) {
            return;
        }

        var children = containerWidget.getDynamicChildren();
        if (children.length < 7) {
            return;
        }

        var topLabel = children[0];
        var potion1Label = children[2];
        var potion2Label = children[4];
        var potion3Label = children[6];

        if(!topLabel.getText().equals("Potion Orders") || topLabel.isHidden()) {
            return;
        }

        if(potion1Label.getText().length() != 3) {
            potion1Label.setText(potionNameToRecipe(potion1Label.getText()));
        }
        if(potion2Label.getText().length() != 3) {
            potion2Label.setText(potionNameToRecipe(potion2Label.getText()));
        }
        if(potion3Label.getText().length() != 3) {
            potion3Label.setText(potionNameToRecipe(potion3Label.getText()));
        }

        var bestContractIndex = mixologyGameState.getBestContractIndex(mixologyGameState.getContracts());

        // Gotta be a better way to do this
        int onColor = Color.GREEN.getRGB();
        int offColor = Color.WHITE.getRGB();
        potion1Label.setTextColor(bestContractIndex == 0 ? onColor : offColor);
        potion2Label.setTextColor(bestContractIndex == 1 ? onColor : offColor);
        potion3Label.setTextColor(bestContractIndex == 2 ? onColor : offColor);
    }

    private String potionNameToRecipe(String potionName) {
        var potion = AlchemyPotion.FromName(potionName);

        if(potion == AlchemyPotion.NONE) {
            return potionName;
        }

        return potion.getRecipeName();
    }
}
