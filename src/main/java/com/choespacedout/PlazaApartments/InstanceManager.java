package com.choespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

public class InstanceManager {
    private HashMap<Integer,PlayerApartment> apartmentInstances = new HashMap<>();
    private Stack<Integer> availableIDs = new Stack<>();
    private HashMap<UUID,Integer> instanceIDLookup = new HashMap<>();
    private final int instanceCount;
    private final String apartmentFolderPath;
    private ApartmentSetupCache apartmentSetupCache;

    public InstanceManager(ApartmentSetupCache newApartmentSetupCache, int newInstanceCount, String newApartmentFolderPath) {
        instanceCount = newInstanceCount;
        apartmentFolderPath = newApartmentFolderPath;
        apartmentSetupCache = newApartmentSetupCache;

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


    private void loadApartment(Structure apartmentStructure, String apartmentType, Integer instanceID) {
        ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(apartmentType);
        Location apartmentLoadLocation = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        apartmentStructure.place(apartmentLoadLocation,false, StructureRotation.NONE, Mirror.NONE,0,1.0F, new Random());
    }

    public boolean prepareInstance(UUID apartmentOwner, String apartmentType) {
        if (availableIDs.isEmpty()) {
            return false;
        }
        int instanceID = availableIDs.pop();
        PlayerApartment playerApartment = new PlayerApartment(instanceID,apartmentOwner,apartmentType,apartmentSetupCache);
        apartmentInstances.put(instanceID,playerApartment);
        instanceIDLookup.put(apartmentOwner,instanceID);

        File defaultApartment = new File(apartmentFolderPath + apartmentType + "/defaultApartment.nbt");
        File userApartmentFile = new File(apartmentFolderPath + apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
        StructureManager structureManager = Bukkit.getStructureManager();

        Structure apartmentStructure;

        try {
            apartmentStructure = structureManager.loadStructure(userApartmentFile);
            loadApartment(apartmentStructure,apartmentType,instanceID);
        } catch (IOException e) {
            try {
                apartmentStructure = structureManager.loadStructure(defaultApartment);
                loadApartment(apartmentStructure,apartmentType,instanceID);
            } catch (IOException ex) {
                return false;
            }
        }

        return true;
    }

    public void closeInstance(PlayerApartment playerApartment) {

        Integer instanceID = playerApartment.getInstanceID();

        UUID apartmentOwner = playerApartment.getOwner();
        String apartmentType = playerApartment.getApartmentType();

        ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(apartmentType);
        Location regionMin = apartmentSetup.getRegionMin().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));
        Location regionMax = apartmentSetup.getRegionMax().clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F));

        File userApartmentFile = new File(apartmentFolderPath + apartmentType + "/userApartments/" + apartmentOwner + ".nbt");
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

    }
}
