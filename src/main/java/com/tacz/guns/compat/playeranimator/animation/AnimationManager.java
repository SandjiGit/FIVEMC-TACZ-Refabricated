package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.AnimationName;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

public class AnimationManager {
    public static boolean hasPlayerAnimator3rd(GunDisplayInstance display) {
        ResourceLocation location = display.getPlayerAnimator3rd();
        if (location == null) {
            return false;
        }
        return PlayerAnimatorAssetManager.get().containsKey(location);
    }

    public static boolean isFlying(AbstractClientPlayer player) {
        return !player.onGround() && player.getAbilities().flying;
    }

    public static void playRotationAnimation(AbstractClientPlayer player, GunDisplayInstance display) {
        String animationName = AnimationName.EMPTY;
        ResourceLocation dataId = PlayerAnimatorCompat.ROTATION_ANIMATION;
        ResourceLocation animator3rd = display.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.get().containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    public static void playLowerAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        // 如果玩家骑乘
        if (player.getVehicle() != null) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RIDE_LOWER);
            return;
        }
        // 如果玩家在天上，下半身动画就是站立动画
        if (isFlying(player)) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
            return;
        }
        if (player.isSprinting()) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RUN_LOWER);
            }
            return;
        }
        if (limbSwingAmount > 0.05) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.WALK_LOWER);
            }
            return;
        }
        if (player.getPose() == Pose.CROUCHING) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_LOWER);
        } else {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
        }
    }

    public static void playLoopUpperAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            // 疾跑时播放的动画
            if (!isFlying(player) && player.isSprinting()) {
                if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.RUN_UPPER);
                }
                return;
            }

            // 行走时的动画
            if (!isFlying(player) && limbSwingAmount > 0.05) {
                if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.WALK_UPPER);
                }
                return;
            }

            playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.HOLD_UPPER);
        } else {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.AIM_UPPER);
        }
    }

    @SuppressWarnings("unchecked")
    public static void playLoopAnimation(AbstractClientPlayer player, GunDisplayInstance display, ResourceLocation dataId, String animationName) {
        ResourceLocation animator3rd = display.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.get().containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            if (modifierLayer.getAnimation() instanceof KeyframeAnimationPlayer animationPlayer && animationPlayer.isActive()) {
                Object extraDataName = animationPlayer.getData().extraData.get("name");
                if (extraDataName instanceof String name && !animationName.equals(name)) {
                    AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                    modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
                }
                return;
            }
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    @SuppressWarnings("unchecked")
    public static void playOnceAnimation(AbstractClientPlayer player, GunDisplayInstance display, ResourceLocation dataId, String animationName) {
        ResourceLocation animator3rd = display.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.get().containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            IAnimation animation = modifierLayer.getAnimation();
            if (animation == null || !animation.isActive()) {
                AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
            }
        });
    }

    public static void stopAllAnimation(AbstractClientPlayer player) {
        stopAllAnimation(player, 8);
    }

    public static void stopAllAnimation(AbstractClientPlayer player, int fadeTime) {
        stopAnimation(player, PlayerAnimatorCompat.LOWER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.ROTATION_ANIMATION, fadeTime);
    }


    @SuppressWarnings("unchecked")
    private static void stopAnimation(AbstractClientPlayer player, ResourceLocation dataId, int fadeTime) {
        var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
        if (modifierLayer != null && modifierLayer.isActive()) {
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(fadeTime, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, null);
        }
    }

    public void onFire(GunShootEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(index -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(player);
            float aimingProgress = operator.getSynAimingProgress();
            if (aimingProgress <= 0) {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.NORMAL_FIRE_UPPER);
            } else {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.AIM_FIRE_UPPER);
            }
        });
    }

    public void onReload(GunReloadEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getEntity();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(
                index -> playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.RELOAD_UPPER)
        );
    }

    public void onMelee(GunMeleeEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        int randomIndex = shooter.getRandom().nextInt(3);
        String animationName = switch (randomIndex) {
            case 0 -> AnimationName.MELEE_UPPER;
            case 1 -> AnimationName.MELEE_2_UPPER;
            default -> AnimationName.MELEE_3_UPPER;
        };
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(
                index -> playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, animationName)
        );
    }

    public void onDraw(GunDrawEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack currentGunItem = event.getCurrentGunItem();
        ItemStack previousGunItem = event.getPreviousGunItem();
        // 在切枪时，重置上半身动画
        if (currentGunItem.getItem() instanceof IGun && previousGunItem.getItem() instanceof IGun) {
            stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, 8);
            stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, 8);
            stopAnimation(player, PlayerAnimatorCompat.LOWER_ANIMATION, 8);
        }
    }
}
