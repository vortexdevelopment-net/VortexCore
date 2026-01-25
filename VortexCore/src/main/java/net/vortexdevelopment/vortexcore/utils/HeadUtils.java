package net.vortexdevelopment.vortexcore.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HeadUtils {

    public static void applyTexture(ItemStack head, String textureUrl) {
        applyTexture(head, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    public static void applyTexture(ItemStack head, String textureUrl, UUID profileId) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("ItemStack does not have SkullMeta");
        }

        applyTexture(meta, textureUrl, profileId);
        head.setItemMeta(meta);
    }

    public static void applyTexture(ItemMeta meta, String textureUrl) {
        applyTexture(meta, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    public static void applyTexture(ItemMeta meta, String textureUrl, UUID profileId) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new IllegalArgumentException("ItemMeta is not SkullMeta");
        }

        try {
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
