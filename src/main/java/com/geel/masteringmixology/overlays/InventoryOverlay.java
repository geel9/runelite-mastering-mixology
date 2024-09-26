package com.geel.masteringmixology.overlays;

import com.geel.masteringmixology.MasteringMixologyConfig;

import com.geel.masteringmixology.enums.HerbPastes;

import java.awt.*;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.Point;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class InventoryOverlay extends Overlay {

    private final Client client;
    private final MasteringMixologyConfig config;

    @Inject
    public InventoryOverlay(Client client, MasteringMixologyConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.inventoryOverlay()) {
            Widget inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);
            if (inventoryWidget.isHidden()) {
                inventoryWidget = client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER);
                if (inventoryWidget.isHidden()) {
                    return null;
                }
            }
            Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
            for (int i = 0; i < items.length; i++) {
                Item item = items[i];
                if (item == null) {
                    continue;
                }
                for (HerbPastes herbPaste : HerbPastes.values()) {
                    if (herbPaste.CanItemBeUsedAsPaste(item.getId())) {
                        drawOverlay(graphics, inventoryWidget, i, herbPaste.getColor());
                    }
                }
            }
        }
        return null;
    }

    private void drawOverlay(Graphics2D graphics, Widget inventoryWidget, int index, Color color) {
        Point location = inventoryWidget
                .getChild(index)
                .getCanvasLocation();
        graphics.setColor(color);
        graphics.fillRect(location.getX() + 20, location.getY() + 20, 10, 10);
    }
}
