package com.tacz.guns.client.renderer.other;

import cn.sh1rocu.tacz.api.event.RenderLivingEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class HumanoidOffhandRender {
    private static final BodyGunRenderCache BODY_GUN_CACHE = new BodyGunRenderCache();
    private static final Quaternionf CROUCHING_BACK_ROTATION = Axis.XP.rotationDegrees(25);

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
        LayerGunShow offhandShow = TimelessAPI.getGunDisplay(itemStack)
                .map(display -> display.getOffhandShow())
                .orElse(null);
        if (offhandShow != null) {
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, BODY_GUN_CACHE.resolveTransform(offhandShow), false);
        }
    }

    private static void renderStoredGuns(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        if (!(entity instanceof Player player)) {
            return;
        }
        List<BodyGunRenderCache.RenderEntry> entries = BODY_GUN_CACHE.getRenderEntries(player);
        boolean crouching = player.isCrouching();
        for (BodyGunRenderCache.RenderEntry entry : entries) {
            renderGunItem(player, matrixStack, buffer, packedLight, entry.itemStack(), entry.transform(), crouching && entry.backGun());
        }
    }

    private static void renderGunItem(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, BodyGunRenderCache.ResolvedTransform transform, boolean crouchingBackGun) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        Vector3f pos = transform.position();
        Vector3f scale = transform.scale();
        matrixStack.pushPose();
        matrixStack.translate(-pos.x() / 16f, 1.5 - pos.y() / 16f, pos.z() / 16f);
        if (crouchingBackGun) {
            matrixStack.translate(0, 0.2, 0.15);
            matrixStack.mulPose(CROUCHING_BACK_ROTATION);
        }
        matrixStack.scale(-scale.x(), -scale.y(), scale.z());
        matrixStack.mulPose(transform.rotation());
        renderer.renderStatic(itemStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, buffer, entity.level(), entity.getId());
        matrixStack.popPose();
    }
}
