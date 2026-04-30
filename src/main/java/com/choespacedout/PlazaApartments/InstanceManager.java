package com.choespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InstanceManager {
    private HashMap<Integer,PlayerApartment> apartmentInstances = new HashMap<>();
    private Stack<Integer> availableIDs = new Stack<>();
    private HashMap<UUID,Integer> instanceIDLookup = new HashMap<>();
    private final int instanceCount;
    private final File apartmentFolder;

    public InstanceManager(int newInstanceCount, File newApartmentFolder) {
        instanceCount = newInstanceCount;
        apartmentFolder = newApartmentFolder;

        for (int i = 0; i < instanceCount; i++) {
            availableIDs.push(i);
        }
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


    private void loadApartment(Structure apartmentStructure, ApartmentSetup apartmentSetup, Integer instanceID) {
        Location apartmentLoadLocation = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        apartmentStructure.place(apartmentLoadLocation,false, StructureRotation.NONE, Mirror.NONE,0,1.0F, new Random());
    }

    public boolean prepareInstance(UUID apartmentOwner, ApartmentSetup apartmentSetup) {
        if (availableIDs.isEmpty()) {
            return false;
        }
        String apartmentType = apartmentSetup.getName();
        int instanceID = availableIDs.pop();
        PlayerApartment playerApartment = new PlayerApartment(instanceID,apartmentOwner,apartmentSetup);
        apartmentInstances.put(instanceID,playerApartment);
        instanceIDLookup.put(apartmentOwner,instanceID);

        File defaultApartment = new File(apartmentFolder,apartmentType + "/defaultApartment.nbt");
        File userApartmentFile = new File(apartmentFolder, apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure;

        try {
            apartmentStructure = structureManager.loadStructure(userApartmentFile);
            loadApartment(apartmentStructure,apartmentSetup,instanceID);
        } catch (IOException e) {
            try {
                apartmentStructure = structureManager.loadStructure(defaultApartment);
                loadApartment(apartmentStructure,apartmentSetup,instanceID);
            } catch (IOException ex) {
                return false;
            }
        }

        return true;
    }

    public void closeInstance(PlayerApartment playerApartment) {

        Integer instanceID = playerApartment.getInstanceID();

        UUID apartmentOwner = playerApartment.getOwner();
        ApartmentSetup apartmentSetup = playerApartment.getApartmentSetup();
        String apartmentType = apartmentSetup.getName();
        Location regionMin = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        Location regionMax = apartmentSetup.getRegionMax().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));

        File userApartmentFile = new File(apartmentFolder,apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure = structureManager.createStructure();
        apartmentStructure.fill(regionMin,regionMax,false);

        try {
            structureManager.saveStructure(userApartmentFile,apartmentStructure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        availableIDs.push(instanceID);
        apartmentInstances.remove(instanceID);
        instanceIDLookup.remove(apartmentOwner);

        List<UUID> containedPlayers = playerApartment.getContainedPlayers();

        for (UUID playerID : containedPlayers) {
            Player player = Bukkit.getPlayer(playerID);
            if (player.isOnline()) {
                player.teleport(playerApartment.getApartmentSetup().getExitTeleport());
                player.sendRichMessage("<red>Apartment was closed");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
            }
        }

        File defaultApartment = new File(apartmentFolder,apartmentType + "/defaultApartment.nbt");

        try {
            Structure defaultApartmentStructure = structureManager.loadStructure(defaultApartment);
            loadApartment(defaultApartmentStructure,apartmentSetup,instanceID);
        } catch (IOException ex) {
            return;
        }

    }

    public void updateInstance(PlayerApartment playerApartment) {
        List<UUID> containedPlayers = playerApartment.getContainedPlayers();
        if (containedPlayers.isEmpty()) {
            closeInstance(playerApartment);
        }
    }
}
