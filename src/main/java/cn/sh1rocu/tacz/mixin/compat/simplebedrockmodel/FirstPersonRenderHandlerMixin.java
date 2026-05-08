package cn.sh1rocu.tacz.mixin.compat.simplebedrockmodel;

import cn.sh1rocu.simplebedrockmodel.api.event.RenderHandEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPGeoItemRenderer;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.renderer.other.HandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonRenderHandler.class)
public class FirstPersonRenderHandlerMixin {
    // FIXME: 跟FirstPersonRenderEvent#onRenderHand一样的暴力解决方式（来自MUKSC的tacz-neoforge），已知会导致无法兼容加速渲染
    @WrapWithCondition(remap = false, method = "onRenderHand(Lcn/sh1rocu/simplebedrockmodel/api/event/RenderHandEvent;)V", at = @At(value = "INVOKE", target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v1/client/renderer/IFPGeoItemRenderer;renderFirstPerson(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"))
    private static boolean tacz$onRenderHand(IFPGeoItemRenderer instance, LocalPlayer player, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float v, @Local(argsOnly = true) RenderHandEvent event) {
        if (instance instanceof AnimateGeoItemRenderer<?, ?>) {
            ItemDisplayContext transformType;
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                transformType = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            } else {
                transformType = ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            }
            GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
            HandRenderer.INSTANCE.renderSolid((poseStack1) -> {
                instance.renderFirstPerson(
                        player, itemStack, transformType,
                        poseStack1 == null ? event.getPoseStack() : poseStack1,
                        event.getMultiBufferSource(),
                        event.getPackedLight(),
                        event.getPartialTick()
                );
            }, event.getPartialTick(), gameRenderer.getMainCamera(), gameRenderer);
            return false;
        }
        return true;
    }
}
