package com.chloespacedout.PlazaApartments;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

import java.util.HashMap;

public class WorldGuardManager {
    RegionManager regions;
    Config config;
    ApartmentSetupCache apartmentSetupCache;

    WorldGuardManager(Config newConfig, ApartmentSetupCache newApartmentSetupCache) {
        config = newConfig;
        apartmentSetupCache = newApartmentSetupCache;

        WorldGuard worldGuard = WorldGuard.getInstance();
        RegionContainer container = worldGuard.getPlatform().getRegionContainer();

        World apartmentWorld = BukkitAdapter.adapt(config.getApartmentWorld());

        regions = container.get(apartmentWorld);

    }

    public void regionSetup() throws ProtectedRegion.CircularInheritanceException {
        GlobalProtectedRegion apartmentTemplate = (GlobalProtectedRegion) regions.getRegion("apartment_template");
        if (apartmentTemplate == null) {
            apartmentTemplate = new GlobalProtectedRegion("apartment_template");
            regions.addRegion(apartmentTemplate);
        }
        int instanceCount = config.getInstanceCount();

        for (int instanceID = 0; instanceID < instanceCount; instanceID++) {
            HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();
            for (int i = 0; i < apartmentSetups.size(); i++) {
                ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
                String apartmentName = apartmentSetup.getName();

                String regionID = "apartment_" + apartmentName + "_" + instanceID;

                regions.removeRegion(regionID);
                Location pos1Location = apartmentSetup.getRegionMin().clone().add(0,0,1024 + (instanceID * 1024));
                BlockVector3 pos1 = BlockVector3.at(pos1Location.getBlockX(),pos1Location.getBlockY(),pos1Location.getBlockZ());

                Location pos2Location = apartmentSetup.getRegionMax().clone().add(0,0,1024 + (instanceID * 1024));
                BlockVector3 pos2 = BlockVector3.at(pos2Location.getBlockX() - 1,pos2Location.getBlockY() - 1,pos2Location.getBlockZ() - 1);

                ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionID,pos1,pos2);
                region.setParent(apartmentTemplate);
                regions.addRegion(region);
            }
        }
    }
}
