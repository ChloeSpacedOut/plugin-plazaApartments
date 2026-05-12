package com.chloespacedout.PlazaApartments;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeyMaker {

    private final UUID keyAvatarID = UUID.fromString("363ed63d-5379-4e88-a3b3-0d005db083c1");

    public ItemStack makeKey(Core pluginInstance, FileManager fileManager, Player player, String apartmentName, Player target, boolean isBuilder, int amount) {
        ItemStack keyItem = new ItemStack(Material.PLAYER_HEAD);
        UUID playerID = player.getUniqueId();

        String keyType;
        if (target == null) {
            keyType = "public";
        } else {
            if (isBuilder) {
                keyType = "builder";
            } else {
                keyType = "private";
            }
        }

        keyItem.editMeta(SkullMeta.class, skullMeta -> {
            TextComponent name = Component.text(player.getName() + "'s " + keyType + " key")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC,false);
            List<TextComponent> lore = new ArrayList<>();
            TextComponent apartmentType = Component.text("Apartment: " + apartmentName)
                    .color(NamedTextColor.BLUE)
                    .decoration(TextDecoration.ITALIC,false);

            lore.add(apartmentType);

            if (target != null) {
                TextComponent keyHolder = Component.text("Keyholder: " + target.getName())
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC,false);
                lore.add(keyHolder);
            }

            skullMeta.customName(name);
            skullMeta.lore(lore);
            final PlayerProfile playerProfile = Bukkit.createProfile(keyAvatarID, "key");
            String data = playerID + ", " + apartmentName + ", " + keyType;
            playerProfile.setProperty(new ProfileProperty("textures", data));
            skullMeta.setPlayerProfile(playerProfile);
        });

        String apartmentKey;

        File playerDataFile = fileManager.getPlayerDataFile(playerID);
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        File apartmentKeysFile = fileManager.getApartmentKeysFile();
        YamlConfiguration apartmentKeys = YamlConfiguration.loadConfiguration(apartmentKeysFile);

        if (target == null) {
            ConfigurationSection publicKeys = playerData.getConfigurationSection("publicKeys." + apartmentName);
            apartmentKey = publicKeys.getKeys(false).stream().toList().getFirst();
        } else {
            apartmentKey = UUID.randomUUID().toString();

            playerData.set("privateKeys." + apartmentName + "." + apartmentKey,0L);
            apartmentKeys.set(apartmentKey + ".owner",playerID.toString());
            apartmentKeys.set(apartmentKey + ".apartment",apartmentName);
            apartmentKeys.set(apartmentKey + ".keyHolder",target.getUniqueId().toString());

            if (isBuilder) {
                apartmentKeys.set(apartmentKey + ".perms","build");
            } else {
                apartmentKeys.set(apartmentKey + ".perms","enter");
            }

            try {
                playerData.save(playerDataFile);
                apartmentKeys.save(apartmentKeysFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        NamespacedKey keyID = new NamespacedKey(pluginInstance,"keyID");
        keyItem.editPersistentDataContainer(pdc -> {
            pdc.set(keyID, PersistentDataType.STRING, apartmentKey);
        });

        return keyItem;
    }
}
