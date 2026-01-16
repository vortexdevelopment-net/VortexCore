package net.vortexdevelopment.vortexcore.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.UUID;

public class HeadUtils {

    public static void applyTexture(ItemStack head, String textureUrl) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("ItemStack does not have SkullMeta");
        }

        try {
            UUID profileId = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(profileId);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void applyTexture(ItemMeta meta, String textureUrl) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new IllegalArgumentException("ItemMeta is not SkullMeta");
        }

        try {
            UUID profileId = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(profileId);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            skullMeta.setPlayerProfile(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
