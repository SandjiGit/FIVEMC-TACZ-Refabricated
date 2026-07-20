package com.tacz.guns.entity.sync;

import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal inventory projection required to render a player's stored guns on remote clients.
 */
public record BodyGunDisplayData(List<Entry> entries) {
    public static final BodyGunDisplayData EMPTY = new BodyGunDisplayData(List.of());

    public BodyGunDisplayData {
        entries = List.copyOf(entries);
    }

    public static BodyGunDisplayData fromInventory(Inventory inventory) {
        List<Entry> entries = null;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.items.get(slot);
            if (isVisibleGun(stack)) {
                if (entries == null) {
                    entries = new ArrayList<>(2);
                }
                entries.add(new Entry(slot, stack));
            }
        }
        return entries == null ? EMPTY : new BodyGunDisplayData(entries);
    }

    /**
     * Checks the current render projection without allocating copied item stacks.
     */
    public boolean matchesInventory(Inventory inventory) {
        int entryIndex = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.items.get(slot);
            if (!isVisibleGun(stack)) {
                continue;
            }
            if (entryIndex >= entries.size()) {
                return false;
            }
            Entry entry = entries.get(entryIndex++);
            if (entry.inventoryIndex != slot || !ItemStack.matches(entry.itemStack, stack)) {
                return false;
            }
        }
        return entryIndex == entries.size();
    }

    private static boolean isVisibleGun(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun != null && gun.isBodyGunVisible(stack);
    }

    public record Entry(int inventoryIndex, ItemStack itemStack) {
        public Entry {
            if (inventoryIndex < 0 || inventoryIndex >= Inventory.INVENTORY_SIZE) {
                throw new IllegalArgumentException("Body gun inventory index is out of bounds: " + inventoryIndex);
            }
            itemStack = Objects.requireNonNull(itemStack, "itemStack").copy();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Entry other)) {
                return false;
            }
            return inventoryIndex == other.inventoryIndex && ItemStack.matches(itemStack, other.itemStack);
        }

        @Override
        public int hashCode() {
            int result = 31 * inventoryIndex + ItemStack.hashItemAndComponents(itemStack);
            return 31 * result + itemStack.getCount();
        }
    }
}
