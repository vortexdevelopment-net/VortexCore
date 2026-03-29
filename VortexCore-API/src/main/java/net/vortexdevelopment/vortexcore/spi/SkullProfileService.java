package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;

/**
 * Paper and Spigot use different {@code PlayerProfile} types on {@link SkullMeta}; this SPI hides that split.
 */
public interface SkullProfileService {

    void applyTexture(ItemStack head, String textureUrl);

    void applyTexture(ItemStack head, String textureUrl, UUID profileId);

    void applyTexture(ItemMeta meta, String textureUrl);

    void applyTexture(ItemMeta meta, String textureUrl, UUID profileId);

    void serializeSkull(SkullMeta skullMeta, Map<String, Object> map);

    void deserializeSkull(SkullMeta skullMeta, Map<String, Object> map);
}
