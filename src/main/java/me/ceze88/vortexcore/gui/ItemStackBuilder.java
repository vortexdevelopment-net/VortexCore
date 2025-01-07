package me.ceze88.vortexcore.gui;

import me.ceze88.vortexcore.VortexCore;
import me.ceze88.vortexcore.text.AdventureUtils;
import me.ceze88.vortexcore.text.MiniMessagePlaceholder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ItemStackBuilder {

    private ItemStack itemStack;

    public ItemStackBuilder() {
        this.itemStack = new ItemStack(Material.AIR);
    }

    public ItemStackBuilder setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    public ItemStackBuilder setType(Material material) {
        itemStack.setType(material);
        return this;
    }

    public ItemStackBuilder setAmount(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set amount for AIR ItemStack");
        }
        itemStack.setAmount(amount);
        return this;
    }

    public ItemStackBuilder setDisplayName(String displayName) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set display name for AIR ItemStack");
        }
        AdventureUtils.formatItemName(itemStack, displayName);
        return this;
    }

    public ItemStackBuilder setDisplayName(String displayName, MiniMessagePlaceholder... placeholders) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set display name for AIR ItemStack");
        }
        AdventureUtils.formatItemName(itemStack, displayName, placeholders);
        return this;
    }

    public ItemStackBuilder setLore(LoreBuilder loreBuilder) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, loreBuilder.lore);
        return this;
    }

    public ItemStackBuilder setLore(String... lore) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, lore);
        return this;
    }

    public ItemStackBuilder setLore(List<String> lore, MiniMessagePlaceholder... placeholders) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, lore, placeholders);
        return this;
    }

    public ItemStackBuilder setModelData(int modelData) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set model data for AIR ItemStack");
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(modelData);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemStackBuilder addGlow() {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot add glow to AIR ItemStack");
        }
        AdventureUtils.addGlow(itemStack);
        return this;
    }

    public ItemStackBuilder modify(Consumer<ItemStack> consumer) {
        consumer.accept(itemStack);
        return this;
    }

    public ItemStack build() {
        if (itemStack.getType() == Material.AIR) {
            VortexCore.getPlugin().getLogger().warning("ItemStack is AIR in ItemStackBuilder. Please set a material before building.");
            new Throwable().printStackTrace();
        }
        return itemStack;
    }

    public static class LoreBuilder {

        private List<String> lore = new LinkedList<>();

        public LoreBuilder addLine(String line) {
            lore.add(line);
            return this;
        }

        public LoreBuilder addLineIf(boolean condition, String line) {
            if (condition) {
                lore.add(line);
            }
            return this;
        }
    }
}
