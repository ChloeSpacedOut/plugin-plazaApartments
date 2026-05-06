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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InstanceManager {
    private static final Logger log = LoggerFactory.getLogger(InstanceManager.class);
    private HashMap<Integer,PlayerApartment> apartmentInstances = new HashMap<>();
    private Stack<Integer> availableIDs = new Stack<>();
    private HashMap<UUID,Integer> instanceIDLookup = new HashMap<>();
    private final Config config;
    private final File apartmentFolder;
    private final ApartmentUtil apartmentUtil;
    private final FileUtil fileUtil;
    private final ApartmentSetupCache apartmentSetupCache;

    public InstanceManager(Config newConfig, File newApartmentFolder, FileUtil newFileUtil, ApartmentSetupCache newApartmentSetupCache) {
        config = newConfig;
        apartmentFolder = newApartmentFolder;
        apartmentUtil = new ApartmentUtil(config,this);
        fileUtil = newFileUtil;
        apartmentSetupCache = newApartmentSetupCache;

        for (int i = 0; i < config.getInstanceCount(); i++) {
            availableIDs.push(i);
        }
    }

    public HashMap<Integer,PlayerApartment> getApartmentInstances() {
        return apartmentInstances;
    }

    public PlayerApartment getApartment(UUID playerID) {
        Integer instanceID = instanceIDLookup.get(playerID);
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
        Location apartmentLoadLocation = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        apartmentStructure.place(apartmentLoadLocation,true, StructureRotation.NONE, Mirror.NONE,0,1.0F, new Random());
    }

    public void validateLoadedEntities(UUID ignoredEntity) {
        List<Entity> entities = config.getApartmentWorld().getEntities();

        for (Entity entity : entities) {
            Location location = entity.getLocation();
            PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(location);

            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (player.hasPermission("apartments.mannage")) continue;
                if ((ignoredEntity != null) && player.getUniqueId().equals(ignoredEntity)) continue;
                File playerDataFile = fileUtil.getPlayerDataFile(player.getUniqueId());
                YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
                String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
                String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

                if (lastEnteredApartmentOwner != null && lastEnteredApartmentType != null) {

                    if (playerApartment != null && playerApartment.getOwner().equals(UUID.fromString(lastEnteredApartmentOwner))) return;

                    PlayerApartment lastApartment = getApartment(UUID.fromString(lastEnteredApartmentOwner));
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

        File defaultApartment = new File(apartmentFolder,apartmentType + "/defaultApartment.nbt");
        File userApartmentFile = new File(apartmentFolder, apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
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
        instanceIDLookup.put(apartmentOwner,instanceID);

        return true;
    }

    public void saveInstance(PlayerApartment playerApartment) {

        Integer instanceID = playerApartment.getInstanceID();

        UUID apartmentOwner = playerApartment.getOwner();
        ApartmentSetup apartmentSetup = playerApartment.getApartmentSetup();
        String apartmentType = apartmentSetup.getName();
        Location regionMin = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        Location regionMax = apartmentSetup.getRegionMax().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));

        File userApartmentFile = new File(apartmentFolder,apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure = structureManager.createStructure();
        apartmentStructure.fill(regionMin,regionMax,true);

        try {
            structureManager.saveStructure(userApartmentFile,apartmentStructure);
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
        instanceIDLookup.remove(apartmentOwner);

        HashSet<UUID> containedPlayers = playerApartment.getContainedPlayers();

        for (UUID playerID : containedPlayers) {
            Player player = Bukkit.getPlayer(playerID);
            if (player.isOnline()) {
                player.teleport(playerApartment.getApartmentSetup().getExitTeleport());
                player.sendRichMessage("<red>Apartment was closed");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
            }
        }

        File defaultApartment = new File(apartmentFolder,apartmentType + "/defaultApartment.nbt");

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
