package com.choespacedout.PlazaApartments.commands;

import com.choespacedout.PlazaApartments.ApartmentSetupCache;
import com.choespacedout.PlazaApartments.InstanceManager;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class ApartmentLeave {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName, ApartmentSetupCache apartmentSetupCache, InstanceManager instanceManager) {
        return Commands.literal(commandName)
                .build();
    }
}
