package com.tacz.guns.entity.sync;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.sync.core.IDataSerializer;
import com.tacz.guns.entity.sync.core.Serializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ModSerializers {
    private static final String INVENTORY_INDEX_TAG = "InventoryIndex";
    private static final String ITEM_STACK_TAG = "ItemStack";

    public static final IDataSerializer<ReloadState> RELOAD_STATE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ReloadState value) {
            buf.writeInt(value.getStateType().ordinal());
            buf.writeLong(value.getCountDown());
        }

        @Override
        public ReloadState read(FriendlyByteBuf buf) {
            ReloadState reloadState = new ReloadState();
            reloadState.setStateType(ReloadState.StateType.values()[buf.readInt()]);
            reloadState.setCountDown(buf.readLong());
            return reloadState;
        }

        @Override
        public Tag write(HolderLookup.Provider provider, ReloadState value) {
            CompoundTag compound = new CompoundTag();
            compound.putString("StateType", value.getStateType().toString());
            compound.putLong("CountDown", value.getCountDown());
            return compound;
        }

        @Override
        public ReloadState read(HolderLookup.Provider provider, Tag nbt) {
            CompoundTag compound = (CompoundTag) nbt;
            try {
                ReloadState.StateType stateType = ReloadState.StateType.valueOf(compound.getString("StateType"));
                long countDown = compound.getLong("CountDown");
                ReloadState reloadState = new ReloadState();
                reloadState.setStateType(stateType);
                reloadState.setCountDown(countDown);
                return reloadState;
            } catch (IllegalArgumentException ignore) {
            }
            return new ReloadState();
        }
    };

    public static final IDataSerializer<BodyGunDisplayData> BODY_GUN_DISPLAY = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, BodyGunDisplayData value) {
            buf.writeVarInt(value.entries().size());
            for (BodyGunDisplayData.Entry entry : value.entries()) {
                buf.writeVarInt(entry.inventoryIndex());
                Serializers.ITEM_STACK.write(buf, entry.itemStack());
            }
        }

        @Override
        public BodyGunDisplayData read(FriendlyByteBuf buf) {
            int size = readEntryCount(buf);
            List<BodyGunDisplayData.Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add(new BodyGunDisplayData.Entry(buf.readVarInt(), Serializers.ITEM_STACK.read(buf)));
            }
            return entries.isEmpty() ? BodyGunDisplayData.EMPTY : new BodyGunDisplayData(entries);
        }

        @Override
        public Tag write(HolderLookup.Provider provider, BodyGunDisplayData value) {
            ListTag entriesTag = new ListTag();
            for (BodyGunDisplayData.Entry entry : value.entries()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putInt(INVENTORY_INDEX_TAG, entry.inventoryIndex());
                entryTag.put(ITEM_STACK_TAG, entry.itemStack().save(provider, new CompoundTag()));
                entriesTag.add(entryTag);
            }
            return entriesTag;
        }

        @Override
        public BodyGunDisplayData read(HolderLookup.Provider provider, Tag tag) {
            if (!(tag instanceof ListTag entriesTag)) {
                return BodyGunDisplayData.EMPTY;
            }
            List<BodyGunDisplayData.Entry> entries = new ArrayList<>(entriesTag.size());
            for (int index = 0; index < entriesTag.size(); index++) {
                CompoundTag entryTag = entriesTag.getCompound(index);
                int inventoryIndex = entryTag.getInt(INVENTORY_INDEX_TAG);
                ItemStack itemStack = ItemStack.parseOptional(provider, entryTag.getCompound(ITEM_STACK_TAG));
                entries.add(new BodyGunDisplayData.Entry(inventoryIndex, itemStack));
            }
            return entries.isEmpty() ? BodyGunDisplayData.EMPTY : new BodyGunDisplayData(entries);
        }
    };

    private static int readEntryCount(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > Inventory.INVENTORY_SIZE) {
            throw new IllegalArgumentException("Invalid body gun entry count: " + size);
        }
        return size;
    }
}
