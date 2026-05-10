package com.chloespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntityHandler implements Listener {

    private Config config;
    private InstanceManager instanceManager;

    public EntityHandler(Core pluginInstance, Config newConfig, InstanceManager newInstanceManager) {
        config = newConfig;
        instanceManager = newInstanceManager;
        Bukkit.getPluginManager().registerEvents(this,pluginInstance);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        Location spawnLocation = e.getLocation();
        ApartmentUtil apartmentUtil = new ApartmentUtil(config,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(spawnLocation);
        if (playerApartment != null) {
            final int entitiesInInstance = playerApartment.getContainedEntities().size();
            final int entityLimit = config.getMaxEntitiesPerInstance();

            if (entitiesInInstance >= entityLimit) {
                e.setCancelled(true);
                playerApartment.warnPlayers("Entity limit of " + entityLimit + " reached!");
                return;
            }
            entity.addScoreboardTag("apartmentEntity+owner=" + playerApartment.getOwner());
            playerApartment.addToContainedEntities(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent e) {
        Entity entity = e.getEntity();
        Location entityLocation = entity.getLocation();
        ApartmentUtil apartmentUtil = new ApartmentUtil(config,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(entityLocation);
        if (playerApartment != null) {
            playerApartment.removeFromContainedEntities(entity.getUniqueId());
        }
    }
}
