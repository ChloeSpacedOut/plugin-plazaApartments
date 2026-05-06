package com.chloespacedout.PlazaApartments.commands;

import com.chloespacedout.PlazaApartments.ApartmentSetupCache;
import com.chloespacedout.PlazaApartments.InstanceManager;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class ApartmentLeave {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName, ApartmentSetupCache apartmentSetupCache, InstanceManager instanceManager) {
        return Commands.literal(commandName)
                .build();
    }
}
