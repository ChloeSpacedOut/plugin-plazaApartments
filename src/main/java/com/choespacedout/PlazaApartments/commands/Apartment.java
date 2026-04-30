package com.choespacedout.PlazaApartments.commands;

import com.choespacedout.PlazaApartments.ApartmentSetup;
import com.choespacedout.PlazaApartments.ApartmentSetupCache;
import com.choespacedout.PlazaApartments.InstanceManager;
import com.choespacedout.PlazaApartments.PlayerApartment;
import com.choespacedout.PlazaApartments.arguments.ApartmentArgument;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Apartment {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName, ApartmentSetupCache apartmentSetupCache, InstanceManager instanceManager) {
        return Commands.literal(commandName)
                .then(Commands.argument("apartment",new ApartmentArgument(apartmentSetupCache))
                        .then(Commands.literal("enter")
                                .executes(ctx -> {
                                    final String apartmentType = StringArgumentType.getString(ctx,"apartment");
                                    final ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(apartmentType);
                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                    final UUID commandSenderID = commandSender.getUniqueId();
                                    PlayerApartment playerApartment = instanceManager.getApartment(commandSenderID);

                                    if (playerApartment != null) {
                                        playerApartment.teleport(commandSender);
                                    } else {
                                        boolean hasPreparedInstance = instanceManager.prepareInstance(commandSenderID,apartmentSetup);
                                        if (hasPreparedInstance) {
                                            playerApartment = instanceManager.getApartment(commandSenderID);
                                            playerApartment.teleport(commandSender);
                                        } else {
                                            // feedback here for if you succeed or fail
                                        }
                                    }

                                    // create player apartment object
                                    // add to next available slot in instance
                                    // from slot data paste apartment & teleport player
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("close")
                                .executes(ctx -> {
                                    final String apartmentType = StringArgumentType.getString(ctx,"apartment");
                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                    final UUID commandSenderID = commandSender.getUniqueId();

                                    PlayerApartment playerApartment = instanceManager.getApartment(commandSenderID);

                                    if (playerApartment != null) {
                                        instanceManager.closeInstance(playerApartment);
                                    } else {

                                    }


                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("delete")) // confirm deletion
                        .then(Commands.literal("manage")

                                .then(Commands.literal("trust")
                                        .then(Commands.literal("add"))
                                        .then(Commands.literal("remove"))
                                )
                                .then(Commands.literal("block")
                                        .then(Commands.literal("add"))
                                        .then(Commands.literal("remove"))
                                )
                        )
                )
                .build();
    }
}
