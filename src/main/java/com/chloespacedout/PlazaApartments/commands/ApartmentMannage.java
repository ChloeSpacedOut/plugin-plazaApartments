package com.chloespacedout.PlazaApartments.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class ApartmentMannage {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
                .then(Commands.literal("maintenanceMode")
                        .then(Commands.literal("on"))
                        .then(Commands.literal("off"))
                )
                .then(Commands.literal("kick")
                        .then(Commands.literal("PLAYER")))
                .then(Commands.literal("kickAll"))
                .build();
    }
}
