package com.chloespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class InstanceManager {
    private HashMap<Integer,PlayerApartment> apartmentInstances = new HashMap<>();
    private Stack<Integer> availableIDs = new Stack<>();
    private HashMap<String,Integer> instanceIDLookup = new HashMap<>();
    private final Config config;
    private final File apartmentsFolder;
    private final ApartmentUtil apartmentUtil;
    private final FileManager fileManager;
    private final ApartmentSetupCache apartmentSetupCache;
    private final WorldGuardManager worldGuardManager;

    public InstanceManager(Config newConfig, File newApartmentsFolder, FileManager newFileManager, ApartmentSetupCache newApartmentSetupCache, WorldGuardManager newWorldGuardManager) {
        config = newConfig;
        apartmentsFolder = newApartmentsFolder;
        apartmentUtil = new ApartmentUtil(config,this);
        fileManager = newFileManager;
        apartmentSetupCache = newApartmentSetupCache;
        worldGuardManager = newWorldGuardManager;

        for (int i = 0; i < config.getInstanceCount(); i++) {
            availableIDs.push(i);
        }
    }

    public HashMap<Integer,PlayerApartment> getApartmentInstances() {
        return apartmentInstances;
    }

    public PlayerApartment getApartment(UUID playerID, String apartmentName) {
        Integer instanceID = instanceIDLookup.get(playerID.toString() + "+" + apartmentName);
        if (instanceID != null) {
            return apartmentInstances.get(instanceID);
        } else {
            return null;
        }
    }

    public PlayerApartment getApartment(int instanceID) {
        return apartmentInstances.get(instanceID);
    }

    private void saveApartmentEntities(Structure apartmentStructure, PlayerApartment playerApartment) {
        apartmentStructure.getEntities().stream()
                .map(Entity::getUniqueId)
                .forEach(playerApartment::addToContainedEntities);
    }


    private void loadApartment(Structure apartmentStructure, ApartmentSetup apartmentSetup, Integer instanceID) {
        Location apartmentLoadLocation = apartmentSetup.getRegion().getMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        apartmentStructure.place(apartmentLoadLocation,false, StructureRotation.NONE, Mirror.NONE,0,1.0F, new Random()); // temp set to false
    }

    public void validateLoadedEntities(UUID ignoredEntity) {
        List<Entity> entities = config.getApartmentWorld().getEntities();

        for (int i = 0; i < apartmentInstances.size(); i++) {
            PlayerApartment playerApartment = (PlayerApartment) apartmentInstances.values().toArray()[i];
            playerApartment.resetEntityCount();
        }

        for (Entity entity : entities) {
            Location location = entity.getLocation();
            PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(location);

            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (player.hasPermission("apartments.mannage")) continue;
                if ((ignoredEntity != null) && player.getUniqueId().equals(ignoredEntity)) continue;
                File playerDataFile = fileManager.getPlayerDataFile(player.getUniqueId());
                YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
                String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
                String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

                if (lastEnteredApartmentOwner != null && lastEnteredApartmentType != null) {

                    if (playerApartment != null && playerApartment.getOwner().equals(UUID.fromString(lastEnteredApartmentOwner))) return;

                    PlayerApartment lastApartment = getApartment(UUID.fromString(lastEnteredApartmentOwner),lastEnteredApartmentType);
                    if (lastApartment != null) {
                        lastApartment.teleport(player);
                        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
                        player.sendRichMessage("<red>You may not travel to other's apartments!");
                    } else {
                        ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(lastEnteredApartmentType);
                        Location exitTeleport = apartmentSetup.getExitTeleport();
                        player.teleport(exitTeleport);
                        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
                        player.sendRichMessage("<red>You may not travel to other's apartments!");
                    }

                } else {
                    Location spawnLocation = config.getMainWorld().getSpawnLocation();
                    player.teleport(spawnLocation);
                    player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
                    player.sendRichMessage("<red>You may not travel outside of your apartment region!");
                }

            } else if (playerApartment == null) {
                entity.remove();
            } else {
                Set<String> scoreboardTags = entity.getScoreboardTags();
                boolean isApartmentEntity = scoreboardTags.contains("apartmentEntity+owner=" + playerApartment.getOwner());
                if (!isApartmentEntity) {
                    entity.remove();
                    continue;
                }
                int entityCount = playerApartment.countEntity();
                if (entityCount > config.getMaxEntitiesPerInstance()) {
                    entity.remove();
                    playerApartment.warnPlayers("Entity limit of reached unexpectedly! Purged entities!");
                }
            }

        }
    }


    public boolean prepareInstance(UUID apartmentOwner, ApartmentSetup apartmentSetup) {
        if (availableIDs.isEmpty()) {
            return false;
        }

        String apartmentType = apartmentSetup.getName();
        int instanceID = availableIDs.pop();
        PlayerApartment playerApartment = new PlayerApartment(instanceID,apartmentOwner,apartmentSetup);

        worldGuardManager.updateBuilders(playerApartment);

        File defaultApartment = new File(apartmentsFolder,apartmentType + "/defaultApartment.nbt");
        File userApartmentFile = new File(apartmentsFolder, apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure;

        try {
            apartmentStructure = structureManager.loadStructure(userApartmentFile);
            saveApartmentEntities(apartmentStructure,playerApartment);
            loadApartment(apartmentStructure,apartmentSetup,instanceID);
        } catch (IOException e) {
            try {
                apartmentStructure = structureManager.loadStructure(defaultApartment);
                saveApartmentEntities(apartmentStructure,playerApartment);
                loadApartment(apartmentStructure,apartmentSetup,instanceID);
            } catch (IOException ex) {
                return false;
            }
        }

        apartmentInstances.put(instanceID,playerApartment);
        instanceIDLookup.put(apartmentOwner.toString() + "+" + apartmentType,instanceID);

        return true;
    }

    public void saveInstance(PlayerApartment playerApartment) {

        Integer instanceID = playerApartment.getInstanceID();

        UUID apartmentOwner = playerApartment.getOwner();
        ApartmentSetup apartmentSetup = playerApartment.getApartmentSetup();
        String apartmentType = apartmentSetup.getName();

        Region region = apartmentSetup.getRegion();
        Location regionMin = region.getMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        Location regionMax = region.getMax().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));

        File userApartmentFileTemp = new File(apartmentsFolder,apartmentType + "/userApartments/" + apartmentOwner + "_temp.nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure = structureManager.createStructure();
        apartmentStructure.fill(regionMin,regionMax,false); // temp set to false

        try {
            structureManager.saveStructure(userApartmentFileTemp,apartmentStructure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (userApartmentFileTemp.length() > 1048576) {
            try {
                Files.delete(userApartmentFileTemp.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            playerApartment.warnPlayers("File size above 1mb. Refused to save apartment!");
            return;
        }

        File userApartmentFile = new File(apartmentsFolder,apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        try {
            Files.copy(userApartmentFileTemp.toPath(),userApartmentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(userApartmentFileTemp.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeInstance(PlayerApartment playerApartment) {

        Integer instanceID = playerApartment.getInstanceID();

        UUID apartmentOwner = playerApartment.getOwner();
        ApartmentSetup apartmentSetup = playerApartment.getApartmentSetup();
        String apartmentType = apartmentSetup.getName();

        saveInstance(playerApartment);

        availableIDs.push(instanceID);
        apartmentInstances.remove(instanceID);
        instanceIDLookup.remove(apartmentOwner.toString() + "+" + apartmentType);

        HashSet<UUID> containedPlayers = playerApartment.getContainedPlayers();

        for (UUID playerID : containedPlayers) {
            Player player = Bukkit.getPlayer(playerID);
            if (player.isOnline()) {
                player.teleport(playerApartment.getApartmentSetup().getExitTeleport());
                player.sendRichMessage("<red>Apartment was closed");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
            }
        }

        File defaultApartment = new File(apartmentsFolder,apartmentType + "/defaultApartment.nbt");

        StructureManager structureManager = Bukkit.getStructureManager();

        try {
            Structure defaultApartmentStructure = structureManager.loadStructure(defaultApartment);
            loadApartment(defaultApartmentStructure,apartmentSetup,instanceID);
        } catch (IOException ex) {
            return;
        }

    }

    public void updateInstance(PlayerApartment playerApartment) {
        HashSet<UUID> containedPlayers = playerApartment.getContainedPlayers();
        if (containedPlayers.isEmpty()) {
            closeInstance(playerApartment);
        }
    }
}
