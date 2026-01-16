package net.vortexdevelopment.vortexcore.vinject.serializer;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.vortexdevelopment.vinject.annotation.yaml.YamlSerializer;
import net.vortexdevelopment.vinject.config.serializer.YamlSerializerBase;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.utils.HeadUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.block.banner.PatternType;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapRenderer;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.entity.Axolotl;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@net.vortexdevelopment.vinject.annotation.component.Component
@YamlSerializer
public class ItemStackSerializer implements YamlSerializerBase<ItemStack> {

    private static final Pattern PDC_PATTERN = Pattern.compile("^([a-z0-9._/:-]+):([A-Z_]+):(.*)$");

    @Override
    public Class<ItemStack> getTargetType() {
        return ItemStack.class;
    }

    @Override
    public Map<String, Object> serialize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Material", item.getType().name());

        if (item.getAmount() != 1) {
            map.put("Amount", item.getAmount());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasItemName()) {
                Component name = meta.itemName();
                map.put("Name", AdventureUtils.toMiniMessage(name));
            }
            if (meta.hasDisplayName()) {
                Component name = meta.displayName();
                if (name != null) {
                    map.put("Name", AdventureUtils.toMiniMessage(name));
                }
            }

            if (meta.hasLore()) {
                List<Component> loreComponents = meta.lore();
                if (loreComponents != null && !loreComponents.isEmpty()) {
                    map.put("Lore", AdventureUtils.toMiniMessage(loreComponents));
                }
            }

