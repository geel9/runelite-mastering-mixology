package com.geel.masteringmixology;

import com.geel.masteringmixology.enums.AlchemyContract;
import com.geel.masteringmixology.enums.AlchemyPotion;
import com.geel.masteringmixology.enums.AlchemyBuilding;
import com.geel.masteringmixology.enums.ContractState;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
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
    private MixologyGameState mixologyGameState;

    private boolean stateStarted = false;


    @Provides
    MasteringMixologyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MasteringMixologyConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(mixologyGameState);
        overlayManager.add(overlay);
        stateStarted = false;
        clientThread.invoke(this::shouldEnablePlugin);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(mixologyGameState);
        overlayManager.remove(overlay);
        clientThread.invoke(this::shouldDisablePlugin);
    }

    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event) {
        if(client.getGameState() != GameState.LOGGED_IN) {
            shouldDisablePlugin();
            return;
        }

        if(!mixologyGameState.isInArea()) {
            shouldDisablePlugin();
            return;
        }

        shouldEnablePlugin();
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event) {
        if(!mixologyGameState.isInArea()) {
            return;
        }

        var contracts = mixologyGameState.getContracts();
        if(contracts == null || contracts.length != 3) {
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

        handlePotionContractLabel(contracts[0], potion1Label);
        handlePotionContractLabel(contracts[1], potion2Label);
        handlePotionContractLabel(contracts[2], potion3Label);
    }

    private void handlePotionContractLabel(AlchemyContract contract, Widget label) {
        if(!contract.isCompleteRecipe()) {
            label.setText("NO RECIPE");
            return;
        }

        label.setText(contract.getType().getId() + " " + contract.getPotion().getRecipeName() + " | " + contract.getState().toString());

        if(contract.getState() == ContractState.EXCLUDED) {
            label.setTextColor(Color.RED.getRGB());
        } else if(contract.getState().ordinal() > ContractState.BASE_IN_MIXER.ordinal()) {
            label.setTextColor(Color.GREEN.getRGB());
        } else if(contract.getState().ordinal() > ContractState.SELECTED.ordinal()) {
            label.setTextColor(Color.YELLOW.getRGB());
        }
    }

    private String potionNameToRecipe(String potionName) {
        var potion = AlchemyPotion.FromName(potionName);

        if(potion == AlchemyPotion.NONE) {
            return potionName;
        }

        return potion.getRecipeName();
    }

    private void shouldDisablePlugin() {
        if(!stateStarted) {
            return;
        }

        stateStarted = false;
        mixologyGameState.stop();
        overlayManager.remove(overlay);
    }

    private void shouldEnablePlugin() {
        if(stateStarted) {
            return;
        }

        stateStarted = true;
        mixologyGameState.start();
        overlayManager.add(overlay);
    }
}
