# GUI System

Inventory menus using VortexCore `Gui` and `PaginatedGui`.

---

## Which Class to Use

| Scenario | Class |
| --- | --- |
| Fixed layout, known slots | `Gui` |
| List of items across pages | `PaginatedGui` |
| YAML-driven static layout | `Gui` + config section |

Default to **`Gui`** unless you need pagination.

---

## `Gui` - Fixed Layout

```java
Gui gui = new Gui(Lang.getComponent("GUI.Shop Title"), 4);
gui.setPreviusGui(parentGui);  // auto back button bottom-left
gui.addItem(itemStack, (event, holder, guiItem) -> {
    // click handler
}, x, y);
gui.fetchFills(configSection); // "Fill Empty", "Fill Border", "Fill Bottom"
gui.show(player);
```

- Slot coords: `addItem(item, x, y)` where x = column (0-8), y = row (0 to rows-1).
- Titles and item text through `AdventureUtils` / `Lang.getComponent`.
- `Gui.BACK_BUTTON_NAME` set from `lang.yml` on `Lang.onReload()`.

Reference: `VortexStacker-Plugin/.../gui/SpawnerUpgradeGui.java`

---

## `PaginatedGui` - Multi-Page Lists

```java
PaginatedGui gui = new PaginatedGui(title, rows);
gui.setPreviousGui(parentGui);
gui.addStaticItem(backItem, clickHandler, x, y);
gui.addItem(new GuiItem(itemStack, clickHandler, -1, -1)); // -1 = auto pagination slot
gui.show(player);
```

Bottom row reserved for navigation:
- Slot 0: back
- Slot 3: previous page
- Slot 5: next page
- Center: page indicator

Reference: `VortexBazaar-Plugin/.../gui/CategoryItemsGui.java`

---

## `GuiItem`

Wraps `ItemStack` + click handler. Supports dynamic updates via `GuiManager` auto-update tick.

---

## YAML-Driven GUIs

Load layout from `@YamlDirectory` gui configs:

```java
BazaarGuiSettings settings = guiConfig.getGuis().get("main");
Gui gui = new Gui(AdventureUtils.formatComponent(settings.getTitle()), settings.getRows());
```

Reference: `VortexBazaar-Plugin/.../config/gui/BazaarGuiConfig.java`, `BazaarGuiSettings.java`

---

## Rules

1. No hardcoded titles or item names - use `lang.yml` or YAML gui configs with MiniMessage.
2. Use `AdventureUtils.formatItemName` / `formatItemLore` for item text.
3. `GuiManager.register(plugin)` is called from `VortexPlugin.onEnable` - do not register manually unless you have a special case.
4. Prefer `setPreviusGui` / `setPreviousGui` for back navigation over manual back buttons.

---

## Canonical References

| Pattern | File |
| --- | --- |
| Fixed Gui | `VortexStacker/.../SpawnerUpgradeGui.java` |
| PaginatedGui | `VortexBazaar/.../CategoryItemsGui.java` |
| YAML gui config | `VortexBazaar/.../BazaarGuiConfig.java` |
| Gui core | `VortexCore/.../gui/Gui.java`, `PaginatedGui.java` |
