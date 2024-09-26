package com.geel.masteringmixology;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
class MixologyOverlay extends Overlay {
    private final Client client;
    private final MixologyGameState gameState;

    @Inject
    private MixologyOverlay(Client client, MixologyGameState gameState, MasteringMixologyPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.gameState = gameState;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!gameState.isInArea())
            return null;
//
//        final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
//        if (itemContainer == null) {
//            return null;
//        }
//
//        plugin.getChests().forEach((tileObject, chest) ->
//        {
//            final int numKeys = itemContainer.count(chest.getKey());
//
//            if (numKeys == 0)
//                return;
//
//            final String chestText = chest.name() + " x" + numKeys;
//
//            highlightObject(graphics, tileObject, Color.GREEN);
//
//            net.runelite.api.Point textLocation = tileObject.getCanvasTextLocation(graphics, chestText, 130);
//
//            if (textLocation != null) {
//                OverlayUtil.renderTextLocation(graphics, textLocation, chestText, Color.WHITE);
//            }
//        });

        return null;
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
