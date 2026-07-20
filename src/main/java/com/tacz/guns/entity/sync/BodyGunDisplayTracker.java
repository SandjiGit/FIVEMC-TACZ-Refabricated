package com.tacz.guns.entity.sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/**
 * Refreshes body-gun render data only when the authoritative server inventory changes.
 */
public final class BodyGunDisplayTracker {
    private int inventoryRevision = Integer.MIN_VALUE;
    private int selectedSlot = -1;

    public void tick(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int currentRevision = inventory.getTimesChanged();
        if (currentRevision == inventoryRevision && inventory.selected == selectedSlot) {
            return;
        }
        inventoryRevision = currentRevision;
        selectedSlot = inventory.selected;
        ModSyncedEntityData.BODY_GUN_DISPLAY_KEY.setValue(player, BodyGunDisplayData.fromInventory(inventory));
    }
}
