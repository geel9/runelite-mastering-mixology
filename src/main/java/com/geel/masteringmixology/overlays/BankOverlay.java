package com.geel.masteringmixology.overlays;

import com.geel.masteringmixology.MasteringMixologyConfig;
import com.geel.masteringmixology.enums.HerbPastes;

import java.awt.*;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;

public class BankOverlay extends Overlay {
    private final Client client;
    private final MasteringMixologyConfig config;

    @Inject
    BankOverlay(Client client, MasteringMixologyConfig config) {
        this.client = client;
        this.config = config;
        this.setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Widget bankWidget = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
        if (bankWidget == null || bankWidget.isHidden() || !config.bankOverlay()) {
            return null;
        }

        Widget[] dynamicChildren = bankWidget.getDynamicChildren();
        for (int i = 0; i < dynamicChildren.length; i++) {
            Widget widget = dynamicChildren[i];
            int itemId = widget.getItemId();
            if (itemId <= 0) {
                continue;
            }
            for (HerbPastes herbPaste : HerbPastes.values()) {
                if (herbPaste.CanItemBeUsedAsPaste(itemId)) {
                    drawOverlay(graphics, widget, herbPaste.getColor());
                }
            }
        }
        return null;
    }
    private void drawOverlay(Graphics2D graphics, Widget widget, Color color) {
        Point location = widget
                .getCanvasLocation();
        graphics.setColor(color);
        graphics.fillRect(location.getX() + 15, location.getY(), 10, 10);
    }
}
