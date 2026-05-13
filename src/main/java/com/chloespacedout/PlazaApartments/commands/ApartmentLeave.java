package com.chloespacedout.PlazaApartments.commands;

import com.chloespacedout.PlazaApartments.ApartmentUtil;
import com.chloespacedout.PlazaApartments.PlayerApartment;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ApartmentLeave {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName, ApartmentUtil apartmentUtil) {
        return Commands.literal(commandName)
                .executes(ctx -> {
                    final Player commandSender = (Player) ctx.getSource().getSender();
                    if (!(commandSender instanceof Player)) {
                        return Command.SINGLE_SUCCESS;
                    }

                    PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(commandSender.getLocation());

                    if (playerApartment == null) {
                        commandSender.sendRichMessage("<red>You are not in an apartment!");
                        return Command.SINGLE_SUCCESS;
                    }

                    Location exitTeleport = playerApartment.getApartmentSetup().getExitTeleport();
                    commandSender.teleport(exitTeleport);
                    commandSender.playSound(commandSender.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("target", ArgumentTypes.player())
                        .requires(sender -> sender.getSender().hasPermission("apartments.mannage"))
                        .executes(ctx -> {
                            final PlayerSelectorArgumentResolver playerSelector = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                            final Player targetPlayer = playerSelector.resolve(ctx.getSource()).getFirst();
                            PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(targetPlayer.getLocation());
                            if (playerApartment != null) {
                                Location exitTeleport = playerApartment.getApartmentSetup().getExitTeleport();
                                targetPlayer.teleport(exitTeleport);
                                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )

                .build();
    }
}
