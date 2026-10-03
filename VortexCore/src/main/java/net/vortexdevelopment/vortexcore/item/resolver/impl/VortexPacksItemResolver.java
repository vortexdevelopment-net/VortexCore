package net.vortexdevelopment.vortexcore.item.resolver.impl;

import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import net.vortexdevelopment.vortexcore.item.resolver.CustomItemResolver;
import net.vortexdevelopment.vortexpacks.api.VortexPacksApi;
import net.vortexdevelopment.vortexpacks.api.block.PackBlock;
import net.vortexdevelopment.vortexpacks.api.furniture.Furniture;
import net.vortexdevelopment.vortexpacks.api.item.PackItem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Element
@DependsOn(className = "net.vortexdevelopment.vortexpacks.api.VortexPacksApi")
public class VortexPacksItemResolver implements CustomItemResolver {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("vortexpacks", "id");
    private static final NamespacedKey BLOCK_ID_KEY = new NamespacedKey("vortexpacks", "block_id");
    private static final NamespacedKey FURNITURE_ID_KEY = new NamespacedKey("vortexpacks", "furniture_id");

    @Override
    public @NotNull String getPrefix() {
        return "vortexpacks";
    }

    @Override
    public boolean matchesPrefix(@NotNull String prefix) {
        return prefix.equalsIgnoreCase("vortexpacks") || prefix.equalsIgnoreCase("vp");
    }

    @Override
    public @Nullable ItemStack resolve(@NotNull String itemId) {
        if (VortexPacksApi.getItemRegistry() != null && VortexPacksApi.getItemManager() != null) {
            PackItem item = VortexPacksApi.getItemRegistry().getById(itemId);
            if (item != null) {
                return VortexPacksApi.getItemManager().createItemStack(item, null, null);
            }
        }

        if (VortexPacksApi.getBlockManager() != null) {
            PackBlock block = VortexPacksApi.getBlockManager().getById(itemId);
            if (block != null) {
                return VortexPacksApi.getBlockManager().createItemStack(block);
            }
        }

        if (VortexPacksApi.getFurnitureManager() != null) {
            Furniture furniture = VortexPacksApi.getFurnitureManager().getById(itemId);
            if (furniture != null) {
                return VortexPacksApi.getFurnitureManager().createItemStack(furniture);
            }
        }

        return null;
    }

    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String itemId) {
        String currentId = getItemId(itemStack);
        if (currentId != null) {
            return currentId.equalsIgnoreCase(itemId);
        }
        return false;
    }

    @Override
    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
        if (itemId != null) {
            return itemId;
        }

        String blockId = pdc.get(BLOCK_ID_KEY, PersistentDataType.STRING);
        if (blockId != null) {
            return blockId;
        }

        String furnitureId = pdc.get(FURNITURE_ID_KEY, PersistentDataType.STRING);
        if (furnitureId != null) {
            return furnitureId;
        }

        return null;
    }
}
