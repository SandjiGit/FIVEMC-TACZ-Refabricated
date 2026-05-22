package com.tacz.guns.mixin.client;

import cn.sh1rocu.tacz.api.event.RenderLivingEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @SuppressWarnings("unchecked")
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void tacz$onRenderLivingPost(T livingEntity, float entityYaw, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        RenderLivingEvent.Post<T, M> event = new RenderLivingEvent.Post<>(
                livingEntity,
                (LivingEntityRenderer<T, M>) (Object) this,
                partialTick,
                poseStack,
                buffer,
                packedLight);
        RenderLivingEvent.POST.invoker().post(event);
    }
}
