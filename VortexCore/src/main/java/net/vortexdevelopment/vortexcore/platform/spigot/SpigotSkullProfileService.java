package net.vortexdevelopment.vortexcore.platform.spigot;

import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.compatibility.ServerProject;
import net.vortexdevelopment.vortexcore.spi.SkullProfileService;
import net.vortexdevelopment.vortexcore.spi.SkullProfiles;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spigot/CraftBukkit skull textures via Mojang {@code GameProfile} and the {@code profile} field on {@link SkullMeta}
 * (same approach as pre-Adventure custom heads). Paper servers should use {@code PaperSkullProfileService} instead.
 */
@net.vortexdevelopment.vinject.annotation.component.Component
public class SpigotSkullProfileService implements SkullProfileService {

    private static final Pattern SKIN_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    @PostConstruct
    public void registerSkullProfile() {
        if (ServerProject.isServer(ServerProject.PAPER)) {
            return;
        }
        SkullProfiles.install(this);
    }

    @Override
    public void applyTexture(ItemStack head, String textureUrl) {
        applyTexture(head, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void applyTexture(ItemStack head, String textureUrl, UUID profileId) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("ItemStack does not have SkullMeta");
        }
        applyTexture(meta, textureUrl, profileId);
        head.setItemMeta(meta);
    }

    @Override
    public void applyTexture(ItemMeta meta, String textureUrl) {
        applyTexture(meta, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void applyTexture(ItemMeta meta, String textureUrl, UUID profileId) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new IllegalArgumentException("ItemMeta is not SkullMeta");
        }
        try {
            Object profile = newGameProfile(profileId, "VortexCore");
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(textureUrl) + "\"}}}";
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            Object property = newProperty("textures", encoded);
            putTextureProperty(profile, property);
            setProfileField(skullMeta, profile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply skull texture on Spigot (GameProfile/reflection)", e);
        }
    }

    @Override
    public void serializeSkull(SkullMeta skullMeta, Map<String, Object> map) {
        try {
            Object profile = getProfileField(skullMeta);
            if (profile == null) {
                return;
            }
            Class<?> gpClass = profile.getClass();
            UUID id = (UUID) gpClass.getMethod("getId").invoke(profile);
            if (id != null) {
                map.put("UUID", id.toString());
            }
            String name = (String) gpClass.getMethod("getName").invoke(profile);
            String textureUrl = extractTextureUrlFromProfile(profile);
            if (textureUrl != null) {
                map.put("Texture", textureUrl);
            } else if (name != null && !name.isEmpty() && !"VortexCore".equals(name)) {
                map.put("Owner", name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deserializeSkull(SkullMeta skullMeta, Map<String, Object> map) {
        UUID uuid = null;
        if (map.containsKey("UUID")) {
            try {
                uuid = UUID.fromString((String) map.get("UUID"));
            } catch (Exception ignored) {
            }
        }

        if (map.containsKey("Texture")) {
            String texture = (String) map.get("Texture");
            if (uuid != null) {
                applyTexture(skullMeta, texture, uuid);
            } else {
                applyTexture(skullMeta, texture);
            }
        } else if (map.containsKey("Owner")) {
            String owner = (String) map.get("Owner");
            OfflinePlayer op = uuid != null ? Bukkit.getOfflinePlayer(uuid) : Bukkit.getOfflinePlayer(owner);
            skullMeta.setOwningPlayer(op);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Object newGameProfile(UUID id, String name) throws Exception {
        Class<?> gpClass = Class.forName("com.mojang.authlib.GameProfile");
        return gpClass.getConstructor(UUID.class, String.class).newInstance(id, name);
    }

    private static Object newProperty(String key, String value) throws Exception {
        Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> ctor = propClass.getConstructor(String.class, String.class);
        return ctor.newInstance(key, value);
    }

    private static void putTextureProperty(Object gameProfile, Object property) throws Exception {
        Class<?> gpClass = gameProfile.getClass();
        Object properties = gpClass.getMethod("getProperties").invoke(gameProfile);
        Class<?> propClass = property.getClass();
        Method put = properties.getClass().getMethod("put", String.class, propClass);
        put.invoke(properties, "textures", property);
    }

    private static void setProfileField(SkullMeta meta, Object gameProfile) throws Exception {
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(meta, gameProfile);
    }

    private static Object getProfileField(SkullMeta meta) throws Exception {
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        return profileField.get(meta);
    }

    private static String extractTextureUrlFromProfile(Object profile) {
        try {
            Class<?> gpClass = profile.getClass();
            Object properties = gpClass.getMethod("getProperties").invoke(profile);
            Method get = properties.getClass().getMethod("get", Object.class);
            @SuppressWarnings("unchecked")
            Iterable<Object> textures = (Iterable<Object>) get.invoke(properties, "textures");
            if (textures == null) {
                return null;
            }
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Method getValue = propClass.getMethod("getValue");
            for (Object prop : textures) {
                if (prop == null) {
                    continue;
                }
                String b64 = (String) getValue.invoke(prop);
                if (b64 == null || b64.isEmpty()) {
                    continue;
                }
                String json = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
                Matcher m = SKIN_URL_PATTERN.matcher(json);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
