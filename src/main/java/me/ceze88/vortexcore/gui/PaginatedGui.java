package me.ceze88.vortexcore.gui;//package me.ceze88.easygui.gui;
//
//import lombok.Getter;
//import me.ceze88.easygui.util.AdventureUtils;
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.bukkit.event.inventory.InventoryCloseEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.jetbrains.annotations.NotNull;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.UUID;
//import java.util.function.Consumer;
//
//public class PaginatedGui implements GuiHolder {
//
//    private static final UUID UID = UUID.randomUUID();
//    private final int rows;
//
//    private final Inventory inventory;
//    private final List<GuiItem> items = new ArrayList<>();
//
//    private @Getter Consumer<InventoryClickEvent> onGlobalClick;
//    private @Getter Consumer<InventoryClickEvent> onTopClick;
//    private @Getter Consumer<InventoryClickEvent> onBottomClick;
//
//    private @Getter Consumer<InventoryClickEvent> onGlobalDrag;
//    private @Getter Consumer<InventoryClickEvent> onTopDrag;
//    private @Getter Consumer<InventoryClickEvent> onBottomDrag;
//
//    private @Getter Consumer<InventoryCloseEvent> onClose;
//
//    private final List<Player> openers = new ArrayList<>();
//
//    private boolean cancelClick = true;
//
//    private int page = 0;
//    private final int startX;
//    private final int startY;
//    private final int endX;
//    private final int endY;
//    private final List<GuiItem> paginatedItems = new ArrayList<>();
//
//    public PaginatedGui(String name, int rows, int startX, int startY, int endX, int endY) {
//        this.rows = rows;
//        this.inventory = Bukkit.createInventory(this, rows * 9, AdventureUtils.formatComponent(name));
//
//        this.startX = startX;
//        this.startY = startY;
//        this.endX = endX;
//        this.endY = endY;
//
//        //Add pagination buttons
//    }
//
//    private void setupPaginationButtons() {
//        //Fill last row with empty items for decoration
//        for (int i = 0; i < 9; i++) {
//            ItemStack empty = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
//            AdventureUtils.formatItemName(empty, " ");
//            inventory.setItem(i + (rows - 1) * 9, empty);
//        }
//
//        ItemStack next = new ItemStack(Material.ARROW);
//        ItemStack previous = new ItemStack(Material.ARROW);
//        AdventureUtils.formatItemName(next, "§a§l-->");
//        AdventureUtils.formatItemLore(next, "§7Következő oldal");
//        AdventureUtils.formatItemName(previous, "§e§l<--");
//        AdventureUtils.formatItemLore(previous, "§7Előző oldal");
//
//        GuiItem nextItem = new GuiItem(next, event -> {
//            nextPage();
//            event.setCancelled(true);
//        }, endX + 1, endY);
//
//        GuiItem previousItem = new GuiItem(previous, event -> {
//            previousPage();
//            event.setCancelled(true);
//        }, startX - 1, endY);
//
//        addItem(nextItem);
//        addItem(previousItem);
//    }
//
//    public PaginatedGui addItem(GuiItem item) {
//        paginatedItems.add(item);
//        refresh();
//        return this;
//    }
//
//    public PaginatedGui addItem(ItemStack item, int x, int y, Consumer<InventoryClickEvent> event) {
//        return addItem(new GuiItem(item, event, x, y));
//    }
//
//    public void refresh() {
//        //set the items again without clearing the inventory
//        for (int i = 0; i < getItemsOnPage(page).size(); i++) {
//            GuiItem item = paginatedItems.get(i);
//            int x = startX + i % (endX - startX + 1);
//            int y = startY + i / (endX - startX + 1);
//            inventory.setItem(x + y * 9, item.getItem());
//        }
//    }
//
//    public List<GuiItem> getItemsOnPage(int page) {
//        List<GuiItem> items = new LinkedList<>();
//        for (int i = 0; i < paginatedItems.size(); i++) {
//            if (i / ((endX - startX + 1) * (endY - startY + 1)) == page) {
//                items.add(paginatedItems.get(i));
//            }
//        }
//        return items;
//    }
//
//    public boolean hasNextPage() {
//        return !getItemsOnPage(page + 1).isEmpty();
//    }
//
//    public void nextPage() {
//        if (!hasNextPage()) {
//            return;
//        }
//        page++;
//        refresh();
//    }
//
//    public void previousPage() {
//        if (page <= 0) {
//            return;
//        }
//        page--;
//        refresh();
//    }
//
//    @Override
//    public @NotNull Inventory getInventory() {
//        return inventory;
//    }
//
//    @Override
//    public GuiHolder setOnGlobalDrag(Consumer<InventoryClickEvent> onGlobalDrag) {
//        this.onGlobalDrag = onGlobalDrag;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnTopDrag(Consumer<InventoryClickEvent> onTopDrag) {
//        this.onTopDrag = onTopDrag;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnBottomDrag(Consumer<InventoryClickEvent> onBottomDrag) {
//        this.onBottomDrag = onBottomDrag;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnGlobalClick(Consumer<InventoryClickEvent> onGlobalClick) {
//        this.onGlobalClick = onGlobalClick;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnTopClick(Consumer<InventoryClickEvent> onTopClick) {
//        this.onTopClick = onTopClick;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnBottomClick(Consumer<InventoryClickEvent> onBottomClick) {
//        this.onBottomClick = onBottomClick;
//        return this;
//    }
//
//    @Override
//    public GuiHolder setOnClose(Consumer<InventoryCloseEvent> onClose) {
//        this.onClose = onClose;
//        return this;
//    }
//
//    @Override
//    public boolean cancelClick() {
//        return cancelClick;
//    }
//
//    @Override
//    public @Nullable GuiItem getItem(int slot) {
//        int x = slot % 9;
//        int y = slot / 9;
//
//        //Find the item in the current page
//        for (GuiItem item : getItemsOnPage(page)) {
//            if (item.getX() == x && item.getY() == y) {
//                return item;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public void onClose(Player player) {
//        openers.remove(player);
//    }
//}
