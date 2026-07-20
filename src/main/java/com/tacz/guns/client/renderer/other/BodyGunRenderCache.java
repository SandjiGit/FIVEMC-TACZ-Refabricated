package com.tacz.guns.client.renderer.other;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import com.tacz.guns.entity.sync.BodyGunDisplayData;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Caches body-gun inventory projections and immutable render metadata on the render thread.
 */
final class BodyGunRenderCache {
    private static final String PISTOL_TYPE = "pistol";
    private static final LayerGunShow BACK_SHOW = new LayerGunShow(
            new Vector3f(-1, 20, 3),
            new Vector3f(-180, 0, 125),
            new Vector3f(0.5f, 0.5f, 0.5f));
    private static final LayerGunShow BELT_SHOW = new LayerGunShow(
            new Vector3f(-5, 13, -1),
            new Vector3f(-90, -35, 90),
            new Vector3f(0.5f, 0.5f, 0.5f));

    private final LocalDisplayCache localDisplayCache = new LocalDisplayCache();
    private final Map<Player, CachedRenderEntries> playerEntries = new WeakHashMap<>();
    private final Map<LayerGunShow, ResolvedTransform> transforms = new WeakHashMap<>();
    private int resourceGeneration = Integer.MIN_VALUE;

    List<RenderEntry> getRenderEntries(Player player) {
        invalidateResourceDataIfNeeded();
        BodyGunDisplayData displayData = getDisplayData(player);
        if (displayData.entries().isEmpty()) {
            return List.of();
        }
        CachedRenderEntries cached = playerEntries.get(player);
        if (cached != null && cached.source() == displayData) {
            return cached.entries();
        }
        List<RenderEntry> entries = resolveEntries(displayData);
        playerEntries.put(player, new CachedRenderEntries(displayData, entries));
        return entries;
    }

    ResolvedTransform resolveTransform(LayerGunShow gunShow) {
        invalidateResourceDataIfNeeded();
        return transforms.computeIfAbsent(gunShow, BodyGunRenderCache::createTransform);
    }

    private BodyGunDisplayData getDisplayData(Player player) {
        if (player == Minecraft.getInstance().player) {
            return localDisplayCache.get(player);
        }
        return ModSyncedEntityData.BODY_GUN_DISPLAY_KEY.getValue(player);
    }

    private List<RenderEntry> resolveEntries(BodyGunDisplayData displayData) {
        List<RenderEntry> entries = new ArrayList<>(displayData.entries().size());
        for (BodyGunDisplayData.Entry entry : displayData.entries()) {
            RenderEntry renderEntry = resolveEntry(entry);
            if (renderEntry != null) {
                entries.add(renderEntry);
            }
        }
        return List.copyOf(entries);
    }

    @Nullable
    private RenderEntry resolveEntry(BodyGunDisplayData.Entry entry) {
        ItemStack itemStack = entry.itemStack();
        IGun gun = IGun.getIGunOrNull(itemStack);
        if (gun == null) {
            return null;
        }
        GunDisplayInstance display = TimelessAPI.getGunDisplay(itemStack).orElse(null);
        if (display == null) {
            return null;
        }
        boolean pistol = isPistol(itemStack, gun);
        LayerGunShow gunShow = resolveGunShow(display.getHotbarShow(), entry.inventoryIndex(), pistol);
        return new RenderEntry(itemStack, resolveTransform(gunShow), !pistol);
    }

    private static LayerGunShow resolveGunShow(@Nullable Map<Integer, LayerGunShow> hotbarShow, int inventoryIndex, boolean pistol) {
        if (hotbarShow == null || hotbarShow.isEmpty()) {
            return pistol ? BELT_SHOW : BACK_SHOW;
        }
        LayerGunShow slotShow = hotbarShow.get(inventoryIndex);
        if (slotShow != null) {
            return slotShow;
        }
        LayerGunShow defaultShow = hotbarShow.get(0);
        return defaultShow != null ? defaultShow : hotbarShow.values().iterator().next();
    }

    private static boolean isPistol(ItemStack itemStack, IGun gun) {
        ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(gun.getGunId(itemStack)).orElse(null);
        return gunIndex != null && PISTOL_TYPE.equals(gunIndex.getType());
    }

    private static ResolvedTransform createTransform(LayerGunShow gunShow) {
        Vector3f rotation = gunShow.getRotate();
        Quaternionf quaternion = new Quaternionf();
        MathUtil.toQuaternion(
                (float) Math.toRadians(rotation.x()),
                (float) Math.toRadians(rotation.y()),
                (float) Math.toRadians(rotation.z()),
                quaternion);
        return new ResolvedTransform(gunShow.getPos(), gunShow.getScale(), quaternion);
    }

    private void invalidateResourceDataIfNeeded() {
        int currentGeneration = ClientIndexManager.getResourceGeneration();
        if (currentGeneration != resourceGeneration) {
            resourceGeneration = currentGeneration;
            playerEntries.clear();
            transforms.clear();
        }
    }

    record RenderEntry(ItemStack itemStack, ResolvedTransform transform, boolean backGun) {
    }

    record ResolvedTransform(Vector3f position, Vector3f scale, Quaternionf rotation) {
    }

    private record CachedRenderEntries(BodyGunDisplayData source, List<RenderEntry> entries) {
    }

    private static final class LocalDisplayCache {
        private WeakReference<Player> playerReference = new WeakReference<>(null);
        private int inventoryRevision = Integer.MIN_VALUE;
        private int selectedSlot = -1;
        private BodyGunDisplayData displayData = BodyGunDisplayData.EMPTY;

        private BodyGunDisplayData get(Player player) {
            Inventory inventory = player.getInventory();
            int currentRevision = inventory.getTimesChanged();
            if (playerReference.get() == player
                    && inventoryRevision == currentRevision
                    && selectedSlot == inventory.selected) {
                return displayData;
            }
            playerReference = new WeakReference<>(player);
            inventoryRevision = currentRevision;
            selectedSlot = inventory.selected;
            if (!displayData.matchesInventory(inventory)) {
                displayData = BodyGunDisplayData.fromInventory(inventory);
            }
            return displayData;
        }
    }
}
