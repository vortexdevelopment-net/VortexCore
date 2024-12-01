package me.ceze88.vortexcore.utils;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.ceze88.vortexcore.compatibility.ServerProject;
import me.ceze88.vortexcore.compatibility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

public class ItemUtils {

    public static ItemStack addGlow(ItemStack item) {
        if (ServerProject.isServer(ServerProject.PAPER) && (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_20_5))) {
            if (item == null || item.getType() == Material.AIR) {
                return item;
            }
            NBTItem nbtItem = new NBTItem(item);
            nbtItem.addCompound("minecraft:enchantments");
            nbtItem.applyNBT(item);
            return item;
        }

        return item;
    }
}
