package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tacz.guns.api.item.IGun;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class BodyGunCommand {
    private static final String BODY_GUN_NAME = "body_gun";
    private static final String SHOW_NAME = "show";
    private static final String HIDE_NAME = "hide";
    private static final SimpleCommandExceptionType GUN_REQUIRED =
            new SimpleCommandExceptionType(Component.literal("You must hold a TACZ gun in your main hand."));

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> bodyGun = Commands.literal(BODY_GUN_NAME);
        bodyGun.then(Commands.literal(SHOW_NAME).executes(BodyGunCommand::show));
        bodyGun.then(Commands.literal(HIDE_NAME).executes(BodyGunCommand::hide));
        return bodyGun;
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldGun(context.getSource());
        IGun iGun = (IGun) stack.getItem();
        iGun.setBodyGunVisible(stack, true);
        context.getSource().getPlayerOrException().getInventory().setChanged();
        context.getSource().sendSuccess(() -> Component.literal("Body gun display enabled."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int hide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldGun(context.getSource());
        IGun iGun = (IGun) stack.getItem();
        iGun.setBodyGunVisible(stack, false);
        context.getSource().getPlayerOrException().getInventory().setChanged();
        context.getSource().sendSuccess(() -> Component.literal("Body gun display disabled."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static ItemStack getHeldGun(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun)) {
            throw GUN_REQUIRED.create();
        }
        return stack;
    }
}
