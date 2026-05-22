package com.tacz.guns.client.input;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.gameplay.LocalPlayerSprint;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.compat.controllable.ControllableCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Environment(EnvType.CLIENT)
public class ShootKey {
    private static final int SHOOT_BUTTON = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    private static boolean lastTimeShootSuccess = false;
    private static boolean controllerShootDown = false;

    public static void autoShoot(Minecraft mc, boolean isPhaseEnd) {
        if (!isPhaseEnd || !isInGame()) {
            return;
        }
        LocalPlayerSprint.stopSprint = false;

        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isBurstAuto = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                    .map(index -> index.getGunData().getBurstData().isContinuousShoot())
                    .orElse(false);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            boolean isShootDown = isShootButtonDown(mc) || controllerShootDown;
            if (operator.chargeShoot(isShootDown)) {
                LocalPlayerSprint.stopSprint = true;
                if (fireMode != FireMode.AUTO && !isBurstAuto && lastTimeShootSuccess) {
                    // 非全自动情况，禁止连续开火
                    return;
                }
                if (operator.shoot() == ShootResult.SUCCESS) {
                    lastTimeShootSuccess = true;
                    ControllableCompat.onGunShoot(mainHandItem, fireMode);
                }
            }
            if (isShootDown) {
                LocalPlayerSprint.stopSprint = true;
            } else {
                lastTimeShootSuccess = false;
                SoundPlayManager.resetDryFireSound();
            }
        }
    }

    private static boolean isShootButtonDown(Minecraft mc) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), SHOOT_BUTTON) == GLFW.GLFW_PRESS;
    }

    public static boolean shootControllerTick(boolean isShootDown) {
        controllerShootDown = isShootDown;
        return false;
    }
}