            if (meta.hasEnchants()) {
                List<String> enchants = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchants.add(entry.getKey().getKey().getKey().toUpperCase() + ":" + entry.getValue());
                }
                map.put("Enchants", enchants);
            }

            if (meta instanceof EnchantmentStorageMeta esm) {
                if (esm.hasStoredEnchants()) {
                    List<String> storedEnchants = new ArrayList<>();
                    for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                        storedEnchants.add(entry.getKey().getKey().getKey().toUpperCase() + ":" + entry.getValue());
                    }
                    map.put("Stored Enchants", storedEnchants);
                }
            }

            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) {
                    flags.add(flag.name());
                }
                map.put("Item Flags", flags);
            }

            if (meta.isUnbreakable()) {
                map.put("Unbreakable", true);
            }

            if (meta instanceof Damageable damageable) {
                if (damageable.hasDamage()) {
                    map.put("Durability", damageable.getDamage());
                }
            }

            if (meta.hasCustomModelData()) {
                map.put("Custom Model Data", meta.getCustomModelData());
            }

            if (meta.hasItemModel()) {
                NamespacedKey model = meta.getItemModel();
                if (model != null) {
                    map.put("Custom Model", model.toString());
                }
            }

            if (meta.hasAttributeModifiers()) {
                List<String> attributes = new ArrayList<>();
                meta.getAttributeModifiers().asMap().forEach((attribute, modifiers) -> {
                    for (AttributeModifier modifier : modifiers) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(attribute.key().key()).append(":");
                        sb.append(modifier.getAmount()).append(":");
                        sb.append(modifier.getOperation().name());
                        if (modifier.getSlotGroup() != null && modifier.getSlotGroup() != EquipmentSlotGroup.ANY) {
                            sb.append(":").append(modifier.getSlotGroup().toString().toUpperCase());
                        }
                        attributes.add(sb.toString());
                    }
                });
                if (!attributes.isEmpty()) {
                    map.put("Attributes", attributes);
                }
            }

            // Tipped arrow and potion types serialization
            if (meta instanceof PotionMeta potionMeta) {
                // Serialize to "Potion" key if it is a tripped arrow or potion
                if (item.getType() == Material.TIPPED_ARROW || item.getType() == Material.POTION) {
                    if (potionMeta.getBasePotionType() != null) {
                        map.put("Potion", potionMeta.getBasePotionType().name());
                    }
                }
            }

            if (meta instanceof LeatherArmorMeta leatherMeta) {
                Color color = leatherMeta.getColor();
                map.put("Color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
            }

            if (meta instanceof SkullMeta skullMeta) {
                PlayerProfile profile = skullMeta.getPlayerProfile();
                if (profile != null) {
                    PlayerTextures textures = profile.getTextures();
                    if (textures != null && textures.getSkin() != null) {
                        map.put("Texture", textures.getSkin().toString());
                    } else if (profile.getName() != null) {
                        map.put("Owner", profile.getName());
                    }
                }
            }

            if (meta instanceof FireworkMeta fireworkMeta) {
                map.put("Power", fireworkMeta.getPower());
                if (fireworkMeta.hasEffects()) {
                    Map<String, Object> effects = new LinkedHashMap<>();
                    int i = 0;
                    for (FireworkEffect effect : fireworkMeta.getEffects()) {
                        effects.put(String.valueOf(i++), serializeFireworkEffect(effect));
                    }
                    map.put("Firework Effects", effects);
                }
            }

            if (meta instanceof BannerMeta bannerMeta) {
                if (bannerMeta.numberOfPatterns() > 0) {
                    List<String> patterns = new ArrayList<>();
                    for (org.bukkit.block.banner.Pattern pattern : bannerMeta.getPatterns()) {
                        patterns.add(pattern.getColor().name() + ":" + pattern.getPattern().getIdentifier());
                    }
                    map.put("Banner Patterns", patterns);
                }
            }

            if (meta instanceof MapMeta mapMeta) {
                if (mapMeta.hasMapView()) {
                    MapView view = mapMeta.getMapView();
                    if (view != null) {
                        map.put("Map ID", view.getId());
                        map.put("Map Locked", view.isLocked());
                        
                        List<String> renderers = new ArrayList<>();
                        for (MapRenderer renderer : view.getRenderers()) {
                            renderers.add(renderer.getClass().getName());
                        }
                        if (!renderers.isEmpty()) {
                            map.put("Map Renderers", renderers);
                        }
                    }
                }
                
                if (mapMeta.isScaling()) {
                    map.put("Map Scaling", true);
                }
                
                if (mapMeta.hasLocationName()) {
                    map.put("Location Name", mapMeta.getLocationName());
                }
                
                if (mapMeta.hasColor()) {
                    Color color = mapMeta.getColor();
                    map.put("Map Color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
                }
            }

            if (meta instanceof AxolotlBucketMeta axolotlMeta) {
                if (axolotlMeta.hasVariant()) {
                    map.put("Axolotl Variant", axolotlMeta.getVariant().name());
                }
            }

            if (meta instanceof BundleMeta bundleMeta) {
                if (bundleMeta.hasItems()) {
                    Map<String, Object> bundleItems = new LinkedHashMap<>();
                    int i = 0;
                    for (ItemStack bundleItem : bundleMeta.getItems()) {
                        bundleItems.put(String.valueOf(i++), serialize(bundleItem));
                    }
                    map.put("Bundle Items", bundleItems);
                }
            }

            if (meta instanceof BlockStateMeta blockStateMeta) {
                BlockState state = blockStateMeta.getBlockState();
                if (state instanceof ShulkerBox shulkerBox) {
                    Map<String, Object> contents = new LinkedHashMap<>();
                    ItemStack[] inv = shulkerBox.getInventory().getContents();
                    for (int i = 0; i < inv.length; i++) {
                        if (inv[i] != null && inv[i].getType() != Material.AIR) {
                            contents.put(String.valueOf(i), serialize(inv[i]));
                        }
                    }
                    if (!contents.isEmpty()) {
                        map.put("Shulker Contents", contents);
                    }
                }
            }

            if (meta instanceof FireworkEffectMeta effectMeta) {
                if (effectMeta.hasEffect()) {
                    map.put("Firework Effect", serializeFireworkEffect(effectMeta.getEffect()));
                }
            }

            if (!meta.getPersistentDataContainer().isEmpty()) {
                List<String> pdcEntries = new ArrayList<>();
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (NamespacedKey key : pdc.getKeys()) {
                    if (pdc.has(key, PersistentDataType.STRING)) {
                        pdcEntries.add(key.toString() + ":STRING:" + pdc.get(key, PersistentDataType.STRING));
                    } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                        pdcEntries.add(key.toString() + ":INTEGER:" + pdc.get(key, PersistentDataType.INTEGER));
                    } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                        pdcEntries.add(key.toString() + ":DOUBLE:" + pdc.get(key, PersistentDataType.DOUBLE));
                    } else if (pdc.has(key, PersistentDataType.LONG)) {
                        pdcEntries.add(key.toString() + ":LONG:" + pdc.get(key, PersistentDataType.LONG));
                    } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                        pdcEntries.add(key.toString() + ":FLOAT:" + pdc.get(key, PersistentDataType.FLOAT));
                    } else if (pdc.has(key, PersistentDataType.SHORT)) {
                        pdcEntries.add(key.toString() + ":SHORT:" + pdc.get(key, PersistentDataType.SHORT));
                    } else if (pdc.has(key, PersistentDataType.BYTE)) {
                        pdcEntries.add(key.toString() + ":BYTE:" + pdc.get(key, PersistentDataType.BYTE));
                    }
                }
                if (!pdcEntries.isEmpty()) {
                    map.put("PDC", pdcEntries);
                }
            }
        }

        return map;
    }

    @Override
    public ItemStack deserialize(Map<String, Object> map) {
        String materialName = (String) map.get("Material");
        if (materialName == null) {
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return null;
        }

        int amount = 1;
        if (map.containsKey("Amount")) {
            Object amountObj = map.get("Amount");
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            }
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (map.containsKey("Name")) {
            meta.itemName(AdventureUtils.formatComponent((String) map.get("Name")));
        }

        if (map.containsKey("Lore")) {
            Object loreObj = map.get("Lore");
            if (loreObj instanceof List<?>) {
                List<Component> lore = new ArrayList<>();
                for (Object o : (List<?>) loreObj) {
                    lore.add(AdventureUtils.formatComponent(o.toString()).decorate(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE).decoration()));
                }
                meta.lore(lore);
            }
        }

        if (map.containsKey("Enchants")) {
            Object enchantsObj = map.get("Enchants");
            if (enchantsObj instanceof List<?>) {
                for (Object o : (List<?>) enchantsObj) {
                    String s = o.toString();
                    String[] parts = s.split(":");
                    if (parts.length >= 2) {
                        try {
                            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                            if (ench != null) {
                                meta.addEnchant(ench, Integer.parseInt(parts[1]), true);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (map.containsKey("Stored Enchants") && meta instanceof EnchantmentStorageMeta esm) {
            Object enchantsObj = map.get("Stored Enchants");
            if (enchantsObj instanceof List<?>) {
                for (Object o : (List<?>) enchantsObj) {
                    String s = o.toString();
                    String[] parts = s.split(":");
                    if (parts.length >= 2) {
                        try {
                            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                            if (ench != null) {
                                esm.addStoredEnchant(ench, Integer.parseInt(parts[1]), true);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (map.containsKey("Item Flags")) {
            Object flagsObj = map.get("Item Flags");
            if (flagsObj instanceof List<?>) {
                for (Object o : (List<?>) flagsObj) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(o.toString().toUpperCase()));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (Boolean.TRUE.equals(map.get("Unbreakable"))) {
            meta.setUnbreakable(true);
        }

        if (map.containsKey("Durability") && meta instanceof Damageable damageable) {
            Object durObj = map.get("Durability");
            if (durObj instanceof Number) {
                damageable.setDamage(((Number) durObj).intValue());
            }
        }

        if (map.containsKey("Custom Model Data")) {
            Object cmdObj = map.get("Custom Model Data");
            if (cmdObj instanceof Number) {
                meta.setCustomModelData(((Number) cmdObj).intValue());
            }
        }

        if (map.containsKey("Custom Model")) {
            String modelStr = (String) map.get("Custom Model");
            meta.setItemModel(NamespacedKey.fromString(modelStr));
        }

        if (map.containsKey("Attributes")) {
            Object attrObj = map.get("Attributes");
            if (attrObj instanceof List<?>) {
                for (Object o : (List<?>) attrObj) {
                    String[] parts = o.toString().split(":");
                    if (parts.length >= 3) {
                        try {
                            // Format: ATTRIBUTE:VALUE:OPERATION[:SLOT]
                            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(parts[0].toUpperCase(Locale.ENGLISH))); //Attribute.valueOf(parts[0].toUpperCase());
                            double value = Double.parseDouble(parts[1]);
                            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(parts[2].toUpperCase());
                            EquipmentSlotGroup slot = parts.length > 3 ? EquipmentSlotGroup.getByName(parts[3].toLowerCase()) : EquipmentSlotGroup.ANY;
                            if (slot == null) {
                                slot = EquipmentSlotGroup.ANY;
                            }

                            AttributeModifier modifier = new AttributeModifier(
                                    new NamespacedKey("vortexcore", "attr_" + UUID.randomUUID()),
                                    value,
                                    op,
                                    slot
                            );
                            meta.addAttributeModifier(attribute, modifier);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // Potion type deserialization
        if (map.containsKey("Potion") && meta instanceof PotionMeta potionMeta) {
            String potionTypeName = (String) map.get("Potion");
            try {
                PotionType potionType = PotionType.valueOf(potionTypeName.toUpperCase());
                potionMeta.setBasePotionType(potionType);
            } catch (Exception ignored) {}
        }

        if (map.containsKey("Color") && meta instanceof LeatherArmorMeta leatherMeta) {
            try {
                String colorStr = (String) map.get("Color");
                if (colorStr.startsWith("#")) {
                    colorStr = colorStr.substring(1);
                }
                int rgb = Integer.parseInt(colorStr, 16);
                leatherMeta.setColor(Color.fromRGB(rgb));
            } catch (Exception ignored) {
            }
        }

        if (meta instanceof SkullMeta skullMeta) {
            if (map.containsKey("Texture")) {
                String texture = (String) map.get("Texture");
                HeadUtils.applyTexture(meta, texture);
            } else if (map.containsKey("Owner")) {
                String owner = (String) map.get("Owner");
                PlayerProfile profile = Bukkit.createProfile(owner);
                skullMeta.setPlayerProfile(profile);
            }
        }

        if (meta instanceof FireworkMeta fireworkMeta) {
            if (map.containsKey("Power") && map.get("Power") instanceof Number) {
                fireworkMeta.setPower(((Number) map.get("Power")).intValue());
            }
            if (map.containsKey("Firework Effects") && map.get("Firework Effects") instanceof Map) {
                Map<String, Object> effectsMap = (Map<String, Object>) map.get("Firework Effects");
                List<String> keys = new ArrayList<>(effectsMap.keySet());
                keys.sort((k1, k2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(k1), Integer.parseInt(k2));
                    } catch (NumberFormatException e) {
                        return k1.compareTo(k2);
                    }
                });
                
                for (String key : keys) {
                    Object obj = effectsMap.get(key);
                    if (obj instanceof Map) {
                        try {
                            fireworkMeta.addEffect(deserializeFireworkEffect((Map<String, Object>) obj));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (meta instanceof FireworkEffectMeta effectMeta) {
            if (map.containsKey("Firework Effect") && map.get("Firework Effect") instanceof Map) {
                try {
                    effectMeta.setEffect(deserializeFireworkEffect((Map<String, Object>) map.get("Firework Effect")));
                } catch (Exception ignored) {}
            }
        }

        if (meta instanceof BannerMeta bannerMeta) {
            if (map.containsKey("Banner Patterns")) {
                Object patternsObj = map.get("Banner Patterns");
                if (patternsObj instanceof List<?>) {
                    List<org.bukkit.block.banner.Pattern> patterns = new ArrayList<>();
                    for (Object o : (List<?>) patternsObj) {
                        String s = o.toString();
                        String[] parts = s.split(":");
                        if (parts.length >= 2) {
                            try {
                                DyeColor color = DyeColor.valueOf(parts[0].toUpperCase());
                                PatternType type = PatternType.getByIdentifier(parts[1]);
                                if (type != null) {
                                    patterns.add(new org.bukkit.block.banner.Pattern(color, type));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    bannerMeta.setPatterns(patterns);
                }
            }
        }

        if (meta instanceof MapMeta mapMeta) {
            if (map.containsKey("Map Scaling")) {
                 mapMeta.setScaling(Boolean.TRUE.equals(map.get("Map Scaling")));
            }
            
            if (map.containsKey("Location Name")) {
                mapMeta.setLocationName((String) map.get("Location Name"));
            }
            
            if (map.containsKey("Map Color")) {
                mapMeta.setColor(parseColor((String) map.get("Map Color")));
            }

            if (map.containsKey("Map ID")) {
                Object idObj = map.get("Map ID");
                if (idObj instanceof Number) {
                    int id = ((Number) idObj).intValue();
                    MapView view = Bukkit.getMap(id);
                    if (view != null) {
                        mapMeta.setMapView(view);
                        
                        if (map.containsKey("Map Locked")) {
                            view.setLocked(Boolean.TRUE.equals(map.get("Map Locked")));
                        }
                        
                        if (map.containsKey("Map Renderers") && map.get("Map Renderers") instanceof List) {
                            for (Object o : (List<?>) map.get("Map Renderers")) {
                                String className = o.toString();
                                try {
                                    Class<?> clazz = Class.forName(className);
                                    if (MapRenderer.class.isAssignableFrom(clazz)) {
                                        MapRenderer renderer = (MapRenderer) clazz.getDeclaredConstructor().newInstance();
                                        view.addRenderer(renderer);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        }

        if (meta instanceof AxolotlBucketMeta axolotlMeta) {
            if (map.containsKey("Axolotl Variant")) {
                try {
                    axolotlMeta.setVariant(Axolotl.Variant.valueOf(((String) map.get("Axolotl Variant")).toUpperCase()));
                } catch (Exception ignored) {}
            }
        }

        if (meta instanceof BundleMeta bundleMeta) {
            if (map.containsKey("Bundle Items") && map.get("Bundle Items") instanceof Map) {
                Map<String, Object> itemsMap = (Map<String, Object>) map.get("Bundle Items");
                List<String> keys = new ArrayList<>(itemsMap.keySet());
                keys.sort((k1, k2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(k1), Integer.parseInt(k2));
                    } catch (NumberFormatException e) {
                        return k1.compareTo(k2);
                    }
                });

                for (String key : keys) {
                    Object obj = itemsMap.get(key);
                    if (obj instanceof Map) {
                        try {
                            bundleMeta.addItem(deserialize((Map<String, Object>) obj));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (map.containsKey("Shulker Contents") && map.get("Shulker Contents") instanceof Map) {
                BlockState state = blockStateMeta.getBlockState();
                if (state instanceof ShulkerBox shulkerBox) {
                    Map<String, Object> contentsMap = (Map<String, Object>) map.get("Shulker Contents");
                    for (Map.Entry<String, Object> entry : contentsMap.entrySet()) {
                        try {
                            int slot = Integer.parseInt(entry.getKey());
                            if (entry.getValue() instanceof Map) {
                                ItemStack itemStack = deserialize((Map<String, Object>) entry.getValue());
                                if (itemStack != null) {
                                    shulkerBox.getInventory().setItem(slot, itemStack);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    blockStateMeta.setBlockState(shulkerBox);
                }
            }
        }

        if (map.containsKey("PDC")) {
            Object pdcObj = map.get("PDC");
            if (pdcObj instanceof List<?>) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (Object o : (List<?>) pdcObj) {
                    Matcher matcher = PDC_PATTERN.matcher(o.toString());
                    if (matcher.matches()) {
                        try {
                            NamespacedKey key = NamespacedKey.fromString(matcher.group(1));
                            String type = matcher.group(2);
                            String value = matcher.group(3);

                            if (key != null) {
                                switch (type) {
                                    case "STRING":
                                        pdc.set(key, PersistentDataType.STRING, value);
                                        break;
                                    case "INTEGER":
                                        pdc.set(key, PersistentDataType.INTEGER, Integer.parseInt(value));
                                        break;
                                    case "DOUBLE":
                                        pdc.set(key, PersistentDataType.DOUBLE, Double.parseDouble(value));
                                        break;
                                    case "LONG":
                                        pdc.set(key, PersistentDataType.LONG, Long.parseLong(value));
                                        break;
                                    case "FLOAT":
                                        pdc.set(key, PersistentDataType.FLOAT, Float.parseFloat(value));
                                        break;
                                    case "SHORT":
                                        pdc.set(key, PersistentDataType.SHORT, Short.parseShort(value));
                                        break;
                                    case "BYTE":
                                        pdc.set(key, PersistentDataType.BYTE, Byte.parseByte(value));
                                        break;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private Map<String, Object> serializeFireworkEffect(FireworkEffect effect) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Type", effect.getType().name());

        if (!effect.getColors().isEmpty()) {
            List<String> colors = new ArrayList<>();
            for (Color color : effect.getColors()) {
                colors.add(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
            }
            map.put("Colors", colors);
        }

        if (!effect.getFadeColors().isEmpty()) {
            List<String> fadeColors = new ArrayList<>();
            for (Color color : effect.getFadeColors()) {
                fadeColors.add(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
            }
            map.put("Fade Colors", fadeColors);
        }

        if (effect.hasFlicker()) map.put("Flicker", true);
        if (effect.hasTrail()) map.put("Trail", true);

        return map;
    }

    private FireworkEffect deserializeFireworkEffect(Map<String, Object> map) {
        FireworkEffect.Builder builder = FireworkEffect.builder();

        if (map.containsKey("Type")) {
            builder.with(FireworkEffect.Type.valueOf(((String) map.get("Type")).toUpperCase()));
        }

        if (map.containsKey("Colors") && map.get("Colors") instanceof List) {
            for (Object obj : (List<?>) map.get("Colors")) {
                builder.withColor(parseColor(obj.toString()));
            }
        }

        if (map.containsKey("Fade Colors") && map.get("Fade Colors") instanceof List) {
            for (Object obj : (List<?>) map.get("Fade Colors")) {
                builder.withFade(parseColor(obj.toString()));
            }
        }

        if (Boolean.TRUE.equals(map.get("Flicker"))) builder.withFlicker();
        if (Boolean.TRUE.equals(map.get("Trail"))) builder.withTrail();

        return builder.build();
    }

    private Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }
        return Color.fromRGB(Integer.parseInt(colorStr, 16));
    }
}
