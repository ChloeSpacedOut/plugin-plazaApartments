package com.chloespacedout.PlazaApartments.commands;

import com.chloespacedout.PlazaApartments.*;
import com.chloespacedout.PlazaApartments.arguments.ApartmentArgument;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Apartment {

    private static void deleteKey(String keyID, ConfigurationSection apartmentKeysSection, YamlConfiguration apartmentKeys) {
        apartmentKeysSection.set(keyID,null);
        apartmentKeys.set(keyID, null);
    }

    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName, Core pluginInstance, ApartmentSetupCache apartmentSetupCache, InstanceManager instanceManager, FileManager fileManager, Config config) {
        return Commands.literal(commandName)
                .requires(sender -> sender.getExecutor() instanceof Player && sender.getSender().hasPermission("apartments.use"))
                .then(Commands.argument("apartment",new ApartmentArgument(apartmentSetupCache))
                        .then(Commands.literal("close")
                                .executes(ctx -> {
                                    final String apartmentType = StringArgumentType.getString(ctx,"apartment");
                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                    final UUID commandSenderID = commandSender.getUniqueId();

                                    PlayerApartment playerApartment = instanceManager.getApartment(commandSenderID,apartmentType);

                                    if (playerApartment != null) {
                                        instanceManager.closeInstance(playerApartment);
                                    } else {
                                        commandSender.sendRichMessage("<red>Your apartment is not currently occupied");
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                    commandSender.sendRichMessage("<gray>Confirm resetting apartment to default by adding \"confirm\"");
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> {
                                            final String apartmentType = StringArgumentType.getString(ctx,"apartment");
                                            final Player commandSender = (Player) ctx.getSource().getSender();
                                            final UUID commandSenderID = commandSender.getUniqueId();
                                            PlayerApartment playerApartment = instanceManager.getApartment(commandSenderID,apartmentType);

                                            if (playerApartment != null) {
                                                instanceManager.closeInstance(playerApartment);
                                            }

                                            fileManager.deleteApartmentFile(commandSenderID.toString(),apartmentType);

                                            commandSender.sendRichMessage("<red>Apartment reset to default");
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("changeLocks")
                                .executes(ctx -> {
                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                    commandSender.sendRichMessage("<gray>Confirm permanently changing locks by adding \"confirm\"");
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> {
                                            final Player commandSender = (Player) ctx.getSource().getSender();
                                            File playerDataFile = fileManager.getPlayerDataFile(commandSender.getUniqueId());
                                            YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);

                                            File apartmentKeysFile = fileManager.getApartmentKeysFile();
                                            YamlConfiguration apartmentKeys = YamlConfiguration.loadConfiguration(apartmentKeysFile);

                                            String[] categories = new String[]{"publicKeys","privateKeys"};

                                            for (String catagory : categories) {
                                                HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();
                                                for (int i = 0; i < apartmentSetups.size(); i++) {
                                                    ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
                                                    String apartmentKeysPath = catagory + "." + apartmentSetup.getName();
                                                    ConfigurationSection apartmentKeysSection = playerData.getConfigurationSection(apartmentKeysPath);
                                                    apartmentKeysSection.getKeys(false)
                                                            .forEach(keyID -> deleteKey(keyID,apartmentKeysSection,apartmentKeys));

                                                    try {
                                                        playerData.save(playerDataFile);
                                                        apartmentKeys.save(apartmentKeysFile);
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                            }

                                            return Command.SINGLE_SUCCESS;
                                        })

                                )
                        )
                        .then(Commands.literal("mintKey")
                                .requires(sender -> sender.getExecutor() instanceof Player && !sender.getExecutor().getWorld().equals(config.getApartmentWorld()))
                                .then(Commands.literal("public")
                                        .executes(ctx -> {
                                            final Player commandSender = (Player) ctx.getSource().getSender();
                                            PlayerInventory inventory = commandSender.getInventory();
                                            String apartment = StringArgumentType.getString(ctx,"apartment");
                                            KeyMaker keyMaker = new KeyMaker();
                                            ItemStack keyItem = keyMaker.makeKey(pluginInstance,fileManager,commandSender,apartment,null,false,1);

                                            commandSender.playSound(commandSender.getLocation(),Sound.ENTITY_ITEM_PICKUP,1.0F,1.0F);
                                            inventory.addItem(keyItem);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> {
                                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                                    int amount = IntegerArgumentType.getInteger(ctx,"amount");
                                                    PlayerInventory inventory = commandSender.getInventory();
                                                    String apartment = StringArgumentType.getString(ctx,"apartment");
                                                    KeyMaker keyMaker = new KeyMaker();
                                                    ItemStack keyItem = keyMaker.makeKey(pluginInstance,fileManager,commandSender,apartment,null,false,amount);

                                                    commandSender.playSound(commandSender.getLocation(),Sound.ENTITY_ITEM_PICKUP,1.0F,1.0F);
                                                    inventory.addItem(keyItem);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("private")
                                        .then(Commands.argument("target", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                                    PlayerInventory inventory = commandSender.getInventory();
                                                    final PlayerSelectorArgumentResolver playerSelector = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                                                    final Player targetPlayer = playerSelector.resolve(ctx.getSource()).getFirst();
                                                    String apartment = StringArgumentType.getString(ctx,"apartment");
                                                    KeyMaker keyMaker = new KeyMaker();
                                                    ItemStack keyItem = keyMaker.makeKey(pluginInstance,fileManager,commandSender,apartment,targetPlayer,false,1);

                                                    commandSender.playSound(commandSender.getLocation(),Sound.ENTITY_ITEM_PICKUP,1.0F,1.0F);
                                                    inventory.addItem(keyItem);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("builder")
                                        .then(Commands.argument("target", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    final Player commandSender = (Player) ctx.getSource().getSender();
                                                    PlayerInventory inventory = commandSender.getInventory();
                                                    final PlayerSelectorArgumentResolver playerSelector = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                                                    final Player targetPlayer = playerSelector.resolve(ctx.getSource()).getFirst();
                                                    String apartment = StringArgumentType.getString(ctx,"apartment");
                                                    KeyMaker keyMaker = new KeyMaker();
                                                    ItemStack keyItem = keyMaker.makeKey(pluginInstance,fileManager,commandSender,apartment,targetPlayer,true,1);

                                                    commandSender.playSound(commandSender.getLocation(),Sound.ENTITY_ITEM_PICKUP,1.0F,1.0F);
                                                    inventory.addItem(keyItem);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )
                .build();
    }
}
