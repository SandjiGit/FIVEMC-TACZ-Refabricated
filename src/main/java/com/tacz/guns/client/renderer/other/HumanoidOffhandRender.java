package com.tacz.guns.client.renderer.other;

import cn.sh1rocu.tacz.api.event.RenderLivingEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class HumanoidOffhandRender {
    private static final LayerGunShow BACK_SHOW = new LayerGunShow(
            new Vector3f(-1, 20, 3),
            new Vector3f(-180, 0, 125),
            new Vector3f(0.5f, 0.5f, 0.5f));
    private static final LayerGunShow BELT_SHOW = new LayerGunShow(
            new Vector3f(-5, 13, -1),
            new Vector3f(-90, -35, 90),
            new Vector3f(0.5f, 0.5f, 0.5f));

    public static void renderGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        renderOffhandGun(entity, matrixStack, buffer, packedLight);
    }

    public static void renderInventoryGuns(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        renderGun(entity, poseStack, buffer, packedLight);
        renderStoredGuns(entity, poseStack, buffer, packedLight);
    }

    private static void renderOffhandGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        ItemStack itemStack = entity.getOffhandItem();
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(itemStack).ifPresent(index -> {
            LayerGunShow offhandShow = index.getOffhandShow();
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, offhandShow);
        });
    }

    private static void renderStoredGuns(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (i == inventory.selected) {
                continue;
            }
            renderStoredGun(entity, matrixStack, buffer, packedLight, inventory.items.get(i), i);
        }
    }

    private static void renderStoredGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, int inventoryIndex) {
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        if (!iGun.isBodyGunVisible(itemStack)) {
            return;
        }
        TimelessAPI.getGunDisplay(itemStack).ifPresent(display -> {
            boolean pistol = isPistol(itemStack, iGun);
            LayerGunShow gunShow = resolveStoredGunShow(display.getHotbarShow(), inventoryIndex, pistol);
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, gunShow, !pistol && entity.isCrouching());
        });
    }

    private static LayerGunShow resolveStoredGunShow(@Nullable Map<Integer, LayerGunShow> hotbarShow, int inventoryIndex, boolean pistol) {
        if (hotbarShow != null && !hotbarShow.isEmpty()) {
            LayerGunShow slotShow = hotbarShow.get(inventoryIndex);
            if (slotShow != null) {
                return slotShow;
            }
            LayerGunShow defaultShow = hotbarShow.get(0);
            if (defaultShow != null) {
                return defaultShow;
            }
            return hotbarShow.values().iterator().next();
        }
        return pistol ? BELT_SHOW : BACK_SHOW;
    }

    private static boolean isPistol(ItemStack itemStack, IGun iGun) {
        return TimelessAPI.getClientGunIndex(iGun.getGunId(itemStack))
                .map(index -> "pistol".equals(index.getType()))
                .orElse(false);
    }

    private static void renderGunItem(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, LayerGunShow offhandShow) {
        renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, offhandShow, false);
    }

    private static void renderGunItem(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, LayerGunShow offhandShow, boolean crouchingBackGun) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        Vector3f pos = offhandShow.getPos();
        Vector3f rotate = offhandShow.getRotate();
        Vector3f scale = offhandShow.getScale();
        matrixStack.pushPose();
        matrixStack.translate(-pos.x() / 16f, 1.5 - pos.y() / 16f, pos.z() / 16f);
        if (crouchingBackGun) {
            matrixStack.translate(0, 0.2, 0.15);
            matrixStack.mulPose(Axis.XP.rotationDegrees(25));
        }
        matrixStack.scale(-scale.x(), -scale.y(), scale.z());
        Quaternionf rotation = new Quaternionf();
        MathUtil.toQuaternion((float) Math.toRadians(rotate.x), (float) Math.toRadians(rotate.y), (float) Math.toRadians(rotate.z), rotation);
        matrixStack.mulPose(rotation);
        renderer.renderStatic(itemStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, buffer, entity.level(), entity.getId());
        matrixStack.popPose();
    }
}
