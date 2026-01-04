package net.vortexdevelopment.vortexcore.vinject.serializer;

import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vinject.annotation.yaml.YamlSerializer;
import net.vortexdevelopment.vinject.config.serializer.YamlSerializerBase;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
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

import java.util.*;

@net.vortexdevelopment.vinject.annotation.Component
@YamlSerializer
public class ItemStackSerializer implements YamlSerializerBase<ItemStack> {

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
            meta.displayName(AdventureUtils.formatComponent((String) map.get("Name")));
        }

        if (map.containsKey("Lore")) {
            Object loreObj = map.get("Lore");
            if (loreObj instanceof List<?>) {
                List<Component> lore = new ArrayList<>();
                for (Object o : (List<?>) loreObj) {
                    lore.add(AdventureUtils.formatComponent(o.toString()));
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

        item.setItemMeta(meta);
        return item;
    }
}
